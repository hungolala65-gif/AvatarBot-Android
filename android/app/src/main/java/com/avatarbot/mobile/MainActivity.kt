package com.avatarbot.mobile

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.avatarbot.mobile.data.AccountImporter
import com.avatarbot.mobile.data.AutoOptions
import com.avatarbot.mobile.data.BotAccount
import com.avatarbot.mobile.data.BotConfig
import com.avatarbot.mobile.data.GameCities
import com.avatarbot.mobile.data.DiagnosticRedactor
import com.avatarbot.mobile.data.SecureConfigStore
import com.avatarbot.mobile.engine.LocalBotRuntime
import com.avatarbot.mobile.legacy.LegacyBotRuntime
import com.avatarbot.mobile.legacy.LegacyFarmCompatibility
import com.avatarbot.mobile.legacy.LegacyControlPanel
import com.avatarbot.mobile.service.BotForegroundService
import com.avatarbot.mobile.ui.AutoSetupScreen
import com.avatarbot.mobile.data.SecureExecutionJournal
import com.avatarbot.mobile.data.ExecutionStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Accent = Color(0xFF72DB91)
private val Surface = Color(0xFF17241F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Credentials / proxy secrets must not appear in Android screenshots.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent, secondary = Color(0xFF9FCBAA), surface = Surface,
                background = Color(0xFF0C1511)
            )) { AvatarBotApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarBotApp() {
    val context = LocalContext.current
    val vault = remember { SecureConfigStore(context) }
    val loaded = remember { runCatching { vault.load() } }
    val storageBlocked = loaded.isFailure
    var config by remember { mutableStateOf(loaded.getOrDefault(BotConfig())) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<BotAccount?>(null) }
    var adding by remember { mutableStateOf(false) }
    var bulkImport by remember { mutableStateOf(false) }
    var editingOptions by remember { mutableStateOf<BotAccount?>(null) }
    var pendingDelete by remember { mutableStateOf<BotAccount?>(null) }
    var info by remember { mutableStateOf("") }
    var probeMessage by remember { mutableStateOf("") }
    var probing by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(emptyList<String>()) }
    var localLogs by remember { mutableStateOf(emptyList<String>()) }
    var legacyLogs by remember { mutableStateOf(emptyList<String>()) }
    val coroutine = rememberCoroutineScope()
    val go = remember { LocalBotRuntime.bridge }
    val caps = remember(go) { runCatching { go.capabilities() }.getOrDefault(JSONObject()) }
    val canAuto = caps.optBoolean("gameAutomation", false) && !storageBlocked
    var city by rememberSaveable { mutableStateOf("hoanmy") }
    var proxyInput by remember(config.proxyUri) { mutableStateOf(config.proxyUri) }
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(go, storageBlocked) {
        if (go.available && !storageBlocked) {
            withContext(Dispatchers.IO) { runCatching { go.pushAccounts(config.accounts) } }
        }
    }

    fun persist(next: BotConfig) {
        if (storageBlocked) { info = "Kho lưu trữ không đọc được. Không ghi đè dữ liệu cũ."; return }
        runCatching { vault.save(next) }.onSuccess {
            config = next
            info = "Đã lưu dữ liệu vào kho mã hóa của điện thoại"
            if (go.available) {
                runCatching { go.pushAccounts(next.accounts) }.onFailure {
                    info = "Đã lưu nhưng không cập nhật được Go core: ${it.javaClass.simpleName}"
                }
            }
        }.onFailure { info = "Lỗi lưu dữ liệu: ${it.javaClass.simpleName}" }
    }

    val startLegacy: (String, Int) -> Unit = { accountId, legacyMode ->
        if (storageBlocked) {
            info = "Không đọc được kho mã hóa nên không khởi chạy"
        } else if (LegacyBotRuntime.active()) {
            info = "Đang có phiên hoạt động; hãy nhấn Dừng trước khi đổi tài khoản hoặc chế độ Auto."
        } else {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            runCatching {
                ContextCompat.startForegroundService(context,
                    Intent(context, BotForegroundService::class.java)
                        .setAction(BotForegroundService.ACTION_LEGACY_START)
                        .putExtra(BotForegroundService.EXTRA_ACCOUNT_ID, accountId)
                        .putExtra(BotForegroundService.EXTRA_MODE, legacyMode))
            }.onSuccess { info = "Đã yêu cầu khởi chạy Java ME; xem Logs để biết kết quả" }
                .onFailure { info = "Lỗi Android Service: " + it.javaClass.simpleName }
        }
    }
    val testLogin: (String) -> Unit = { id ->
        val account = config.accounts.firstOrNull { it.id == id && it.enabled }
        if (storageBlocked || account == null) {
            info = "Chưa có tài khoản hợp lệ để thử đăng nhập"
        } else if (LegacyBotRuntime.active()) {
            info = "Đang có tác vụ khác; hãy dừng trước khi thử đăng nhập"
        } else {
            info = "Đang kiểm tra đăng nhập cho ${GameCities.label(account.city)} bằng JAR (không bật Auto)..."
            coroutine.launch {
                val outcome = withContext(Dispatchers.IO) {
                    runCatching {
                        val endpoint = GameCities.endpoint(account.city)
                        LegacyBotRuntime.testLogin(account.username, account.password,
                            endpoint.substringBeforeLast(':'), endpoint.substringAfterLast(':').toInt(),
                            account.proxyUri.ifBlank { config.proxyUri })
                    }
                }
                outcome.onSuccess { result ->
                    val stage = when (result) {
                        "success" -> ExecutionStage.LOGIN_ACCEPTED
                        "rejected" -> ExecutionStage.LOGIN_NOT_CONFIRMED
                        "error" -> ExecutionStage.LOGIN_ERROR
                        else -> null
                    }
                    if (stage != null) runCatching {
                        SecureExecutionJournal(context).append(account.id, -1, stage)
                    }
                }
                info = outcome.fold(
                    onSuccess = { LegacyBotRuntime.probeMessage() },
                    onFailure = { "Không thể kiểm tra đăng nhập: " + it.javaClass.simpleName +
                        ". Kiểm tra thành phố và cấu hình SOCKS5." }
                )
            }
        }
    }
    val stopLegacy: () -> Unit = {
        runCatching {
            context.startService(Intent(context, BotForegroundService::class.java)
                .setAction(BotForegroundService.ACTION_LEGACY_STOP))
        }.onSuccess { info = "Đã yêu cầu dừng bot" }
            .onFailure {
                LegacyBotRuntime.stop()
                info = "Đã ngắt kết nối; Android Service báo: " + it.javaClass.simpleName
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("AvatarBot 2X", fontWeight = FontWeight.Bold)
                    Text("Android Native • Local Core", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        },
        bottomBar = {
            // The dedicated Setup pages control back navigation so unsaved options aren't lost
            // by switching tabs without a confirmation dialog.
            if (editingOptions == null) NavigationBar {
                listOf("🏠" to "Tổng quan", "👤" to "Tài khoản", "⚙" to "Auto", "🔌" to "Kết nối", "📋" to "Logs")
                    .forEachIndexed { index, item ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index },
                            icon = { Text(item.first) }, label = { Text(item.second) })
                    }
            }
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (storageBlocked) {
                WarningCard("Không đọc được dữ liệu mã hóa. Có thể khóa Android Keystore đã thay đổi. " +
                    "Ứng dụng giữ nguyên dữ liệu hiện tại để tránh mất thông tin; hãy sao lưu trước khi đặt lại.")
            }
            if (info.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF263B31))) {
                    Text(info, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            when(tab) {
                0 -> {
                    OverviewScreen(config)
                    LegacyControlPanel(config, onStart = startLegacy, onStop = stopLegacy,
                        onTestLogin = testLogin)
                }
                1 -> AccountsScreen(config.accounts, !storageBlocked,
                    onAdd = { editing = null; adding = true },
                    onImport = { bulkImport = true },
                    onOptions = { editingOptions = it; tab = 2 },
                    onEdit = { editing = it; adding = true },
                    onToggle = { changed ->
                        persist(config.copy(accounts = config.accounts.map {
                            if (it.id == changed.id) it.copy(enabled = !it.enabled) else it
                        }))
                    },
                    onDelete = { removed -> pendingDelete = removed })
                2 -> {
                    val selected = editingOptions?.let { chosen -> config.accounts.firstOrNull { it.id == chosen.id } }
                    if (selected != null) {
                        key(selected.id) { AutoSetupScreen(selected, editable = !storageBlocked,
                            onBack = { editingOptions = null },
                            onSave = { options ->
                                persist(config.copy(accounts = config.accounts.map { a ->
                                    if (a.id == selected.id) a.copy(options = options) else a
                                }))
                            },
                            onStart = startLegacy, onStop = stopLegacy, onTestLogin = testLogin) }
                    } else {
                        AutoModulesScreen(config.accounts, !storageBlocked) { editingOptions = it }
                        LegacyControlPanel(config, onStart = startLegacy, onStop = stopLegacy,
                            onTestLogin = testLogin)
                    }
                }
                3 -> ConnectionScreen(city, onCity = { city = it }, proxyInput = proxyInput,
                    onProxyChange = { proxyInput = it },
                    onSaveProxy = { persist(config.copy(proxyUri = proxyInput.trim())) },
                    onProbe = {
                        probing = true
                        coroutine.launch {
                            val endpoint = GameCities.endpoint(city)
                            probeMessage = withContext(Dispatchers.IO) {
                                runCatching {
                                    val r = go.probe(endpoint, proxyInput.trim())
                                    if (r.optBoolean("ok")) "TCP kết nối thành công ($endpoint) • ${r.optLong("latencyMs")} ms" 
                                    else "Không thể kết nối: ${r.optString("error", "unknown")}" 
                                }.getOrElse { "Kiểm tra lỗi: ${it.message ?: it.javaClass.simpleName}" }
                            }
                            localLogs = (localLogs + "${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())} Kiểm tra TCP: ${if (probeMessage.startsWith("TCP kết nối")) "thành công" else "không thành công"}").takeLast(120)
                            probing = false
                        }
                    }, probeResult = probeMessage, probing = probing, canProbe = true)
                else -> {
                    val sensitiveValues = buildList {
                        add(config.proxyUri)
                        config.accounts.forEach { a ->
                            add(a.username); add(a.password); add(a.proxyUri)
                        }
                    }
                    val safeLogs = (logs + localLogs + legacyLogs).takeLast(120).map {
                        DiagnosticRedactor.redact(it, sensitiveValues)
                    }
                    LogsScreen(safeLogs, onRefresh = {
                    legacyLogs = LegacyBotRuntime.logs().map { line ->
                        config.accounts.fold(line) { masked, a ->
                            masked.replace(a.username, "[account]")
                                .let { if (a.password.isNotBlank()) it.replace(a.password, "[secret]") else it }
                        }
                    }
                    val state = runCatching { go.state() }.getOrDefault(JSONObject())
                    val goLogs = state.optJSONArray("logs")
                    logs = if (goLogs != null) (0 until goLogs.length()).map { goLogs.optString(it) } else emptyList()
                })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (bulkImport) {
        BulkImportDialog(accounts = config.accounts, onDismiss = { bulkImport = false },
            onImport = { imported, importCity, cloneId ->
            val templateOptions = config.accounts.firstOrNull { it.id == cloneId }?.options
            val present = config.accounts.map { "${it.city}:${it.username.lowercase()}" }.toSet()
            val newAccounts = imported.filterNot { "${importCity}:${it.username.lowercase()}" in present }.map {
                BotAccount(username = it.username, password = it.password, label = it.label, city = importCity,
                    options = templateOptions ?: AutoOptions())
            }
            if (config.accounts.size + newAccounts.size > 1000) {
                info = "Tối đa 1000 tài khoản"
            } else if (newAccounts.isEmpty()) {
                info = "Không có tài khoản mới (các tài khoản đã tồn tại)"
            } else {
                persist(config.copy(accounts = config.accounts + newAccounts))
                info = "Đã nhập ${newAccounts.size} tài khoản vào ${GameCities.label(importCity)}"
                bulkImport = false
            }
        })
    }
    pendingDelete?.let { account ->
        AlertDialog(onDismissRequest = { pendingDelete = null },
            title = { Text("Xóa tài khoản?") },
            text = { Text("Xóa ${account.username} khỏi kho mã hóa trên điện thoại? Thao tác này không thể hoàn tác.") },
            confirmButton = { TextButton(onClick = {
                persist(config.copy(accounts = config.accounts.filterNot { it.id == account.id }))
                pendingDelete = null
            }) { Text("Xóa") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Hủy") } })
    }
    if (adding) {
        AccountEditor(existing = editing, onDismiss = { adding = false }, onSave = { value ->
            if (value.username.isBlank() || value.password.isBlank()) {
                info = "Tài khoản và mật khẩu không được để trống"
            } else if (value.username.trim().length > 256 || value.password.length > 512) {
                info = "Tên đăng nhập hoặc mật khẩu quá dài"
            } else if (config.accounts.size >= 1000 && editing == null) {
                info = "Tối đa 1000 tài khoản"
            } else if (config.accounts.any { it.username.equals(value.username.trim(), ignoreCase = true) &&
                    it.city == value.city && it.id != value.id }) {
                info = "Tài khoản này đã có ở thành phố đã chọn"
            } else if (value.proxyUri.isNotEmpty() && !value.proxyUri.startsWith("socks5://")) {
                info = "Proxy phải có dạng socks5://host:port"
            } else {
                val current = config.accounts.filterNot { it.id == value.id }
                persist(config.copy(accounts = current + value.copy(username = value.username.trim())))
                adding = false
            }
        })
    }
}

@Composable
private fun SectionTitle(title: String, sub: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WarningCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF403323))) {
        Text(text, Modifier.padding(15.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Accent)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun OverviewScreen(config: BotConfig) {
    SectionTitle("Tổng quan", "Chạy bot Java ME ngay trên Android, không dùng PC/VPS")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        DashboardMetric("Tài khoản", config.accounts.size.toString(), Modifier.weight(1f))
        DashboardMetric("Đã bật", config.accounts.count { it.enabled }.toString(), Modifier.weight(1f))
    }
    Text("Chọn tài khoản ở phần dưới và bấm Kiểm tra đăng nhập. " +
        "Sau khi nhận LoginSuccess, bạn có thể thử một chế độ Auto.",
        style = MaterialTheme.typography.bodySmall)
    WarningCard("Đây là bản kiểm thử. Không dùng tài khoản quan trọng trước khi xác minh server.")
}

@Composable
private fun SurfaceCard(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountsScreen(accounts: List<BotAccount>, editable: Boolean,
                           onAdd: () -> Unit, onImport: () -> Unit, onOptions: (BotAccount) -> Unit,
                           onEdit: (BotAccount) -> Unit,
                           onToggle: (BotAccount) -> Unit, onDelete: (BotAccount) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) { SectionTitle("Tài khoản", "Dữ liệu lưu bằng Android Keystore") }
        TextButton(onClick = onImport, enabled = editable) { Text("Nhập loạt") }
        Button(onClick = onAdd, enabled = editable) { Text("+ Thêm") }
    }
    if (accounts.isEmpty()) SurfaceCard("Chưa có tài khoản", "Chọn + Thêm để lưu tài khoản game")
    accounts.forEach { account ->
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(account.username, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text((if (account.label.isNotBlank()) "${account.label} • " else "") + GameCities.label(account.city),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = account.enabled, onCheckedChange = { onToggle(account) }, enabled = editable)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { onEdit(account) }, enabled = editable) { Text("Sửa") }
                    TextButton(onClick = { onOptions(account) }, enabled = editable) { Text("Cấu hình Auto") }
                    TextButton(onClick = { onDelete(account) }, enabled = editable) { Text("Xóa") }
                }
            }
        }
    }
}

@Composable
private fun AccountEditor(existing: BotAccount?, onDismiss: () -> Unit, onSave: (BotAccount) -> Unit) {
    var user by remember(existing) { mutableStateOf(existing?.username ?: "") }
    var password by remember(existing) { mutableStateOf(existing?.password ?: "") }
    var label by remember(existing) { mutableStateOf(existing?.label ?: "") }
    var accountProxy by remember(existing) { mutableStateOf(existing?.proxyUri ?: "") }
    var city by remember(existing) { mutableStateOf(existing?.city ?: "hoanmy") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Thêm tài khoản" else "Chỉnh sửa tài khoản") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(user, { user = it }, singleLine = true,
                    label = { Text("Tên đăng nhập game") })
                OutlinedTextField(password, { password = it }, singleLine = true,
                    label = { Text("Mật khẩu game") }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(label, { label = it }, singleLine = true,
                    label = { Text("Tên gợi nhớ (không bắt buộc)") })
                OutlinedTextField(accountProxy, { accountProxy = it }, singleLine = true,
                    label = { Text("Proxy SOCKS5 riêng (tuỳ chọn)") },
                    placeholder = { Text("socks5://host:1080") },
                    visualTransformation = PasswordVisualTransformation())
                Text("Thành phố", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = city == "hoanmy", onClick = { city = "hoanmy" }, label = { Text("Hoàn Mỹ") })
                    FilterChip(selected = city == "dieuky", onClick = { city = "dieuky" }, label = { Text("Diệu Kỳ") })
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            onSave(BotAccount(id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                username = user, password = password, city = city, enabled = existing?.enabled ?: true,
                label = label.take(128), options = existing?.options ?: AutoOptions(),
                proxyUri = accountProxy.trim()))
        }) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun ConnectionScreen(city: String, onCity: (String) -> Unit, proxyInput: String,
                             onProxyChange: (String) -> Unit, onSaveProxy: () -> Unit,
                             onProbe: () -> Unit, probeResult: String, probing: Boolean, canProbe: Boolean) {
    SectionTitle("Kiểm tra kết nối", "Hoạt động trực tiếp trên Android (Go Core hoặc Kotlin fallback)")
    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Server Avatar 2X", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(city == "hoanmy", onClick = { onCity("hoanmy") }, label = { Text("Hoàn Mỹ") })
                FilterChip(city == "dieuky", onClick = { onCity("dieuky") }, label = { Text("Diệu Kỳ") })
            }
            Text(GameCities.endpoint(city), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = proxyInput, onValueChange = onProxyChange, modifier = Modifier.fillMaxWidth(),
                label = { Text("SOCKS5 (không bắt buộc)") },
                placeholder = { Text("socks5://host:1080") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Text("Proxy có thể chứa mật khẩu; trường này được ẩn và lưu cùng dữ liệu mã hóa.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onSaveProxy) { Text("Lưu Proxy") }
                Button(onClick = onProbe, enabled = canProbe && !probing) {
                    Text(if (probing) "Đang thử..." else "Thử TCP")
                }
            }
            if (probeResult.isNotEmpty()) Text(probeResult, style = MaterialTheme.typography.bodySmall)
        }
    }
    WarningCard("TCP kết nối thành công không có nghĩa game đã đăng nhập hoặc Auto hoạt động. " +
        "Server IP được lấy từ HUONG-DAN-SU-DUNG.txt của bản PC và cần kiểm tra lại nếu server thay đổi.")
}

