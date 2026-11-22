package com.avatarbot.mobile.engine

import org.json.JSONObject
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Android/JVM TCP & SOCKS5 diagnostics, used even before the optional Go AAR is
 * installed. Only checks the transport; NEVER sends a game login or account data.
 */
object KotlinNetworkProbe {
    private const val TIMEOUT_MS = 6000

    fun probeJSON(endpoint: String, proxyURI: String): JSONObject {
        val begin = System.nanoTime()
        val viaProxy = proxyURI.isNotBlank()
        var connected = false
        var error: String? = null
        try {
            parseHostPort(endpoint).let { (host, port) ->
                if (viaProxy) openSocks5(host, port, proxyURI.trim()).use { connected = true }
                else Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
                    connected = true
                }
            }
        } catch (e: Exception) {
            error = when (e) {
                is IllegalArgumentException -> "Địa chỉ hoặc proxy không hợp lệ"
                is java.net.SocketTimeoutException -> "Hết thời gian kết nối"
                is java.net.ConnectException -> "Server/proxy từ chối kết nối"
                else -> "Không thể kết nối TCP/SOCKS5"
            }
        }
        return JSONObject().put("ok", connected).put("viaProxy", viaProxy)
            .put("latencyMs", (System.nanoTime() - begin) / 1_000_000L)
            .put("endpoint", endpoint).apply { if (error != null) put("error", error) }
    }

    private fun parseHostPort(value: String): Pair<String, Int> {
        val uri = URI("tcp://$value")
        val host = uri.host ?: throw IllegalArgumentException("host")
        val port = uri.port
        require(port in 1..65535 && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawPath.isNullOrEmpty())
        return host to port
    }

    private fun openSocks5(host: String, port: Int, raw: String): Socket {
        val uri = URI(raw)
        require(uri.scheme == "socks5" && !uri.host.isNullOrBlank() && uri.port in 1..65535 &&
            uri.rawQuery == null && uri.rawFragment == null && uri.rawPath.isNullOrEmpty())
        val auth = uri.rawUserInfo?.split(':', limit = 2)
        if (auth != null) require(auth.size == 2)
        val user = auth?.get(0)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()).toByteArray(Charsets.UTF_8) }
        val pass = auth?.get(1)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()).toByteArray(Charsets.UTF_8) }
        require((user == null || user.size in 1..255) && (pass == null || pass.size <= 255))
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(uri.host, uri.port), TIMEOUT_MS)
            socket.soTimeout = TIMEOUT_MS
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            output.write(if (user == null) byteArrayOf(5, 1, 0) else byteArrayOf(5, 2, 0, 2))
            output.flush()
            val hello = ByteArray(2)
            readFully(input, hello)
            require(hello[0] == 5.toByte())
            when (hello[1].toInt() and 0xff) {
                0 -> Unit
                2 -> {
                    require(user != null && pass != null)
                    output.write(byteArrayOf(1, user.size.toByte()))
                    output.write(user)
                    output.write(pass.size)
                    output.write(pass)
                    output.flush()
                    val answer = ByteArray(2)
                    readFully(input, answer)
                    require(answer[0] == 1.toByte() && answer[1] == 0.toByte())
                }
                else -> throw IllegalArgumentException("auth")
            }
            val hostname = host.toByteArray(Charsets.UTF_8)
            require(hostname.size in 1..255)
            // SOCKS5 domain-based connection: DNS resolution is performed by proxy.
            output.write(byteArrayOf(5, 1, 0, 3, hostname.size.toByte()))
            output.write(hostname)
            output.write(byteArrayOf((port ushr 8).toByte(), port.toByte()))
            output.flush()
            val header = ByteArray(4)
            readFully(input, header)
            require(header[0] == 5.toByte() && header[1] == 0.toByte() && header[2] == 0.toByte())
            val addressSize = when (header[3].toInt() and 0xff) {
                1 -> 4
                4 -> 16
                3 -> input.read().also { if (it < 0) throw EOFException() }
                else -> throw IllegalArgumentException("reply")
            }
            readFully(input, ByteArray(addressSize + 2))
            return socket
        } catch (e: Exception) {
            socket.close()
            throw e
        }
    }

    private fun readFully(input: java.io.InputStream, data: ByteArray) {
        var pos = 0
        while (pos < data.size) {
            val n = input.read(data, pos, data.size - pos)
            if (n <= 0) throw EOFException()
            pos += n
        }
    }
}
