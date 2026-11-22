package com.avatarbot.mobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AutoOptions(
    val farm: Boolean = true,
    val fishing: Boolean = false,
    val teacher: Boolean = false,
    val diamond: Boolean = false,
    val harvest: Boolean = true,
    val plant: Boolean = true,
    val seedMode: String = "preferred",
    val water: Boolean = true,
    val animals: Boolean = true,
    val merchant: Boolean = false,
    val cooking: Boolean = false,
    val starfruit: Boolean = false,
    val dailyQuest: Boolean = false,
    val hugoQuest: Boolean = false,
    val eventQuest: Boolean = false,
    val farmMission: Boolean = false,
    val farmCheckin: Boolean = false,
    val keepCpuAwake: Boolean = false,
    val restartOnUnexpectedExit: Boolean = false,
    val loopDelaySeconds: Int = 15,
    /** 0 means unlimited in Bot.runFishing (checked against JAR bytecode). */
    val fishingRounds: Int = 0,
    /** -1 = automatic area. Bot.runTeacher constrains 0..99. */
    val teacherArea: Int = -1,
    /** Explicit opt-in: original JAR defaults both sell flags to true. */
    val diamondSellKCX: Boolean = false,
    val diamondSellNHB: Boolean = false,
    /** JAR default 'late' sells animals: opt in explicitly. Empty string disables both sell paths. */
    val animalSellLate: Boolean = false,
    /** PC-parity additions; operations affecting inventory/currency require opt-in. */
    val farmAutoSell: Boolean = false,
    val farmSellProductIds: String = "",
    val farmSellThreshold: Int = 50,
    val farmRebuySold: Boolean = false,
    val farmStarUpgrade: Boolean = false,
    val farmStarTargetLevel: Int = 50,
    /** Identifiers use JAR defaults; exact game catalog IDs must be checked in a test account. */
    val preferredSeedId: Int = -1,
    val targetLandCount: Int = 0,
    val fishingMap: Int = 14,
    val fishingArea: Int = -1,
    val fishingBaitId: Int = 448,
    val fishingRodId: Int = 442,
    val diamondSellDurationMin: Int = 3,
    val diamondSellIntervalMs: Int = 200
) {
    fun toJson() = JSONObject().apply {
        put("farm", farm); put("fishing", fishing); put("teacher", teacher)
        put("diamond", diamond); put("harvest", harvest); put("plant", plant)
        put("seedMode", if (seedMode in listOf("preferred", "harvest_replay")) seedMode else "preferred")
        put("water", water); put("animals", animals); put("merchant", merchant)
        put("cooking", cooking); put("starfruit", starfruit)
        put("dailyQuest", dailyQuest); put("hugoQuest", hugoQuest)
        put("eventQuest", eventQuest); put("farmMission", farmMission)
        put("farmCheckin", farmCheckin)
        put("keepCpuAwake", keepCpuAwake); put("restartOnUnexpectedExit", restartOnUnexpectedExit)
        put("loopDelaySeconds", loopDelaySeconds.coerceIn(15, 300))
        put("fishingRounds", fishingRounds.coerceIn(0, 100))
        put("teacherArea", teacherArea.coerceIn(-1, 99))
        put("diamondSellKCX", diamondSellKCX); put("diamondSellNHB", diamondSellNHB)
        put("animalSellLate", animalSellLate)
        put("farmAutoSell", farmAutoSell)
        put("farmSellProductIds", farmSellProductIds)
        put("farmSellThreshold", farmSellThreshold.coerceIn(1, 99999))
        put("farmRebuySold", farmRebuySold)
        put("farmStarUpgrade", farmStarUpgrade)
        put("farmStarTargetLevel", farmStarTargetLevel.coerceIn(1, 100))
        put("preferredSeedId", preferredSeedId.coerceIn(-1, 65535))
        put("targetLandCount", targetLandCount.coerceIn(0, 100))
        put("fishingMap", fishingMap.coerceIn(0, 999))
        put("fishingArea", fishingArea.coerceIn(-1, 99))
        put("fishingBaitId", fishingBaitId.coerceIn(0, 65535))
        put("fishingRodId", fishingRodId.coerceIn(0, 65535))
        put("diamondSellDurationMin", diamondSellDurationMin.coerceIn(1, 30))
        put("diamondSellIntervalMs", diamondSellIntervalMs.coerceIn(200, 5000))
    }
    /** These five flags map directly to named fields in the user-provided JAR Config. */
    fun javaMEFeatureMask(): Int =
        (if (dailyQuest) 1 else 0) or
        (if (hugoQuest) 2 else 0) or
        (if (eventQuest) 4 else 0) or
        (if (farmMission) 8 else 0) or
        (if (farmCheckin) 16 else 0)
    companion object {
        fun fromJson(o: JSONObject?) = AutoOptions(
            farm = o?.optBoolean("farm", true) ?: true,
            fishing = o?.optBoolean("fishing", false) ?: false,
            teacher = o?.optBoolean("teacher", false) ?: false,
            diamond = o?.optBoolean("diamond", false) ?: false,
            harvest = o?.optBoolean("harvest", true) ?: true,
            plant = o?.optBoolean("plant", true) ?: true,
            seedMode = o?.optString("seedMode", "preferred")?.takeIf {
                it == "preferred" || it == "harvest_replay"
            } ?: "preferred",
            water = o?.optBoolean("water", true) ?: true,
            animals = o?.optBoolean("animals", true) ?: true,
            merchant = o?.optBoolean("merchant", false) ?: false,
            cooking = o?.optBoolean("cooking", false) ?: false,
            starfruit = o?.optBoolean("starfruit", false) ?: false,
            dailyQuest = o?.optBoolean("dailyQuest", false) ?: false,
            hugoQuest = o?.optBoolean("hugoQuest", false) ?: false,
            eventQuest = o?.optBoolean("eventQuest", false) ?: false,
            farmMission = o?.optBoolean("farmMission", false) ?: false,
            farmCheckin = o?.optBoolean("farmCheckin", false) ?: false,
            keepCpuAwake = o?.optBoolean("keepCpuAwake", false) ?: false,
            restartOnUnexpectedExit = o?.optBoolean("restartOnUnexpectedExit", false) ?: false,
            loopDelaySeconds = (o?.optInt("loopDelaySeconds", 15) ?: 15).coerceIn(15, 300),
            fishingRounds = (o?.optInt("fishingRounds", 0) ?: 0).coerceIn(0, 100),
            teacherArea = (o?.optInt("teacherArea", -1) ?: -1).coerceIn(-1, 99),
            diamondSellKCX = o?.optBoolean("diamondSellKCX", false) ?: false,
            diamondSellNHB = o?.optBoolean("diamondSellNHB", false) ?: false,
            animalSellLate = o?.optBoolean("animalSellLate", false) ?: false,
            farmAutoSell = o?.optBoolean("farmAutoSell", false) ?: false,
            farmSellProductIds = o?.optString("farmSellProductIds", "") ?: "",
            farmSellThreshold = (o?.optInt("farmSellThreshold", 50) ?: 50).coerceIn(1, 99999),
            farmRebuySold = o?.optBoolean("farmRebuySold", false) ?: false,
            farmStarUpgrade = o?.optBoolean("farmStarUpgrade", false) ?: false,
            farmStarTargetLevel = (o?.optInt("farmStarTargetLevel", 50) ?: 50).coerceIn(1, 100),
            preferredSeedId = (o?.optInt("preferredSeedId", -1) ?: -1).coerceIn(-1, 65535),
            targetLandCount = (o?.optInt("targetLandCount", 0) ?: 0).coerceIn(0, 100),
            fishingMap = (o?.optInt("fishingMap", 14) ?: 14).coerceIn(0, 999),
            fishingArea = (o?.optInt("fishingArea", -1) ?: -1).coerceIn(-1, 99),
            fishingBaitId = (o?.optInt("fishingBaitId", 448) ?: 448).coerceIn(0, 65535),
            fishingRodId = (o?.optInt("fishingRodId", 442) ?: 442).coerceIn(0, 65535),
            diamondSellDurationMin = (o?.optInt("diamondSellDurationMin", 3) ?: 3).coerceIn(1, 30),
            diamondSellIntervalMs = (o?.optInt("diamondSellIntervalMs", 200) ?: 200).coerceIn(200, 5000)
        )
    }
}

