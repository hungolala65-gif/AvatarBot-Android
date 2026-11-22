package org.json
/** JVM-only JSON facade for testing AutoOptions round trips without Android SDK. */
class JSONObject {
    private val values = mutableMapOf<String, Any>()
    fun put(key: String, value: Any): JSONObject { values[key] = value; return this }
    fun optBoolean(key: String, fallback: Boolean): Boolean = values[key] as? Boolean ?: fallback
    fun optInt(key: String, fallback: Int): Int = values[key] as? Int ?: fallback
    fun optString(key: String, fallback: String): String = values[key] as? String ?: fallback
}
