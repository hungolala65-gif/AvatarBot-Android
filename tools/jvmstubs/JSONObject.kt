package org.json
/** Test-only stub. Not shipped with the Android application. */
class JSONObject {
    private val values = mutableMapOf<String, Any?>()
    fun put(key: String, value: Any?): JSONObject { values[key] = value; return this }
    fun optBoolean(key: String, default: Boolean = false): Boolean = values[key] as? Boolean ?: default
    fun optString(key: String): String = values[key]?.toString() ?: ""
    fun optLong(key: String): Long = values[key] as? Long ?: 0L
}
