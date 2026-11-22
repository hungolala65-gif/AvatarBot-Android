package com.avatarbot.mobile.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avatarbot.mobile.data.ActiveAutoSession
import com.avatarbot.mobile.data.AutoOptions
import com.avatarbot.mobile.data.BotAccount
import com.avatarbot.mobile.data.DiagnosticRedactor
import com.avatarbot.mobile.data.ExecutionRecord
import com.avatarbot.mobile.data.GameCities
import com.avatarbot.mobile.data.SecureExecutionJournal
import com.avatarbot.mobile.legacy.LegacyBotRuntime
import com.avatarbot.mobile.legacy.LegacyAdvancedSetup
import com.avatarbot.mobile.legacy.LegacyFarmCompatibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Four separate settings pages. Every save replaces only this account's options in the encrypted vault.
 * No unimplemented independent crop/animal switches are presented as working controls.
 */
@Composable
fun AutoSetupScreen(account: BotAccount, editable: Boolean,
    onBack: () -> Unit, onSave: (AutoOptions) -> Unit,
    onStart: (String, Int) -> Unit, onStop: () -> Unit, onTestLogin: (String) -> Unit) {
    val context = LocalContext.current
    var draft by remember(account.id) { mutableStateOf(account.options) }
    var mode by rememberSaveable(account.id) { mutableIntStateOf(0) }
    var showUnsaved by remember { mutableStateOf(false) }
    var resetRevision by remember { mutableIntStateOf(0) }
    val invalid = remember(account.id) { mutableStateMapOf<String, Boolean>() }
    var recent by remember(account.id) { mutableStateOf(emptyList<ExecutionRecord>()) }
    var currentState by remember { mutableStateOf("stopped") }
    var gameAuthState by remember { mutableStateOf("inactive") }
    var liveId by remember { mutableStateOf<String?>(null) }
    var liveMode by remember { mutableIntStateOf(-1) }
    var loginStatus by remember { mutableStateOf("idle") }
    var loginMessage by remember { mutableStateOf("") }
    var logLines by remember { mutableStateOf(emptyList<String>()) }
    val dirty = draft != account.options
    val modeNames = listOf("🌱 Nông trại", "🎣 Câu cá", "📚 Giáo viên", "💎 Kim cương")
    val workerModes = listOf(0, 1, 3, 2)
    val workerMode = workerModes[mode]
    val globalBusy = currentState in setOf("running", "starting", "retry_wait", "network_wait")
    val enabled = when (mode) { 0 -> draft.farm; 1 -> draft.fishing; 2 -> draft.teacher; else -> draft.diamond }
    val compatible = workerMode != 0 || LegacyFarmCompatibility.isCompatible(
        draft.harvest, draft.water, draft.animals, draft.starfruit)
    val validCity = runCatching { GameCities.endpoint(account.city) }.isSuccess
    val validFields = invalid.values.none { it } && validFarmSellIds(draft.farmSellProductIds, draft.farmAutoSell)

    LaunchedEffect(account.id) {
        val journal = SecureExecutionJournal(context)
        while (true) {
            recent = withContext(Dispatchers.IO) {
                runCatching { journal.forAccount(account.id) }.getOrDefault(emptyList())
            }
            liveId = ActiveAutoSession.accountId
            liveMode = ActiveAutoSession.mode
            currentState = LegacyBotRuntime.state()
            gameAuthState = LegacyBotRuntime.liveLoginState()
            val match = runCatching { LegacyBotRuntime.probeMatches(account.username, account.password,
                GameCities.endpoint(account.city)) }.getOrDefault(false)
            loginStatus = if (match) LegacyBotRuntime.probeState() else "idle"
            loginMessage = if (match) LegacyBotRuntime.probeMessage() else "Không có kết quả đăng nhập cho tài khoản này"
            logLines = if (liveId == account.id) LegacyBotRuntime.logs().takeLast(8).map {
                DiagnosticRedactor.redact(it, listOf(account.username,account.password,account.proxyUri))
            } else emptyList()
            delay(1500L)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={ if (dirty) showUnsaved = true else onBack() }) { Text("← Trở lại Auto") }
            Spacer(Modifier.weight(1f))
            Text("Setup v0.18", style=MaterialTheme.typography.labelMedium)
        }
        Text("Thiết lập riêng: ${account.label.ifBlank { account.username }}",
            style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold)
        Text("Máy chủ ${GameCities.label(account.city)} • Mỗi lần chỉ chạy 1 tài khoản. " +
            "Lưu xong cần dừng và chạy lại để áp dụng thiết lập mới.",
            style=MaterialTheme.typography.bodySmall)
        Row(modifier=Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            modeNames.forEachIndexed { idx, label ->
                FilterChip(selected=mode==idx, onClick={mode=idx},label={Text(label)})
            }
        }
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(modeNames[mode], style=MaterialTheme.typography.titleLarge)
                key(resetRevision) {
                    when (mode) {
                        0 -> FarmSetup(draft, onChange={ draft=it },validity={ field,bad ->invalid[field]=bad })
                        1 -> FishSetup(draft, onChange={ draft=it },validity={ field,bad ->invalid[field]=bad })
                        2 -> TeacherSetup(draft, onChange={ draft=it },validity={ field,bad ->invalid[field]=bad })
                        3 -> DiamondSetup(draft, onChange={ draft=it },validity={ field,bad ->invalid[field]=bad })
                    }
                }
                HorizontalDivider()
                Text("Thiết lập treo chung của tài khoản", fontWeight=FontWeight.SemiBold)
                Text("Chu kỳ tác vụ, không phải số lần đăng nhập thành công.",
                    style=MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf(15,30,60,120,300).forEach { n ->
                        FilterChip(selected=draft.loopDelaySeconds==n,
                            onClick={draft=draft.copy(loopDelaySeconds=n)},label={Text("${n}s")})
                    }
                }
                AutoToggle("Giữ CPU khi màn hình tắt (hao pin)",draft.keepCpuAwake) {
                    draft=draft.copy(keepCpuAwake=it)
                }
                AutoToggle("Thử lại nếu worker thoát (tối đa 3 lần)",draft.restartOnUnexpectedExit) {
                    draft=draft.copy(restartOnUnexpectedExit=it)
                }
            }
        }
        if (dirty) Text("Có thay đổi chưa lưu. Hãy lưu trước khi chạy bot.",
            color=MaterialTheme.colorScheme.tertiary,style=MaterialTheme.typography.bodySmall)
        if (!validFields) Text("Có giá trị nhập chưa hợp lệ, cần sửa trước khi lưu.",
            color=MaterialTheme.colorScheme.error, style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={onSave(draft)},enabled=editable && dirty && validFields) { Text("Lưu Setup") }
            OutlinedButton(onClick={draft=account.options; invalid.clear(); resetRevision++},enabled=dirty || !validFields) {
                Text("Hoàn tác")
            }
        }
        Card {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Điều khiển và kiểm tra",style=MaterialTheme.typography.titleMedium)
                Text("Trạng thái luồng: " + (if (liveId == account.id) currentState else "Không có phiên đang chạy"),
                    style=MaterialTheme.typography.bodyMedium)
                if (liveId == account.id && currentState == "running") Text(
                    "Phiên game hiện tại: " + when(gameAuthState) {
                        "confirmed" -> "Đã nhận LoginSuccess; phiên còn mở"
                        "awaiting" -> "Đang chờ LoginSuccess"
                        "disconnected" -> "Đã mất phiên game"
                        else -> "Không xác định"
                    }, style=MaterialTheme.typography.labelLarge)
                Text("Không suy ra đã đăng nhập thành công hoặc hoàn thành Auto chỉ từ trạng thái worker.",
                    style=MaterialTheme.typography.bodySmall)
                Text("Đăng nhập: " + when(loginStatus) {
                    "success" -> "Đã nhận LoginSuccess từ server"
                    "testing" -> "Đang kiểm tra"
                    "rejected" -> "Không nhận LoginSuccess"
                    "error" -> "Lỗi kết nối / giao thức"
                    else -> "Chưa có kết quả cho tài khoản này"
                },style=MaterialTheme.typography.bodyMedium)
                Text(loginMessage,style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={onTestLogin(account.id)},enabled=editable && validCity && !globalBusy) {
                        Text("Thử đăng nhập")
                    }
                    Button(onClick={onStart(account.id,workerMode)},
                        enabled=editable && validCity && !globalBusy && enabled && !dirty && validFields && compatible) {
                        Text("Chạy ${modeNames[mode].substringAfter(' ')}")
                    }
                }
                OutlinedButton(onClick=onStop, enabled=globalBusy && liveId == account.id) {
                    Text("Dừng phiên đang chạy")
                }
                if (!compatible) Text("Farm có cấu hình cũ không tương thích; chọn Phục hồi mặc định JAR.",
                    color=MaterialTheme.colorScheme.error)
            }
        }
        Card {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text("Lịch sử thực thi của tài khoản",style=MaterialTheme.typography.titleMedium)
                Text("Chỉ ghi nhận sự kiện ứng dụng quan sát được. Không có số liệu hoàn thành thu hoạch, " +
                    "câu cá, giáo viên hay kim cương khi server chưa xác thực.",
                    style=MaterialTheme.typography.bodySmall)
                val filtered = recent.filter { it.mode == workerMode || it.mode == -1 }.take(12)
                if (filtered.isEmpty()) Text("Chưa có sự kiện được lưu cho chế độ này")
                filtered.forEach { event ->
                    val whenStr = remember(event.timestamp) {
                        SimpleDateFormat("dd/MM HH:mm:ss",Locale.getDefault()).format(Date(event.timestamp))
                    }
                    Text("$whenStr · ${event.stage.label}",style=MaterialTheme.typography.bodySmall)
                }
                if (logLines.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Nhật ký JAR đang chạy (đã che thông tin nhạy cảm)",
                        style=MaterialTheme.typography.labelLarge)
                    logLines.forEach { Text(it,style=MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
    if (showUnsaved) AlertDialog(onDismissRequest={showUnsaved=false},
        title={Text("Thiết lập chưa lưu")},
        text={Text("Thoát sẽ bỏ thay đổi chưa lưu của tài khoản này.")},
        confirmButton={TextButton(onClick={showUnsaved=false;onBack()}){Text("Bỏ thay đổi")}},
        dismissButton={TextButton(onClick={showUnsaved=false}){Text("Ở lại")}})
}

/** Prevent saving a sale configuration which fails Java's exact whitelist parser. */
private fun validFarmSellIds(ids: String, enabled: Boolean): Boolean = try {
    val codes = LegacyAdvancedSetup.parseSellWhitelist(ids)
    !enabled || codes.isNotEmpty()
} catch (_: IllegalArgumentException) { false }

@Composable
private fun AutoToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
        Switch(checked=value,onCheckedChange=onChange)
    }
}

