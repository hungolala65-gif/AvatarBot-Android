package com.avatarbot.mobile.legacy;

import farm2x.Bot;
import farm2x.BotWorker;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import farm2x.Config;
import farm2x.Log;
import javax.microedition.io.Connector;
import java.util.Arrays;

/**
 * Headless adapter for the user-supplied Java ME JAR. Not a reimplementation.
 * Explicitly starts ONE worker only; JAR globals (Wallet, Log, TeacherStore,
 * Stats) are shared and must not be used with multiple accounts concurrently.
 */
public final class LegacyBotRuntime {
    public static final int FARM=0, FISH=1, DIAMOND=2, TEACHER=3;
    private static BotWorker worker;
    private static Thread thread;
    private static String currentUser="";
    private static String currentPassword="";
    private static int currentMode=-1;
    private static String lastError="";
    private static boolean starting=false;
    private static volatile Bot authProbeBot;
    private static volatile Thread authProbeThread;
    private static volatile String authProbeState="idle";
    private static volatile boolean retryPending=false;
    private static volatile boolean networkWaiting=false;
    private static String authProbeHostPort="";
    private static String authProbeUser="";
    private static volatile long startedAtNano=0L;
    // Evidence counter is per worker run, not per account across runs.
    private static Object lastAuthenticatedBotIdentity;
    private static long observedLoginCycles;
    private static volatile String authProbeMessage="Chưa kiểm tra đăng nhập";
    /** Monotonic identifier invalidating pending async launch requests on Stop. */
    private static long launchGeneration=0;
    private LegacyBotRuntime() { }

