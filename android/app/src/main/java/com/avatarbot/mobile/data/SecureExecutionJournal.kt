package com.avatarbot.mobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** No credential, server response body, proxy or message text is persisted here. */
enum class ExecutionStage(val label: String) {
    START_REQUESTED("Đã gửi yêu cầu chạy"),
    WORKER_STARTED("Đã tạo luồng bot (chưa xác minh game)"),
    START_FAILED("Không khởi chạy được"),
    WORKER_ERROR("Luồng bot dừng do lỗi"),
    WORKER_EXITED("Luồng bot đã kết thúc"),
    RETRY_SCHEDULED("Đã lên lịch thử lại"),
    WAIT_NETWORK("Chờ khôi phục kết nối mạng"),
    STOP_REQUESTED("Đã gửi yêu cầu dừng"),
    LIVE_LOGIN_CONFIRMED("Phiên Auto nhận LoginSuccess (chưa xác minh Auto)"),
    LOGIN_RECONFIRMED("Phiên game được xác nhận lại sau khi mất kết nối"),
    AUTH_STALLED("Đã ngắt bot: chờ LoginSuccess hoặc kết nối lại quá thời hạn"),
    LOGIN_ACCEPTED("Đã nhận phản hồi LoginSuccess"),
    LOGIN_NOT_CONFIRMED("Server không xác nhận đăng nhập"),
    LOGIN_ERROR("Lỗi kết nối hoặc giao thức khi thử đăng nhập")
}

data class ExecutionRecord(val accountId: String, val mode: Int,
    val stage: ExecutionStage, val timestamp: Long)

/** Encrypted bounded lifecycle history; no fabricated counts of farm/fish/diamond successes.
 * Backend and UI use the same encrypted private file but never store raw JAR logs.
 */
class SecureExecutionJournal(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("avatarbot_execution_v1", Context.MODE_PRIVATE)
    private val alias = "avatarbot_execution_key_v1"
    private val mutex = globalLock
    private val key: SecretKey
        get() {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existing = ks.getKey(alias, null) as? SecretKey
            if (existing != null) return existing
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .apply { init(KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build()) }.generateKey()
        }

    private fun readAll(): List<ExecutionRecord> {
        val encoded = prefs.getString("events", null) ?: return emptyList()
        val raw = Base64.decode(encoded, Base64.NO_WRAP)
        require(raw.size > 12) { "Invalid execution history" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, raw.copyOfRange(0, 12)))
        val array = JSONArray(cipher.doFinal(raw.copyOfRange(12, raw.size)).toString(Charsets.UTF_8))
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val stage = runCatching { ExecutionStage.valueOf(o.getString("stage")) }.getOrNull()
                ?: return@mapNotNull null
            ExecutionRecord(o.optString("account"), o.optInt("mode"), stage, o.optLong("when"))
        }
    }

    @Synchronized fun append(accountId: String, mode: Int, stage: ExecutionStage) {
        if (accountId.isBlank() || accountId.length > 80 || mode !in -1..3) return
        synchronized(mutex) {
            val records = (readAll() + ExecutionRecord(accountId, mode, stage,
                System.currentTimeMillis())).takeLast(150)
            val arr = JSONArray()
            records.forEach { r -> arr.put(JSONObject().apply {
                put("account",r.accountId); put("mode",r.mode)
                put("stage",r.stage.name); put("when",r.timestamp)
            }) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val ciphertext = cipher.iv + cipher.doFinal(arr.toString().toByteArray(Charsets.UTF_8))
            check(prefs.edit().putString("events",
                Base64.encodeToString(ciphertext, Base64.NO_WRAP)).commit())
        }
    }
    fun forAccount(id: String): List<ExecutionRecord> = synchronized(mutex) {
        readAll().filter { it.accountId == id }.takeLast(30).reversed()
    }
    companion object { private val globalLock = Any() }
}

/** Process-local live session indicator. Never infer authentication or Auto success from this. */
object ActiveAutoSession {
    @Volatile var accountId: String? = null
        private set
    @Volatile var mode: Int = -1
        private set
    fun set(id: String, selectedMode: Int) { accountId = id; mode = selectedMode }
    fun clear() { accountId = null; mode = -1 }
}