data class BotAccount(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val password: String,
    val city: String = "hoanmy",
    val enabled: Boolean = true,
    val label: String = "",
    val options: AutoOptions = AutoOptions(),
    val proxyUri: String = ""
)

data class BotConfig(val accounts: List<BotAccount> = emptyList(), val proxyUri: String = "")

/** App-private AES-GCM storage protected by Android Keystore.
 *  A reinstall or KeyStore reset may make old encrypted data unrecoverable.
 *  Never log accounts or plaintext passwords. This is NOT the PC vault.dat format.
 */
class SecureConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("avatarbot_secure_v1", Context.MODE_PRIVATE)
    private val alias = "avatarbot_accounts_key_v1"
    private val key: SecretKey
        get() {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val current = store.getKey(alias, null) as? SecretKey
            if (current != null) return current
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(
                    alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build())
            }.generateKey()
        }

    fun load(): BotConfig {
        val encoded = prefs.getString("profile", null) ?: return BotConfig()
        val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
        require(encrypted.size > 12) { "Dữ liệu mã hóa bị hỏng" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, encrypted.copyOfRange(0, 12)))
        val raw = cipher.doFinal(encrypted.copyOfRange(12, encrypted.size))
        val obj = JSONObject(raw.toString(Charsets.UTF_8))
        val array = obj.optJSONArray("accounts") ?: JSONArray()
        val accounts = buildList {
            for (i in 0 until array.length()) {
                val a = array.getJSONObject(i)
                add(BotAccount(
                    id = a.getString("id"), username = a.getString("username"),
                    password = a.getString("password"), city = a.getString("city"),
                    enabled = a.optBoolean("enabled", true),
                    label = a.optString("label", ""),
                    options = AutoOptions.fromJson(a.optJSONObject("options")),
                    proxyUri = a.optString("accountProxyUri", "")
                ))
            }
        }
        return BotConfig(accounts, obj.optString("proxyUri", ""))
    }

    fun save(config: BotConfig) {
        val accounts = JSONArray()
        config.accounts.forEach { a ->
            accounts.put(JSONObject().apply {
                put("id", a.id); put("username", a.username); put("password", a.password)
                put("city", a.city); put("enabled", a.enabled)
                put("label", a.label); put("options", a.options.toJson())
                put("accountProxyUri", a.proxyUri)
            })
        }
        val raw = JSONObject().apply { put("version", 2); put("accounts", accounts); put("proxyUri", config.proxyUri) }
            .toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val packed = cipher.iv + cipher.doFinal(raw)
        check(prefs.edit().putString("profile", Base64.encodeToString(packed, Base64.NO_WRAP)).commit()) {
            "Không thể lưu dữ liệu mã hóa"
        }
    }
}
