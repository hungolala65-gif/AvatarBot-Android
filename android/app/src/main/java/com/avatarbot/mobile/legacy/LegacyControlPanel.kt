package com.avatarbot.mobile.legacy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avatarbot.mobile.data.BotAccount
import com.avatarbot.mobile.data.BotConfig
import com.avatarbot.mobile.data.GameCities
import com.avatarbot.mobile.data.ActiveAutoSession
import kotlinx.coroutines.delay

/** Explicit opt-in: all user credentials remain in encrypted local storage. */
@Composable
fun LegacyControlPanel(config: BotConfig, onStart: (String, Int) -> Unit, onStop: () -> Unit,
    onTestLogin: (String) -> Unit) {
    val accounts = config.accounts.filter { it.enabled }
    var chosen by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var runState by remember { mutableStateOf(LegacyBotRuntime.state()) }
    var liveAuth by remember { mutableStateOf(LegacyBotRuntime.liveLoginState()) }
    var observedLogins by remember { mutableLongStateOf(0L) }
    var uptimeSeconds by remember { mutableLongStateOf(0L) }
    var recentLogs by remember { mutableStateOf(emptyList<String>()) }
    var probeState by remember { mutableStateOf(LegacyBotRuntime.probeState()) }
    var probeMessage by remember { mutableStateOf(LegacyBotRuntime.probeMessage()) }
    LaunchedEffect(Unit) {
        while (true) {
            runState = LegacyBotRuntime.state()
            liveAuth = LegacyBotRuntime.liveLoginState()
            observedLogins = LegacyBotRuntime.observedLoginCycles()
            uptimeSeconds = LegacyBotRuntime.uptimeSeconds()
            recentLogs = LegacyBotRuntime.logs().takeLast(4)
            probeState = LegacyBotRuntime.probeState()
            probeMessage = LegacyBotRuntime.probeMessage()
            delay(1200L)
        }
    }
    val active = runState == "starting" || runState == "running" ||
        runState == "retry_wait" || runState == "network_wait"
    val account = accounts.firstOrNull { it.id == chosen } ?: accounts.firstOrNull()
    val validCity = account?.let { runCatching { GameCities.endpoint(it.city) }.isSuccess } == true
    val probeBelongsToSelection = account?.let {
        runCatching {
            LegacyBotRuntime.probeMatches(it.username, it.password, GameCities.endpoint(it.city))
        }.getOrDefault(false)
    } == true
    val selectedProbeState = if (probeBelongsToSelection) probeState else "idle"
    val selectedProbeMessage = if (probeBelongsToSelection) probeMessage
        else "Chưa kiểm tra đăng nhập cho tài khoản và máy chủ đang chọn"
    val names = listOf("Nông trại", "Câu cá", "Kim cương", "Giáo viên")
    val farmOptionsSupported = account?.options?.let {
        LegacyFarmCompatibility.isCompatible(it.harvest, it.water, it.animals, it.starfruit)
    } ?: false
    val allowed = account != null && validCity && (mode != 0 || farmOptionsSupported) && when (mode) {
        0 -> account.options.farm
        1 -> account.options.fishing
        2 -> account.options.diamond
        3 -> account.options.teacher
        else -> false
    }
    Card {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Chạy trực tiếp bằng Java ME Core",style=MaterialTheme.typography.titleMedium)
            Text("Thử nghiệm tích hợp JAR thực tế • 1 tài khoản/lần. " +
                "Không cần PC/VPS; dịch vụ giải CAPTCHA bên ngoài bị chặn.",
                style=MaterialTheme.typography.bodySmall)
            val stateLabel = when(runState) {
                "starting" -> "Đang chuẩn bị kết nối"
                "retry_wait" -> "Worker đã thoát, đang chờ thử lại có giới hạn"
                "network_wait" -> "Mất mạng; đang chờ mạng phục hồi (tối đa 30 phút)"
                "running" -> "Tiến trình JAR đang chạy (đăng nhập chưa xác minh)"
                "failed" -> "Không thể khởi chạy"
                else -> "Worker đã dừng"
            }
            if (runState == "running" && ActiveAutoSession.accountId == account?.id) {
                Text("Phiên game: " + when (liveAuth) {
                    "confirmed" -> "Đã nhận LoginSuccess (phiên đang mở)"
                    "awaiting" -> "Chưa nhận LoginSuccess"
                    "disconnected" -> "Kết nối game đã đóng"
                    else -> "Không đọc được trạng thái phiên"
                }, style = MaterialTheme.typography.labelLarge)
            }
            Text("Trạng thái: $stateLabel",
                style=MaterialTheme.typography.labelLarge)
            if (runState == "running" && ActiveAutoSession.accountId == account?.id) {
                Text("Đã quan sát LoginSuccess ở ít nhất $observedLogins chu kỳ JAR; " +
                    "đây không phải số thao tác Auto hoàn thành.",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (runState == "running") Text(
                "Giám sát xác thực: nếu không có LoginSuccess trong 3 phút " +
                "(hoặc không xác nhận lại trên 10 phút sau lần đăng nhập), bot tự ngắt để tránh treo giả.",
                style=MaterialTheme.typography.bodySmall)
            if (runState == "running") Text("Đã treo: ${uptimeSeconds / 3600} giờ " +
                "${(uptimeSeconds / 60) % 60} phút ${uptimeSeconds % 60} giây",
                style=MaterialTheme.typography.bodySmall)
            if (account != null) Text("Chu kỳ Auto: ${account.options.loopDelaySeconds} giây (tùy chọn khi lưu cấu hình)",
                style=MaterialTheme.typography.bodySmall)
            if (account?.options?.keepCpuAwake == true)
                Text("Giữ CPU khi tắt màn hình: BẬT (hao pin hơn)",style=MaterialTheme.typography.bodySmall)
            if (account?.options?.restartOnUnexpectedExit == true)
                Text("Thử lại nếu worker tự thoát: tối đa 3 lần, cách 30/60/120 giây",
                    style=MaterialTheme.typography.bodySmall)
            val lastError=LegacyBotRuntime.error()
            if (lastError.isNotBlank()) Text("Lỗi khởi chạy: $lastError",
                color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
            Box {
                OutlinedButton(onClick={menu=true},enabled=accounts.isNotEmpty()) {
                    Text(account?.let { if (it.label.isBlank()) it.username else it.label } ?: "Chưa có tài khoản")
                }
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    accounts.forEach { a ->
                        DropdownMenuItem(text={Text(if(a.label.isBlank()) a.username else a.label)},onClick={chosen=a.id;menu=false})
                    }
                }
            }
            if (account != null && !validCity)
                Text("Tài khoản này có mã thành phố không hợp lệ. Hãy sửa tài khoản trước khi kết nối.",
                    color=MaterialTheme.colorScheme.error, style=MaterialTheme.typography.bodySmall)
            Text("Kiểm tra đăng nhập độc lập",style=MaterialTheme.typography.labelLarge)
            Text("Nút này chỉ gửi handshake và Login qua JAR; không kích hoạt Auto.",
                style=MaterialTheme.typography.bodySmall)
            Button(onClick={account?.let { onTestLogin(it.id) }},
                enabled=account != null && validCity && !active) {
                Text(if (selectedProbeState=="testing") "Đang thử đăng nhập..." else "Kiểm tra đăng nhập")
            }
            Text("Kết quả: " + when(selectedProbeState) {
                "testing" -> "Đang chờ server (tối đa khoảng 20 giây)"
                "success" -> "Đã nhận LoginSuccess"
                "rejected" -> "Không xác thực được"
                "error" -> "Lỗi kết nối hoặc giao thức"
                "cancelled" -> "Đã hủy"
                else -> "Chưa kiểm tra"
            }, style=MaterialTheme.typography.labelLarge)
            Text(selectedProbeMessage, style=MaterialTheme.typography.bodySmall)
            Text("Chế độ Auto",style=MaterialTheme.typography.labelLarge)
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                names.forEachIndexed { idx, name ->
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        RadioButton(selected=mode==idx,onClick={mode=idx})
                        Text(name,Modifier.padding(top=10.dp))
                    }
                }
            }
            if (!allowed && !active) Text("Hãy bật mode tương ứng. Nếu Farm bị chặn do cấu hình cũ, vào Thiết lập để Đặt về mặc định JAR. Gieo hạt có thể bật/tắt bằng seedMode.",
                style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Button(onClick={account?.let { onStart(it.id,mode) }},enabled=allowed && !active) { Text("Chạy JAR") }
                OutlinedButton(onClick=onStop,enabled=active) { Text("Dừng") }
            }
            if (recentLogs.isNotEmpty()) {
                Text("Nhật ký gần đây", style = MaterialTheme.typography.labelLarge)
                recentLogs.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Câu cá và Kim cương sẽ không tự chuyển sang Farm sau 30 phút. " +
                "Lưu ý: TCP/SOCKS5 và handshake đã thử với server giả lập. " +
                "Chưa xác nhận đăng nhập/Auto với server thật; không bảo đảm tránh khóa tài khoản.",
                style=MaterialTheme.typography.bodySmall)
        }
    }
}