@Composable
private fun LogsScreen(logs: List<String>, onRefresh: () -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { SectionTitle("Nhật ký Java ME và kết nối") }
        OutlinedButton(onClick = onRefresh) { Text("Làm mới") }
    }
    OutlinedButton(enabled = logs.isNotEmpty(), onClick = {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val report = "AvatarBot Android v0.12 • nhật ký chẩn đoán (kiểm tra lại trước khi chia sẻ)\n" +
            "Trạng thái worker: ${LegacyBotRuntime.state()}\n" +
            "Trạng thái thử đăng nhập: ${LegacyBotRuntime.probeState()}\n" +
            logs.takeLast(100).joinToString("\n")
        clipboard?.setPrimaryClip(ClipData.newPlainText("AvatarBot diagnostics", report))
    }) { Text("Sao chép nhật ký đã che dữ liệu") }
    Text("Trước khi gửi cho người khác, hãy kiểm tra nội dung vừa sao chép. " +
        "Không gửi mật khẩu, token hoặc URL proxy có tài khoản.",
        style = MaterialTheme.typography.bodySmall)
    if (logs.isEmpty()) SurfaceCard("Chưa có nhật ký", "Nhấn Làm mới để đọc nhật ký Go và Java ME Core")
    else Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            logs.takeLast(100).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}


