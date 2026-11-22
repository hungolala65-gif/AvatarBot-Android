package com.avatarbot.mobile.engine

import com.avatarbot.mobile.data.BotAccount
import org.json.JSONArray
import org.json.JSONObject

/** Reflection keeps the Compose app buildable before mobilecore.aar is installed.
 *  Go package mobilecore generates mobilecore.Mobilecore and mobilecore.Engine.
 *  Fail closed on missing/incompatible AAR: do not silently emulate auto tasks.
 */
class GoMobileBridge {
    private val engine: Any?
    val installError: String?

    init {
        var obj: Any? = null
        var err: String? = null
        try {
            val klass = Class.forName("mobilecore.Mobilecore")
            val newEngine = klass.methods.firstOrNull {
                it.name.equals("newEngine", ignoreCase = true) && it.parameterCount == 0
            } ?: error("Không tìm thấy Mobilecore.newEngine")
            obj = newEngine.invoke(null) ?: error("Go không khởi tạo Engine")
        } catch (_: ClassNotFoundException) {
            err = "Chưa có mobilecore.aar (cần gomobile bind)"
        } catch (e: LinkageError) {
            err = "Thư viện native Go không tương thích hoặc thiếu ABI: ${e.javaClass.simpleName}"
        } catch (e: Exception) {
            err = "Go AAR không tương thích: ${e.javaClass.simpleName}"
        }
        engine = obj
        installError = err
    }

    val available: Boolean get() = engine != null

    private fun call(name: String, vararg args: Any): Any? {
        val instance = engine ?: error(installError ?: "Go core chưa cài")
        val method = instance.javaClass.methods.firstOrNull {
            it.name.equals(name, ignoreCase = true) && it.parameterCount == args.size
        } ?: error("Go AAR chưa có hàm $name")
        return method.invoke(instance, *args)
    }

    fun capabilities(): JSONObject {
        if (engine == null) return JSONObject().put("nativeGo", false).put("gameAutomation", false)
        return JSONObject(call("capabilitiesJSON") as String)
    }

    fun pushAccounts(accounts: List<BotAccount>) {
        // Passwords deliberately never leave encrypted Android storage at this stage.
        val array = JSONArray()
        accounts.forEach {
            array.put(JSONObject().put("id", it.id).put("username", it.username)
                .put("city", it.city).put("enabled", it.enabled)
                .put("options", it.options.toJson()))
        }
        call("setAccountsJSON", array.toString())
    }

    fun probe(endpoint: String, proxyURI: String): JSONObject =
        if (available) JSONObject(call("probeJSON", endpoint, proxyURI) as String)
        else KotlinNetworkProbe.probeJSON(endpoint, proxyURI)

    fun start() { call("start") }
    fun stop() { if (available) call("stop") }
    fun state(): JSONObject = if (available) JSONObject(call("stateJSON") as String) else JSONObject()
}

/** One engine instance shared by Activity and Service within the app process. */
object LocalBotRuntime {
    val bridge: GoMobileBridge by lazy { GoMobileBridge() }
}
