package com.avatarbot.mobile.data

/** Pure Kotlin: safe to test on a desktop JVM without Android. */
data class ImportedAccount(val username: String, val password: String, val label: String = "")
data class ImportIssue(val line: Int, val reason: String)
data class ImportResult(val records: List<ImportedAccount>, val issues: List<ImportIssue>)

/**
 * Implements import formats documented by the PC user guide. Never include input
 * lines or passwords in diagnostics (error messages may be shared).
 * Lines: username password [label], or username,password[,label], or the same
 * with |, ;, -, ignoring # comments and empty lines.
 */
object AccountImporter {
    const val MAX_ACCOUNTS = 1000
    const val MAX_TEXT_LENGTH = 150_000

    fun parse(text: String): ImportResult {
        if (text.length > MAX_TEXT_LENGTH) {
            return ImportResult(emptyList(), listOf(ImportIssue(0, "Dữ liệu nhập quá dài")))
        }
        val records = mutableListOf<ImportedAccount>()
        val issues = mutableListOf<ImportIssue>()
        val seen = mutableSetOf<String>()
        text.lineSequence().forEachIndexed { position, original ->
            val line = original.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val lineNo = position + 1
            if (records.size >= MAX_ACCOUNTS) {
                if (issues.none { it.reason == "Tối đa $MAX_ACCOUNTS tài khoản" }) {
                    issues += ImportIssue(lineNo, "Tối đa $MAX_ACCOUNTS tài khoản")
                }
                return@forEachIndexed
            }
            val delimiter = listOf('|', ',', ';').firstOrNull { line.contains(it) }
                ?: if (line.contains(Regex("\\s"))) null else if (line.contains('-')) '-' else null
            val fields = if (delimiter != null) {
                val first = line.indexOf(delimiter)
                val second = line.indexOf(delimiter, first + 1)
                if (second >= 0) listOf(line.substring(0, first).trim(),
                    line.substring(first + 1, second).trim(), line.substring(second + 1).trim())
                else listOf(line.substring(0, first).trim(), line.substring(first + 1).trim())
            } else line.split(Regex("\\s+"), limit = 3)
            if (fields.size < 2 || fields[0].isBlank() || fields[1].isBlank()) {
                issues += ImportIssue(lineNo, "Thiếu tên đăng nhập hoặc mật khẩu")
                return@forEachIndexed
            }
            val username = fields[0]
            val password = fields[1]
            val label = fields.getOrElse(2) { "" }
            if (username.length > 256 || password.length > 512 || label.length > 128) {
                issues += ImportIssue(lineNo, "Một trường nhập vượt giới hạn độ dài")
                return@forEachIndexed
            }
            if (username.any { it.isWhitespace() }) {
                issues += ImportIssue(lineNo, "Tên đăng nhập chứa khoảng trắng")
                return@forEachIndexed
            }
            if (!seen.add(username.lowercase())) {
                issues += ImportIssue(lineNo, "Tên đăng nhập trùng trong danh sách")
                return@forEachIndexed
            }
            records += ImportedAccount(username, password, label)
        }
        return ImportResult(records, issues)
    }
}