@Composable
private fun AutoModulesScreen(accounts: List<BotAccount>, editable: Boolean, onOptions: (BotAccount) -> Unit) {
    SectionTitle("Cấu hình các module Auto", "Chế độ chính chạy qua JAR; các tùy chọn nâng cao còn cần ánh xạ")
    WarningCard("JAR chạy một chế độ trên một tài khoản mỗi lần. Gieo hạt, lái buôn, nấu ăn, " +
        "nhiệm vụ, chu kỳ, câu cá và lựa chọn bán vật phẩm kim cương được ánh xạ. " +
        "Thu hoạch/Tưới/Vật nuôi/Thu khế chưa có công tắc độc lập; chưa kiểm thử server thật.")
    val modules = listOf(
        Triple("🌱 Nông trại", "farm", { o: AutoOptions -> o.farm }),
        Triple("🎣 Câu cá", "fishing", { o: AutoOptions -> o.fishing }),
        Triple("📚 Giáo viên", "teacher", { o: AutoOptions -> o.teacher }),
        Triple("💎 Kim cương", "diamond", { o: AutoOptions -> o.diamond })
    )
    modules.forEach { (name, _, enabled) ->
        SurfaceCard(name, "${accounts.count { it.enabled && enabled(it.options) }} tài khoản bật cấu hình")
    }
    SectionTitle("Theo tài khoản")
    if (accounts.isEmpty()) SurfaceCard("Chưa có tài khoản", "Thêm hoặc nhập hàng loạt trong tab Tài khoản")
    accounts.forEach { a ->
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(a.label.ifBlank { a.username }, Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { onOptions(a) }, enabled = editable) { Text("Thiết lập") }
            }
        }
    }
}

