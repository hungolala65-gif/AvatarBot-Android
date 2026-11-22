package com.avatarbot.mobile.data

/** Local-only diagnostics: omit obvious credentials before displaying or copying logs.
 *  This is best-effort redaction. Users must review before sharing any report.
 */
object DiagnosticRedactor {
    private val sensitiveKey = Regex("(?i)\\b(password|passwd|pwd|token|secret|authorization)\\s*[:=]\\s*[^\\s;,]+")
    private val proxyUri = Regex("(?i)socks5://[^\\s,;]+")

    fun redact(text: String, knownSecrets: Collection<String>): String {
        var output = text
        knownSecrets.asSequence().filter { it.isNotBlank() }.distinct()
            .sortedByDescending { it.length }.forEach { secret ->
                output = output.replace(secret, "[ẩn]")
            }
        output = proxyUri.replace(output, "socks5://[ẩn]")
        output = sensitiveKey.replace(output) { match ->
            match.groupValues[1] + "=[ẩn]"
        }
        return output
    }
}
