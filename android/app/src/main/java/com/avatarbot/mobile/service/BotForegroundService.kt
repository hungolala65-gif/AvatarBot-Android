package com.avatarbot.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import com.avatarbot.mobile.MainActivity
import com.avatarbot.mobile.data.GameCities
import com.avatarbot.mobile.data.SecureConfigStore
import com.avatarbot.mobile.data.SecureExecutionJournal
import com.avatarbot.mobile.data.ExecutionStage
import com.avatarbot.mobile.data.ActiveAutoSession
import com.avatarbot.mobile.legacy.LegacyAdvancedSetup
import com.avatarbot.mobile.engine.LocalBotRuntime
import com.avatarbot.mobile.legacy.LegacyBotRuntime
import com.avatarbot.mobile.legacy.AuthWatchdogPolicy
import com.avatarbot.mobile.legacy.LegacyFarmCompatibility
import com.avatarbot.mobile.legacy.RetryPolicy
import com.avatarbot.mobile.legacy.NetworkWaitPolicy
import com.avatarbot.mobile.legacy.StartAdmissionPolicy
import java.util.concurrent.Executors

/** One opt-in, user-started bot session. Never starts automatically after reboot/process death.
 *  The JAR handles cycle-level reconnection internally. This monitor ONLY restarts
 *  a worker whose thread has actually exited, with a strict 3-attempt budget.
 */