@Composable
private fun OptionSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun BulkImportDialog(accounts: List<BotAccount>, onDismiss: () -> Unit,
    onImport: (List<com.avatarbot.mobile.data.ImportedAccount>, String, String?) -> Unit) {
    var input by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("hoanmy") }
    var cloneId by remember { mutableStateOf<String?>(null) }
    var showTemplates by remember { mutableStateOf(false) }
    val parsed = remember(input) { AccountImporter.parse(input) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nhập tài khoản hàng loạt") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mỗi dòng: tài khoản mật_khẩu [tên gợi nhớ]. Cũng hỗ trợ dấu | , ; - như bản PC.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Thành phố", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(city == "hoanmy", onClick = { city = "hoanmy" }, label = { Text("Hoàn Mỹ") })
                    FilterChip(city == "dieuky", onClick = { city = "dieuky" }, label = { Text("Diệu Kỳ") })
                }
                Box {
                    OutlinedButton(onClick = { showTemplates = true }) {
                        Text("Clone cấu hình: " + (accounts.firstOrNull { it.id == cloneId }?.let {
                            it.label.ifBlank { it.username }
                        } ?: "Không"))
                    }
                    DropdownMenu(expanded = showTemplates, onDismissRequest = { showTemplates = false }) {
                        DropdownMenuItem(text = { Text("Không clone") }, onClick = {
                            cloneId = null; showTemplates = false
                        })
                        accounts.forEach { account ->
                            DropdownMenuItem(text = { Text(account.label.ifBlank { account.username }) }, onClick = {
                                cloneId = account.id; showTemplates = false
                            })
                        }
                    }
                }
                OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    minLines = 5, maxLines = 10, placeholder = { Text("acc1 pass1\nacc2|pass2|Acc phụ") })
                Text("Hợp lệ: ${parsed.records.size} • Lỗi: ${parsed.issues.size}",
                    style = MaterialTheme.typography.labelMedium)
                parsed.issues.take(4).forEach { issue ->
                    Text("Dòng ${issue.line}: ${issue.reason}", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (parsed.issues.size > 4) Text("Còn ${parsed.issues.size - 4} lỗi khác")
                Text("Chỉ clone cấu hình Auto, không sao chép mật khẩu/proxy. Tài khoản được mã hóa khi lưu.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = parsed.records.isNotEmpty() && parsed.issues.isEmpty(),
                onClick = { onImport(parsed.records, city, cloneId) }) { Text("Nhập ${parsed.records.size} tài khoản") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}