    /** Reserve a start request before asynchronous Android storage reads begin. */
    public static synchronized long reserveStartTicket() {
        if (starting || running() || retryPending) return -1;
        starting = true;
        lastError = "";
        return ++launchGeneration;
    }
    /** Retained for older JVM integration tests. */
    public static synchronized boolean reserveStart() { return reserveStartTicket() >= 0; }
    public static synchronized boolean isStartReserved(long ticket) {
        return starting && launchGeneration == ticket;
    }
    /** For queued Android callbacks: reject stale generations after Stop/new Start. */
    public static synchronized boolean isTicketCurrent(long ticket) {
        return launchGeneration == ticket;
    }
    /** A queued start must never restart a worker after the user pressed Stop. */
    public static synchronized boolean startReserved(long ticket, String username, String password,
        String host, int port, String proxyUri, int mode, boolean trader, boolean cooking) {
        if (!isStartReserved(ticket)) return false;
        start(username, password, host, port, proxyUri, mode, trader, cooking);
        return true;
    }
    /** Extended start maps explicitly opted-in task flags to JAR Config fields. */
    public static synchronized boolean startReserved(long ticket, String username, String password,
        String host, int port, String proxyUri, int mode, boolean trader, boolean cooking, int featureMask) {
        if (!isStartReserved(ticket)) return false;
        start(username, password, host, port, proxyUri, mode, trader, cooking, featureMask, 15);
        return true;
    }
    /** User-selected interval; bounded to avoid high-frequency reconnect pressure. */
    public static synchronized boolean startReserved(long ticket, String username, String password,
        String host, int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds) {
        return startReserved(ticket, username, password, host, port, proxyUri, mode, trader,
            cooking, featureMask, loopDelaySeconds, "preferred");
    }
    /** JAR seedMode: off stops new planting; harvest_replay chooses the just-harvested seed. */
    public static synchronized boolean startReserved(long ticket, String username, String password,
        String host, int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds, String seedMode) {
        if (!isStartReserved(ticket)) return false;
        start(username, password, host, port, proxyUri, mode, trader, cooking,
            featureMask, loopDelaySeconds, seedMode);
        return true;
    }
    /** Additional mode-specific configuration. All destructive sales must be opted-in. */
    public static synchronized boolean startReserved(long ticket, String username, String password,
        String host, int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds, String seedMode,
        int fishingRounds, int teacherArea, boolean diamondSellKCX, boolean diamondSellNHB) {
        if (!isStartReserved(ticket)) return false;
        start(username, password, host, port, proxyUri, mode, trader, cooking,
            featureMask, loopDelaySeconds, seedMode,
            fishingRounds, teacherArea, diamondSellKCX, diamondSellNHB, false);
        return true;
    }
    public static synchronized boolean startReserved(long ticket, String username, String password,
        String host, int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds, String seedMode,
        int fishingRounds, int teacherArea, boolean diamondSellKCX, boolean diamondSellNHB,
        boolean animalSellLate) {
        return startReserved(ticket, username, password, host, port, proxyUri, mode, trader,
            cooking, featureMask, loopDelaySeconds, seedMode, fishingRounds, teacherArea,
            diamondSellKCX, diamondSellNHB, animalSellLate, new LegacyAdvancedSetup());
    }
    public static synchronized boolean startReserved(long ticket, String username, String password,
        String host, int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds, String seedMode,
        int fishingRounds, int teacherArea, boolean diamondSellKCX, boolean diamondSellNHB,
        boolean animalSellLate, LegacyAdvancedSetup extra) {
        if (!isStartReserved(ticket)) return false;
        start(username, password, host, port, proxyUri, mode, trader, cooking,
            featureMask, loopDelaySeconds, seedMode, fishingRounds, teacherArea,
            diamondSellKCX, diamondSellNHB, animalSellLate, extra);
        return true;
    }
    /** A login-only check sends the JAR handshake and Login packet; it never runs any Auto cycle.
     *  It must run off the Android main thread. The same worker reservation prevents overlap.
     */
    public static String testLogin(String username, String password, String host, int port, String proxyURI) {
        long ticket = reserveStartTicket();
        if (ticket < 0) return "busy";
        synchronized (LegacyBotRuntime.class) {
            authProbeState = "testing";
            authProbeMessage = "Đang kết nối và chờ phản hồi đăng nhập từ game";
            authProbeHostPort = host + ":" + port;
            authProbeUser = username == null ? "" : username;
            authProbeThread = Thread.currentThread();
            currentUser = username == null ? "" : username;
            currentPassword = password == null ? "" : password;
        }
        Bot bot = null;
        try {
            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty())
                throw new IllegalArgumentException("Missing account credentials");
            Config cfg = createConfig(username, password, host, port, FARM, false, false, 0);
            if (!isStartReserved(ticket)) return "cancelled";
            Connector.authorize(host, port, proxyURI);
            Log.clear();
            bot = new Bot(cfg);
            synchronized (LegacyBotRuntime.class) {
                if (isStartReserved(ticket)) authProbeBot = bot;
            }
            if (!isStartReserved(ticket)) return "cancelled";
            Method login = Bot.class.getDeclaredMethod("connectLogin", boolean.class);
            login.setAccessible(true);
            boolean authenticated = Boolean.TRUE.equals(login.invoke(bot, false));
            synchronized (LegacyBotRuntime.class) {
                if (!isStartReserved(ticket)) return "cancelled";
                authProbeState = authenticated ? "success" : "rejected";
                authProbeMessage = authenticated
                    ? "Server đã trả LoginSuccess. Kiểm tra đăng nhập thành công; chưa chạy Auto."
                    : "Không nhận được LoginSuccess. Có thể sai tài khoản, khác phiên bản game, bị server từ chối hoặc hết thời gian; xem Logs.";
            }
            return authenticated ? "success" : "rejected";
        } catch (Throwable original) {
            Throwable e = original;
            if (e instanceof InvocationTargetException && ((InvocationTargetException)e).getCause()!=null)
                e = ((InvocationTargetException)e).getCause();
            synchronized (LegacyBotRuntime.class) {
                if (!isStartReserved(ticket)) return "cancelled";
                authProbeState = "error";
                authProbeMessage = "Không thực hiện được Login: " + e.getClass().getSimpleName() +
                    ". Kiểm tra kết nối/SOCKS5 hoặc phiên bản server rồi xem Logs.";
            }
            return "error";
        } finally {
            if (bot != null) bot.stop();
            // Revoke only if this test still owns the network permission; don't close a new run.
            synchronized (LegacyBotRuntime.class) {
                if (authProbeBot == bot) authProbeBot = null;
                if (authProbeThread == Thread.currentThread()) authProbeThread = null;
                if (launchGeneration == ticket) {
                    Connector.revoke();
                    starting = false;
                }
            }
        }
    }
    public static String probeState() { return authProbeState; }
    public static String probeMessage() { return authProbeMessage; }
    /** Prevents a LoginSuccess for one account/credential/city being shown as another's result. */
    public static synchronized boolean probeMatches(String username, String password, String endpoint) {
        return username != null && password != null && endpoint != null
            && username.equals(authProbeUser) && password.equals(currentPassword)
            && endpoint.equals(authProbeHostPort);
    }
    /** Wait state remains cancellable from UI even when no worker thread exists. */
    public static synchronized void markRetryPending(boolean value) {
        retryPending=value;
        if (!value) networkWaiting=false;
    }
    public static synchronized void markNetworkWaiting(boolean value) {
        networkWaiting=value;
        retryPending=value;
    }
    public static synchronized boolean active() { return starting || running() || retryPending; }
    public static synchronized long uptimeSeconds() {
        if (!running() || startedAtNano == 0) return 0L;
        return Math.max(0L, (System.nanoTime()-startedAtNano) / 1000000000L);
    }
    public static synchronized String state() {
        if (starting) return "starting";
        if (networkWaiting) return "network_wait";
        if (retryPending) return "retry_wait";
        if (running()) return "running";
        return lastError.isEmpty() ? "stopped" : "failed";
    }
    /** For explicit user-selected account only. Never log credentials. */
    public static synchronized void start(String username, String password, String host,
        int port, String proxyUri, int mode, boolean trader, boolean cooking) {
        start(username, password, host, port, proxyUri, mode, trader, cooking, 0);
    }
    public static synchronized void start(String username, String password, String host,
        int port, String proxyUri, int mode, boolean trader, boolean cooking, int featureMask) {
        start(username, password, host, port, proxyUri, mode, trader, cooking, featureMask, 15);
    }
    public static synchronized void start(String username, String password, String host,
        int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds) {
        start(username, password, host, port, proxyUri, mode, trader, cooking,
            featureMask, loopDelaySeconds, "preferred");
    }
    public static synchronized void start(String username, String password, String host,
        int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds, String seedMode) {
        start(username, password, host, port, proxyUri, mode, trader, cooking,
            featureMask, loopDelaySeconds, seedMode, 0, -1, false, false, false);
    }
    public static synchronized void start(String username, String password, String host,
        int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds, String seedMode,
        int fishingRounds, int teacherArea, boolean diamondSellKCX, boolean diamondSellNHB,
        boolean animalSellLate) {
        start(username, password, host, port, proxyUri, mode, trader, cooking,
            featureMask, loopDelaySeconds, seedMode, fishingRounds, teacherArea,
            diamondSellKCX, diamondSellNHB, animalSellLate, new LegacyAdvancedSetup());
    }
    public static synchronized void start(String username, String password, String host,
        int port, String proxyUri, int mode, boolean trader, boolean cooking,
        int featureMask, int loopDelaySeconds, String seedMode, int fishingRounds,
        int teacherArea, boolean diamondSellKCX, boolean diamondSellNHB,
        boolean animalSellLate, LegacyAdvancedSetup extra) {
        if (thread != null && thread.isAlive()) {
            starting = false;
            throw new IllegalStateException("A bot is already running");
        }
        if (mode<0 || mode>3) throw new IllegalArgumentException("Invalid mode");
        if (username==null || username.trim().isEmpty() || password==null || password.isEmpty())
            throw new IllegalArgumentException("Missing account credentials");
        if (username.length()>256 || password.length()>512) throw new IllegalArgumentException("Account fields too long");
        lastError="";
        // Auto start does not inherit an old login-only result.
        authProbeState="idle";
        authProbeMessage="Phiên Auto mới; cần xem Logs để biết xác thực game";
        try {
            Connector.authorize(host,port,proxyUri);
            Config cfg=createConfig(username,password,host,port,mode,trader,cooking,
                featureMask,loopDelaySeconds,seedMode,
                fishingRounds,teacherArea,diamondSellKCX,diamondSellNHB,animalSellLate,extra);
            Log.clear();
            BotWorker bot=new BotWorker(cfg,mode);
            Thread runner=new Thread(() -> {
                try { bot.run(); }
                catch (Throwable t) {
                    synchronized (LegacyBotRuntime.class) { lastError=t.getClass().getSimpleName(); }
                    Log.add("Worker dừng do lỗi: " + t.getClass().getSimpleName());
                }
                finally { Connector.revoke(); }
            },"AvatarBot-Legacy-Worker");
            observedLoginCycles=0L;
            lastAuthenticatedBotIdentity=null;
            worker=bot; thread=runner;currentMode=mode;currentUser=username;currentPassword=password;
            runner.setDaemon(true);
            startedAtNano=System.nanoTime();
            retryPending=false;
            runner.start();
            starting = false;
        } catch (RuntimeException ex) { starting = false; Connector.revoke(); throw ex; }
    }
    /** Configuration mapping is isolated so it can be tested without a live server. */
    static Config createConfig(String username, String password, String host, int port,
        int mode, boolean trader, boolean cooking) {
        return createConfig(username, password, host, port, mode, trader, cooking, 0);
    }
    static Config createConfig(String username, String password, String host, int port,
        int mode, boolean trader, boolean cooking, int featureMask) {
        return createConfig(username, password, host, port, mode, trader, cooking, featureMask, 15);
    }
    static Config createConfig(String username, String password, String host, int port,
        int mode, boolean trader, boolean cooking, int featureMask, int loopDelaySeconds) {
        return createConfig(username, password, host, port, mode, trader, cooking,
            featureMask, loopDelaySeconds, "preferred");
    }
    static Config createConfig(String username, String password, String host, int port,
        int mode, boolean trader, boolean cooking, int featureMask, int loopDelaySeconds,
        String seedMode) {
        return createConfig(username, password, host, port, mode, trader, cooking,
            featureMask, loopDelaySeconds, seedMode, 0, -1, false, false, false);
    }
    static Config createConfig(String username, String password, String host, int port,
        int mode, boolean trader, boolean cooking, int featureMask, int loopDelaySeconds,
        String seedMode, int fishingRounds, int teacherArea,
        boolean diamondSellKCX, boolean diamondSellNHB,
        boolean animalSellLate) {
        if (fishingRounds < 0 || fishingRounds > 100)
            throw new IllegalArgumentException("Fishing rounds must be 0..100");
        if (teacherArea < -1 || teacherArea > 99)
            throw new IllegalArgumentException("Teacher area must be -1..99");
        if (!"preferred".equals(seedMode) && !"harvest_replay".equals(seedMode) && !"off".equals(seedMode))
            throw new IllegalArgumentException("Unsupported seed mode");
        if ((featureMask & ~31) != 0) throw new IllegalArgumentException("Unknown JAR flags");
        if (loopDelaySeconds < 15 || loopDelaySeconds > 300)
            throw new IllegalArgumentException("Invalid loop delay");
        Config cfg=Config.load(); // JAR default store is volatile; secrets never persisted here.
        cfg.host=host; cfg.port=port;
        cfg.username=username; cfg.password=password;
        cfg.autoFishing=(mode==FISH);
        cfg.autoDiamond=(mode==DIAMOND);
        cfg.autoTeacher=(mode==TEACHER);
        cfg.traderAssist=trader;
        cfg.kitchenEnabled=cooking;
        cfg.loopDelayMs=loopDelaySeconds*1000;
        cfg.seedMode=seedMode;
        cfg.autoDailyQuest=(featureMask & 1) != 0;
        cfg.autoQuestHugo=(featureMask & 2) != 0;
        cfg.autoQuestEvent=(featureMask & 4) != 0;
        cfg.autoFarmMission=(featureMask & 8) != 0;
        cfg.autoFarmCheckin=(featureMask & 16) != 0;
        cfg.autoHarvestLove=false; // Not equivalent to the Android "starfruit" toggle.
        // Config.load() defaults diamond sales to TRUE. Never sell unless opted in.
        cfg.diamondSellKCX=(mode==DIAMOND && diamondSellKCX);
        cfg.diamondSellNHB=(mode==DIAMOND && diamondSellNHB);
        cfg.fishingRounds=(mode==FISH ? fishingRounds : 0);
        cfg.teacherArea=(mode==TEACHER ? teacherArea : -1);
        // Avoid accidental purchases/auto-selling inherited from Java ME RMS config.
        cfg.autoSell=false;
        // JAR may buy supplies with premium 'Lượng' unless explicitly prohibited.
        // Conservative default protects trial accounts from unrequested spending.
        cfg.noLuongBuy=true;
        cfg.animalBuyMode="off";
        // Bot.animalCare queues meat animals for sale for any NON-EMPTY mode,
        // and Bot.eggCare sells mature egg animals when mode is "late".
        // Therefore "off" is NOT a safe value; empty string disables both paths.
        cfg.animalSellMode=(mode==FARM && animalSellLate) ? "late" : "";
        cfg.autoStarUpgrade=false;
        // JAR defaults secretly switch Fishing/Diamond to farm after 30 min.
        // A selected Auto mode should never silently run another mode.
        cfg.fishFarmIntervalMin=0;
        cfg.diamondFarmIntervalMin=0;
        return cfg;
    }
    /** New optional per-module fields preserve the original factory overloads and defaults. */
    static Config createConfig(String username, String password, String host, int port,
        int mode, boolean trader, boolean cooking, int featureMask, int loopDelaySeconds,
        String seedMode, int fishingRounds, int teacherArea, boolean diamondSellKCX,
        boolean diamondSellNHB, boolean animalSellLate, LegacyAdvancedSetup extra) {
        if (extra == null) throw new IllegalArgumentException("Missing advanced options");
        extra.validate();
        Config cfg = createConfig(username,password,host,port,mode,trader,cooking,featureMask,
            loopDelaySeconds,seedMode,fishingRounds,teacherArea,diamondSellKCX,
            diamondSellNHB,animalSellLate);
        if (mode == FARM) {
            cfg.preferredSeedID = extra.preferredSeedId;
            cfg.targetLandCount = extra.targetLandCount;
            extra.applyFarm(cfg);
        } else if (mode == FISH) {
            cfg.fishingMap = extra.fishingMap;
            cfg.fishingArea = extra.fishingArea;
            cfg.fishingBaitID = extra.fishingBaitId;
            cfg.fishingRodID = extra.fishingRodId;
        } else if (mode == DIAMOND) {
            cfg.diamondSellDurationMin = extra.diamondSellDurationMin;
            cfg.diamondSellIntervalMs = extra.diamondSellIntervalMs;
        }
        return cfg;
    }
    /** Avoid surfacing password/host data inside exception messages. */
    public static synchronized void setFailure(Throwable cause) {
        starting=false;
        lastError=cause==null?"Unknown error":cause.getClass().getSimpleName();
    }
    /** A failure from an obsolete queued launch must not alter a newer worker. */
    public static synchronized boolean setFailure(long ticket, Throwable cause) {
        // start() can clear `starting` before throwing, but its ticket remains valid.
        if (ticket != launchGeneration) return false;
        setFailure(cause);
        return true;
    }
    public static synchronized String error() { return lastError; }
    public static synchronized void stop() {
        launchGeneration++;
        retryPending=false;
        networkWaiting=false;
        starting=false;
        lastError="";
        Connector.revoke();
        if (authProbeBot!=null) authProbeBot.stop();
        if (authProbeThread!=null) authProbeThread.interrupt();
        if ("testing".equals(authProbeState)) {
            authProbeState = "cancelled";
            authProbeMessage = "Đã hủy kiểm tra đăng nhập";
        }
        if (worker!=null) worker.stop();
        if (thread!=null) thread.interrupt();
    }
    public static synchronized boolean running() { return thread!=null && thread.isAlive(); }
    /**
     * Count at most one real LoginSuccess per original-JAR Bot object. Unlike
     * liveLoginState(), this evidence survives a brief socket close at the end
     * of a healthy cycle (until the next Bot cycle replaces the object).
     * It must NEVER be called a completed farm/fish/teacher/diamond action.
     */
    public static synchronized long observedLoginCycles() {
        if (running()) {
            Object identity=LegacySessionInspector.authenticatedCycleIdentity(worker);
            if (identity != null && identity != lastAuthenticatedBotIdentity) {
                lastAuthenticatedBotIdentity=identity;
                observedLoginCycles++;
            }
        }
        return observedLoginCycles;
    }
    /** Verify the currently open JAR session; never infer login from thread.isAlive(). */
    public static synchronized String liveLoginState() {
        if (!running()) return "inactive";
        return LegacySessionInspector.inspect(worker);
    }
    public static synchronized int mode() { return currentMode; }
    public static synchronized String username() { return currentUser; }
    /** Masks username and password; the UI additionally masks all stored accounts. */
    public static synchronized String[] logs() {
        String[] raw=Log.snapshot();
        int from=Math.max(0,raw.length-120);
        String[] out=Arrays.copyOfRange(raw,from,raw.length);
        for (int i=0;i<out.length;i++) {
            String s=out[i]==null?"":out[i];
            if (!currentUser.isEmpty()) s=s.replace(currentUser,"[account]");
            if (!currentPassword.isEmpty()) s=s.replace(currentPassword,"[secret]");
            out[i]=s.length()>512?s.substring(0,512):s;
        }
        return out;
    }
}