@Composable
private fun AutoNumber(key: String, title: String, value: Int, min: Int, max: Int,
    onChange: (Int) -> Unit, validity:(String,Boolean)->Unit) {
    var text by rememberSaveable(key) { mutableStateOf(value.toString()) }
    val valid = text.toIntOrNull()?.let {it in min..max} ?: false
    OutlinedTextField(value=text,onValueChange={ changed ->
        if(changed.length <= 6 && changed.all {it.isDigit() || it=='-'}) {
            text=changed
            val n=changed.toIntOrNull()
            val ok=n!=null && n in min..max
            validity(key,!ok)
            if(ok) onChange(n!!)
        }
    },modifier=Modifier.fillMaxWidth(),singleLine=true,isError=!valid,
        label={Text(title)}, supportingText={Text("Hợp lệ: $min đến $max; mã vật phẩm cần đối chiếu với game")})
}

@Composable
private fun FarmSetup(o:AutoOptions,onChange:(AutoOptions)->Unit,validity:(String,Boolean)->Unit) {
    AutoToggle("Bật Auto Farm",o.farm){onChange(o.copy(farm=it))}
    Text("JAR không cho tắt độc lập thu hoạch, tưới, chăm vật nuôi hoặc bật Thu khế như bản PC.",
        style=MaterialTheme.typography.bodySmall)
    if (!LegacyFarmCompatibility.isCompatible(o.harvest,o.water,o.animals,o.starfruit)) {
        TextButton(onClick={onChange(o.copy(harvest=true,water=true,animals=true,starfruit=false))}) {
            Text("Phục hồi cấu hình Farm tương thích JAR")
        }
    }
    AutoToggle("Gieo hạt sau thu hoạch",o.plant){onChange(o.copy(plant=it))}
    if (o.plant) {
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            FilterChip(selected=o.seedMode=="preferred",onClick={onChange(o.copy(seedMode="preferred"))},label={Text("Giống ưu tiên")})
            FilterChip(selected=o.seedMode=="harvest_replay",onClick={onChange(o.copy(seedMode="harvest_replay"))},label={Text("Trồng lại giống cũ")})
        }
    }
    AutoNumber("farmSeed","Mã giống ưu tiên (-1 = JAR tự chọn)",o.preferredSeedId,-1,65535,
        {onChange(o.copy(preferredSeedId=it))},validity)
    AutoNumber("farmLand","Số ô đất mục tiêu (0 = mặc định)",o.targetLandCount,0,100,
        {onChange(o.copy(targetLandCount=it))},validity)
    AutoToggle("Bán vật nuôi già (có thể mất tài sản)",o.animalSellLate){onChange(o.copy(animalSellLate=it))}
    AutoToggle("Mua bù vật nuôi đã bán (chi Xu)",o.farmRebuySold) {onChange(o.copy(farmRebuySold=it))}
    if(o.farmRebuySold) Text("Chỉ mua bù theo mã vật nuôi đã bán. Không cho phép dùng Lượng.",
        style=MaterialTheme.typography.bodySmall)
    HorizontalDivider()
    Text("Bán sản phẩm trong kho — mặc định tắt",fontWeight=FontWeight.SemiBold)
    AutoToggle("Cho phép bán danh sách sản phẩm",o.farmAutoSell){onChange(o.copy(farmAutoSell=it))}
    var saleListText by rememberSaveable(o.farmSellProductIds) { mutableStateOf(o.farmSellProductIds) }
    val saleValid = validFarmSellIds(saleListText, o.farmAutoSell)
    OutlinedTextField(value=saleListText,onValueChange={ text ->
        if(text.length<=110) {
            saleListText=text
            validity("farmSellIds", !validFarmSellIds(text,o.farmAutoSell))
            onChange(o.copy(farmSellProductIds=text))
        }
    }, modifier=Modifier.fillMaxWidth(), singleLine=true, isError=!saleValid,
       label={Text("Mã sản phẩm được phép bán, cách nhau bởi dấu phẩy")},
       supportingText={Text("Chỉ mã 1..65535, tối đa 16 mã. Không cho phép -1 hoặc trùng mã.")})
    AutoNumber("farmSellThreshold","Chỉ bán nếu tồn kho từ số lượng",o.farmSellThreshold,1,99999,
        {onChange(o.copy(farmSellThreshold=it))},validity)
    HorizontalDivider()
    AutoToggle("Tự nâng cấp cây khế (có thể tốn Xu)",o.farmStarUpgrade) {onChange(o.copy(farmStarUpgrade=it))}
    if(o.farmStarUpgrade) AutoNumber("farmStarTarget","Cấp cây khế mục tiêu",o.farmStarTargetLevel,1,100,
        {onChange(o.copy(farmStarTargetLevel=it))},validity)
    AutoToggle("Hỗ trợ lái buôn",o.merchant){onChange(o.copy(merchant=it))}
    AutoToggle("Nấu ăn",o.cooking){onChange(o.copy(cooking=it))}
    HorizontalDivider()
    Text("Nhiệm vụ, điểm danh trong chu kỳ Farm",fontWeight=FontWeight.SemiBold)
    AutoToggle("Nhiệm vụ hàng ngày",o.dailyQuest){onChange(o.copy(dailyQuest=it))}
    AutoToggle("Hugo",o.hugoQuest){onChange(o.copy(hugoQuest=it))}
    AutoToggle("Sự kiện",o.eventQuest){onChange(o.copy(eventQuest=it))}
    AutoToggle("Nhiệm vụ nông trại",o.farmMission){onChange(o.copy(farmMission=it))}
    AutoToggle("Điểm danh nông trại",o.farmCheckin){onChange(o.copy(farmCheckin=it))}
    Text("Mặc định KHÔNG tự mua bù, không bán kho, không nâng khế, không dùng Lượng. " +
        "Mọi giao dịch chủ động bật cần kiểm thử trước trên tài khoản phụ.",
        style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun FishSetup(o:AutoOptions,onChange:(AutoOptions)->Unit,validity:(String,Boolean)->Unit) {
    AutoToggle("Bật Auto Fishing",o.fishing){onChange(o.copy(fishing=it))}
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        listOf(0,5,10,20,50,100).forEach { n ->
            FilterChip(selected=o.fishingRounds==n,onClick={onChange(o.copy(fishingRounds=n))},
                label={Text(if(n==0)"Không giới hạn" else "$n lượt")})
        }
    }
    AutoNumber("fishMap","Mã bản đồ câu (JAR mặc định 14)",o.fishingMap,0,999,
        {onChange(o.copy(fishingMap=it))},validity)
    AutoNumber("fishArea","Khu vực câu (-1 = tự chọn)",o.fishingArea,-1,99,
        {onChange(o.copy(fishingArea=it))},validity)
    AutoNumber("fishBait","Mã mồi (JAR mặc định 448)",o.fishingBaitId,0,65535,
        {onChange(o.copy(fishingBaitId=it))},validity)
    AutoNumber("fishRod","Mã cần (JAR mặc định 442)",o.fishingRodId,0,65535,
        {onChange(o.copy(fishingRodId=it))},validity)
    Text("Mã bản đồ/mồi/cần được lấy từ cấu hình JAR, chưa xác minh với máy chủ hiện tại. " +
        "Chế độ câu cá không tự chuyển sang Farm.",style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun TeacherSetup(o:AutoOptions,onChange:(AutoOptions)->Unit,validity:(String,Boolean)->Unit) {
    AutoToggle("Bật Auto Teacher",o.teacher){onChange(o.copy(teacher=it))}
    AutoNumber("teacherArea","Khu vực giáo viên (-1 = tự chọn)",o.teacherArea,-1,99,
        {onChange(o.copy(teacherArea=it))},validity)
    Text("Dữ liệu học câu hỏi được lưu trong RMS mã hóa. Dịch vụ giải CAPTCHA bên ngoài " +
        "bị chặn mặc định; phiên có CAPTCHA có thể không hoàn tất.",
        style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun DiamondSetup(o:AutoOptions,onChange:(AutoOptions)->Unit,validity:(String,Boolean)->Unit) {
    AutoToggle("Bật Auto Diamond",o.diamond){onChange(o.copy(diamond=it))}
    Text("Cảnh báo: hai thao tác bán có thể làm mất vật phẩm. Mặc định đều TẮT.",
        color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    AutoToggle("Cho phép bán KCX",o.diamondSellKCX){onChange(o.copy(diamondSellKCX=it))}
    AutoToggle("Cho phép bán NHB",o.diamondSellNHB){onChange(o.copy(diamondSellNHB=it))}
    AutoNumber("diamondDuration","Thời gian bán (phút)",o.diamondSellDurationMin,1,30,
        {onChange(o.copy(diamondSellDurationMin=it))},validity)
    AutoNumber("diamondInterval","Khoảng cách bán (mili giây)",o.diamondSellIntervalMs,200,5000,
        {onChange(o.copy(diamondSellIntervalMs=it))},validity)
    Text("Ứng dụng không tự chuyển sang Farm. Chỉ dùng tài khoản thử nghiệm cho đến khi xác minh hành vi bán.",
        style=MaterialTheme.typography.bodySmall)
}
