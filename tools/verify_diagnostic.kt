import com.avatarbot.mobile.data.DiagnosticRedactor

fun main() {
    val values=listOf("tai_khoan_01", "Secret123", "socks5://user:proxypass@example.com:1080")
    val log="user=tai_khoan_01 passwd=Secret123 token=ABC123 proxy=socks5://user:proxypass@example.com:1080"
    val clean=DiagnosticRedactor.redact(log,values)
    require(!clean.contains("tai_khoan_01") && !clean.contains("Secret123"))
    require(!clean.contains("proxypass") && !clean.contains("ABC123"))
    require(clean.contains("token=[ẩn]") && clean.contains("passwd=[ẩn]"))
    require(DiagnosticRedactor.redact("message=hello",emptyList())=="message=hello")
    println("PASS: diagnostics redaction for usernames, passwords, proxy and tokens")
}
