import com.avatarbot.mobile.engine.KotlinNetworkProbe
import java.net.ServerSocket
import java.io.DataInputStream
import kotlin.concurrent.thread

fun main() {
    ServerSocket(0).use { server ->
        val thread = thread { server.accept().use { it.close() } }
        val ok = KotlinNetworkProbe.probeJSON("127.0.0.1:${server.localPort}", "")
        check(ok.optBoolean("ok") && !ok.optBoolean("viaProxy")) { ok.optString("error") }
        thread.join()
    }
    val invalid = KotlinNetworkProbe.probeJSON("not_a_host", "")
    check(!invalid.optBoolean("ok"))
    ServerSocket(0).use { server ->
        val thread = thread {
            server.accept().use { client ->
                val i = DataInputStream(client.getInputStream())
                val out = client.getOutputStream()
                check(i.readUnsignedByte() == 5)
                val count = i.readUnsignedByte()
                val methods = ByteArray(count)
                i.readFully(methods)
                check(methods.contains(2.toByte()))
                out.write(byteArrayOf(5,2))
                check(i.readUnsignedByte() == 1)
                val ulen = i.readUnsignedByte(); val u = ByteArray(ulen); i.readFully(u)
                val plen = i.readUnsignedByte(); val p = ByteArray(plen); i.readFully(p)
                check(String(u) == "login" && String(p) == "secret")
                out.write(byteArrayOf(1,0))
                val request = ByteArray(5); i.readFully(request)
                check(request[0] == 5.toByte() && request[3] == 3.toByte())
                val hostname = ByteArray(request[4].toInt()); i.readFully(hostname)
                check(String(hostname) == "avatar.example")
                i.readUnsignedByte(); i.readUnsignedByte()  // port
                out.write(byteArrayOf(5,0,0,1,127,0,0,1,0,80))
                out.flush()
            }
        }
        val result = KotlinNetworkProbe.probeJSON("avatar.example:19128", "socks5://login:secret@127.0.0.1:${server.localPort}")
        check(result.optBoolean("ok") && result.optBoolean("viaProxy")) { result.optString("error") }
        thread.join()
    }
    println("PASS: Kotlin TCP direct, invalid endpoint and SOCKS5 username/password")
}