class BotForegroundService : Service() {
    private val io = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val retry = RetryPolicy()
    private val authWatchdog = AuthWatchdogPolicy()
    private var observingLegacy = false
    @Volatile private var requestedStop = false
    @Volatile private var workerStarted = false
    @Volatile private var allowRetry = false
    private var activeAccountId: String? = null
    private var activeMode: Int = -1
    private fun event(stage: ExecutionStage) {
        val id = activeAccountId ?: return
        runCatching { SecureExecutionJournal(this).append(id, activeMode, stage) }
    }
    private var scheduledRetry: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastWakeRenewMs = 0L
    private var lastNotifiedState = ""
    private var lastAuthState = "inactive"
    private var loginObservedForStart = false
    private var lastObservedLoginCycles = 0L
    private val networkWait = NetworkWaitPolicy()
    private val connectivity by lazy { getSystemService(ConnectivityManager::class.java) }
    private var networkCallbackRegistered = false
    private var offlineExpiryTask: Runnable? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { mainHandler.post { resumeIfNetworkRestored() } }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            mainHandler.post { resumeIfNetworkRestored() }
        }
        override fun onLost(network: Network) { mainHandler.post { resumeIfNetworkRestored() } }
    }

    private fun internetAvailable(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        // Android's INTERNET bit means a potential route, NOT a verified game login.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun cancelOfflineWait() {
        offlineExpiryTask?.let(mainHandler::removeCallbacks)
        offlineExpiryTask = null
        networkWait.cancel()
        if (networkCallbackRegistered) {
            runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
            networkCallbackRegistered = false
        }
    }

    private fun waitForInternet() {
        if (requestedStop || !allowRetry || activeAccountId == null) {
            updateNotification("Bot đã dừng")
            stopSelf()
            return
        }
        networkWait.begin(android.os.SystemClock.elapsedRealtime())
        event(ExecutionStage.WAIT_NETWORK)
        LegacyBotRuntime.markNetworkWaiting(true)
        releaseCpuWakeLock()
        updateNotification("Mất kết nối mạng • chờ phục hồi, không tiêu lượt thử lại")
        if (!networkCallbackRegistered) {
            try {
                connectivity.registerDefaultNetworkCallback(networkCallback)
                networkCallbackRegistered = true
            } catch (_: RuntimeException) {
                // Fallback below still checks connectivity without adding retry attempts.
            }
        }
        offlineExpiryTask?.let(mainHandler::removeCallbacks)
        val check = object : Runnable {
            override fun run() {
                if (requestedStop || !networkWait.waiting()) return
                if (networkWait.expired(android.os.SystemClock.elapsedRealtime())) {
                    cancelOfflineWait()
                    LegacyBotRuntime.markRetryPending(false)
                    allowRetry = false
                    updateNotification("Ngừng bot: mất mạng quá 30 phút")
                    stopSelf()
                } else {
                    resumeIfNetworkRestored()
                    if (networkWait.waiting()) mainHandler.postDelayed(this, 30000L)
                }
            }
        }
        offlineExpiryTask = check
        mainHandler.postDelayed(check, 30000L)
        resumeIfNetworkRestored()
    }

    private fun resumeIfNetworkRestored() {
        if (requestedStop || !networkWait.waiting() || !internetAvailable()) return
        if (networkWait.expired(android.os.SystemClock.elapsedRealtime())) {
            cancelOfflineWait()
            LegacyBotRuntime.markRetryPending(false)
            allowRetry = false
            updateNotification("Ngừng bot: mất mạng quá 30 phút")
            stopSelf()
            return
        }
        if (!networkWait.restore(android.os.SystemClock.elapsedRealtime())) return
        cancelOfflineWait()
        LegacyBotRuntime.markRetryPending(false)
        scheduleWorkerRetry()
    }

    private fun scheduleWorkerRetry() {
        if (requestedStop || !allowRetry || activeAccountId == null) {
            updateNotification("Bot đã dừng")
            stopSelf()
            return
        }
        val delay = retry.nextDelayMs(true, false)
        if (delay <= 0L) {
            LegacyBotRuntime.markRetryPending(false)
            updateNotification("Bot đã dừng • hết lượt thử lại")
            stopSelf()
            return
        }
        LegacyBotRuntime.markRetryPending(true)
        event(ExecutionStage.RETRY_SCHEDULED)
        val id = activeAccountId!!
        val mode = activeMode
        val attempt = retry.used()
        updateNotification("Worker kết thúc • thử lại $attempt/3 sau ${delay/1000}s")
        val task = Runnable {
            scheduledRetry = null
            if (requestedStop || !allowRetry) return@Runnable
            if (!internetAvailable()) {
                // This attempt never reached startService, so preserve the user's 3-run budget.
                retry.refundUndispatchedAttempt()
                waitForInternet()
                return@Runnable
            }
            LegacyBotRuntime.markRetryPending(false)
            if (LegacyBotRuntime.active()) return@Runnable
            try {
                startService(Intent(this@BotForegroundService, BotForegroundService::class.java)
                    .setAction(ACTION_LEGACY_RETRY)
                    .putExtra(EXTRA_ACCOUNT_ID, id).putExtra(EXTRA_MODE, mode))
            } catch (_: RuntimeException) { stopSelf() }
        }
        scheduledRetry = task
        mainHandler.postDelayed(task, delay)
    }

    private val lifecycleMonitor = object : Runnable {
        override fun run() {
            if (!observingLegacy) return
            if (!LegacyBotRuntime.active()) {
                if (!requestedStop) event(if (LegacyBotRuntime.state() == "failed")
                    ExecutionStage.WORKER_ERROR else ExecutionStage.WORKER_EXITED)
                observingLegacy = false
                releaseCpuWakeLock()
                if (!requestedStop && allowRetry && workerStarted && activeAccountId != null) {
                    workerStarted = false
                    if (internetAvailable()) scheduleWorkerRetry() else waitForInternet()
                    return
                }
                updateNotification("Bot đã dừng")
                stopSelf()
                return
            }
            refreshWakeLockLease()
            val state = LegacyBotRuntime.state()
            val auth = LegacyBotRuntime.liveLoginState()
            // BotWorker creates a new Bot every cycle. ownerID is assigned only
            // after LoginSuccess, but the socket may have closed before this poll.
            // Count the evidence identity ONCE rather than trusting thread liveness.
            val cycleCount = LegacyBotRuntime.observedLoginCycles()
            val newLoginEvidence = cycleCount > lastObservedLoginCycles
            if (newLoginEvidence) lastObservedLoginCycles = cycleCount
            if (newLoginEvidence || (auth == "confirmed" && lastAuthState != "confirmed")) {
                event(if (loginObservedForStart) ExecutionStage.LOGIN_RECONFIRMED
                    else ExecutionStage.LIVE_LOGIN_CONFIRMED)
                loginObservedForStart = true
            }
            // The original JAR can keep its worker thread alive after repeated login
            // failures. Stop conservatively rather than show false long-running success.
            // This does not retry credentials or infer anything from opaque task results.
            if (state == "running") {
                val health = authWatchdog.observe(
                    if (newLoginEvidence) "confirmed" else auth,
                    android.os.SystemClock.elapsedRealtime())
                if (health == AuthWatchdogPolicy.INITIAL_AUTH_TIMEOUT ||
                    health == AuthWatchdogPolicy.RECONNECT_TIMEOUT) {
                    event(ExecutionStage.AUTH_STALLED)
                    requestedStop = true
                    allowRetry = false
                    retry.cancel()
                    cancelPendingRetry()
                    cancelOfflineWait()
                    LegacyBotRuntime.stop()
                    releaseCpuWakeLock()
                    observingLegacy = false
                    updateNotification("Đã ngừng: phiên game chưa xác nhận quá thời hạn")
                    stopSelf()
                    return
                }
            }
            if (state != lastNotifiedState || auth != lastAuthState) {
                lastAuthState = auth
                lastNotifiedState = state
                updateNotification(when {
                    state != "running" -> "Đang khởi chạy worker"
                    auth == "confirmed" -> "Đã nhận LoginSuccess • chưa xác minh thao tác Auto"
                    auth == "disconnected" -> "Phiên game đã đóng • worker còn chạy"
                    else -> "Worker đang chạy • chưa nhận LoginSuccess"
                })
            }
            mainHandler.postDelayed(this, 1500L)
        }
    }

    private fun observeLegacy() {
        observingLegacy = true
        mainHandler.removeCallbacks(lifecycleMonitor)
        mainHandler.postDelayed(lifecycleMonitor, 1500L)
    }

    private fun cancelPendingRetry() {
        scheduledRetry?.let(mainHandler::removeCallbacks)
        scheduledRetry = null
    }

    private fun acquireCpuWakeLock() {
        if (requestedStop) return
        if (wakeLock == null) {
            val power = getSystemService(PowerManager::class.java)
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AvatarBot:ActiveWorker")
                .apply { setReferenceCounted(false) }
        }
        // Renewable finite lease prevents a leaked permanent lock after Android kills the service.
        wakeLock?.acquire(15 * 60 * 1000L)
        lastWakeRenewMs = android.os.SystemClock.elapsedRealtime()
    }

    private fun refreshWakeLockLease() {
        if (wakeLock != null && android.os.SystemClock.elapsedRealtime() - lastWakeRenewMs >= 5 * 60 * 1000L)
            acquireCpuWakeLock()
    }

    private fun releaseCpuWakeLock() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: RuntimeException) { }
        wakeLock = null
        lastWakeRenewMs = 0L
    }

    companion object {
        const val ACTION_START = "com.avatarbot.mobile.START"
        const val ACTION_STOP = "com.avatarbot.mobile.STOP"
        const val ACTION_LEGACY_START = "com.avatarbot.mobile.LEGACY_START"
        const val ACTION_LEGACY_RETRY = "com.avatarbot.mobile.LEGACY_RETRY"
        const val ACTION_LEGACY_STOP = "com.avatarbot.mobile.LEGACY_STOP"
        const val EXTRA_ACCOUNT_ID = "account_id"
        const val EXTRA_MODE = "mode"
        private const val CHANNEL_ID = "avatarbot_active"
        private const val NOTIFICATION_ID = 101
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_LEGACY_STOP, ACTION_STOP -> {
                event(ExecutionStage.STOP_REQUESTED)
                requestedStop = true
                ActiveAutoSession.clear()
                allowRetry = false
                retry.cancel()
                cancelPendingRetry()
                cancelOfflineWait()
                observingLegacy = false
                mainHandler.removeCallbacks(lifecycleMonitor)
                releaseCpuWakeLock()
                LegacyBotRuntime.stop() // immediately closes pending sockets, including SOCKS5.
                io.execute { LocalBotRuntime.bridge.stop(); stopSelfResult(startId) }
                return START_NOT_STICKY
            }
            ACTION_LEGACY_START, ACTION_LEGACY_RETRY -> {
                val id = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                val mode = intent.getIntExtra(EXTRA_MODE, -1)
                val isRetry = intent.action == ACTION_LEGACY_RETRY
                // Do not let a second Start change account/mode, retry policy or notification
                // of an already running worker. A fresh Start requires an explicit Stop first.
                if (!isRetry && !StartAdmissionPolicy.acceptManualStart(
                        LegacyBotRuntime.active(), observingLegacy,
                        scheduledRetry != null, networkWait.waiting())) {
                    return START_NOT_STICKY
                }
                if (id.isNullOrBlank() || mode !in 0..3) {
                    if (!LegacyBotRuntime.active() && !observingLegacy) stopSelfResult(startId)
                    return START_NOT_STICKY
                }
                if (isRetry) {
                    if (requestedStop || !allowRetry || id != activeAccountId || mode != activeMode)
                        return START_NOT_STICKY
                } else {
                    cancelPendingRetry()
                    cancelOfflineWait()
                    LegacyBotRuntime.markRetryPending(false)
                    retry.reset()
                    requestedStop = false
                    allowRetry = false
                    activeAccountId = id
                    activeMode = mode
                    ActiveAutoSession.set(id, mode)
                }
                workerStarted = false
                loginObservedForStart = false
                lastObservedLoginCycles = 0L
                authWatchdog.reset()
                lastAuthState = "inactive"
                cancelOfflineWait()
                LegacyBotRuntime.markRetryPending(false)
                val ticket = LegacyBotRuntime.reserveStartTicket()
                if (ticket < 0L) return START_NOT_STICKY
                event(ExecutionStage.START_REQUESTED)
                try { postForegroundNotification() }
                catch (e: RuntimeException) {
                    if (LegacyBotRuntime.setFailure(ticket, e)) stopSelfResult(startId)
                    return START_NOT_STICKY
                }
                observeLegacy()
                io.execute {
                    try {
                        if (!LegacyBotRuntime.isStartReserved(ticket)) return@execute
                        val cfg = SecureConfigStore(this).load()
                        if (!LegacyBotRuntime.isStartReserved(ticket)) return@execute
                        val account = cfg.accounts.firstOrNull { it.id == id && it.enabled }
                            ?: error("Account unavailable")
                        val permission = when (mode) {
                            0 -> account.options.farm
                            1 -> account.options.fishing
                            2 -> account.options.diamond
                            else -> account.options.teacher
                        }
                        check(permission) { "Selected Auto mode disabled" }
                        if (mode == 0) check(LegacyFarmCompatibility.isCompatible(
                                account.options.harvest, account.options.water,
                                account.options.animals, account.options.starfruit)) {
                            LegacyFarmCompatibility.explanation()
                        }
                        val endpoint = GameCities.endpoint(account.city)
                        val host = endpoint.substringBeforeLast(':')
                        val port = endpoint.substringAfterLast(':').toInt()
                        if (!LegacyBotRuntime.isStartReserved(ticket) || requestedStop) return@execute
                        allowRetry = account.options.restartOnUnexpectedExit
                        if (account.options.keepCpuAwake) mainHandler.post {
                            if (!requestedStop && LegacyBotRuntime.isTicketCurrent(ticket) &&
                                (LegacyBotRuntime.isStartReserved(ticket) || LegacyBotRuntime.running())) {
                                acquireCpuWakeLock()
                            }
                        }
                        if (!LegacyBotRuntime.isStartReserved(ticket) || requestedStop) return@execute
                        workerStarted = LegacyBotRuntime.startReserved(ticket, account.username,
                            account.password, host, port, account.proxyUri.ifBlank { cfg.proxyUri },
                            mode, account.options.merchant, account.options.cooking,
                            account.options.javaMEFeatureMask(),
                            account.options.loopDelaySeconds,
                            if (account.options.plant) account.options.seedMode else "off",
                            account.options.fishingRounds,
                            account.options.teacherArea,
                            account.options.diamondSellKCX,
                            account.options.diamondSellNHB,
                            account.options.animalSellLate,
                            LegacyAdvancedSetup(
                                account.options.preferredSeedId, account.options.targetLandCount,
                                account.options.fishingMap, account.options.fishingArea,
                                account.options.fishingBaitId, account.options.fishingRodId,
                                account.options.diamondSellDurationMin, account.options.diamondSellIntervalMs,
                                account.options.farmAutoSell, account.options.farmSellProductIds,
                                account.options.farmSellThreshold, account.options.farmRebuySold,
                                account.options.farmStarUpgrade, account.options.farmStarTargetLevel))
                        if (workerStarted) event(ExecutionStage.WORKER_STARTED)
                    } catch (e: Exception) {
                        // Never retry a failed *start*, missing credentials, or invalid config.
                        if (LegacyBotRuntime.setFailure(ticket, e)) {
                            event(ExecutionStage.START_FAILED)
                            allowRetry = false
                            mainHandler.post { if (!LegacyBotRuntime.active()) stopSelfResult(startId) }
                        }
                    }
                }
                return START_NOT_STICKY
            }
            ACTION_START -> {
                // An obsolete Go-only action must never destroy an active Java ME worker.
                if (LegacyBotRuntime.active() || observingLegacy) return START_NOT_STICKY
                val core = LocalBotRuntime.bridge
                if (!core.available || !core.capabilities().optBoolean("gameAutomation", false)) {
                    stopSelfResult(startId); return START_NOT_STICKY
                }
                postForegroundNotification()
                io.execute { try { core.start() } catch (_: Exception) { stopSelfResult(startId) } }
                return START_NOT_STICKY
            }
            else -> {
                if (!LegacyBotRuntime.active() && !observingLegacy) stopSelfResult(startId)
                return START_NOT_STICKY
            }
        }
    }

    private fun buildNotification(message: String): Notification {
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stopPending = PendingIntent.getService(this, 1,
            Intent(this, BotForegroundService::class.java).setAction(ACTION_LEGACY_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("AvatarBot 2X • đang chạy trên điện thoại")
            .setContentText(message)
            .setContentIntent(pending)
            .addAction(Notification.Action.Builder(
                android.R.drawable.ic_media_pause, "Dừng bot", stopPending).build())
            .setOngoing(true)
            .build()
    }

    private fun postForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, "AvatarBot đang hoạt động", NotificationManager.IMPORTANCE_LOW))
        val notification = buildNotification("Đang chuẩn bị phiên Auto")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(message))
    }

    override fun onDestroy() {
        ActiveAutoSession.clear()
        requestedStop = true
        retry.cancel()
        cancelPendingRetry()
        cancelOfflineWait()
        observingLegacy = false
        mainHandler.removeCallbacks(lifecycleMonitor)
        releaseCpuWakeLock()
        LegacyBotRuntime.stop()
        io.shutdownNow()
        runCatching { LocalBotRuntime.bridge.stop() }
        super.onDestroy()
    }
}
