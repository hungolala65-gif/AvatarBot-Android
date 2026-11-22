import com.avatarbot.mobile.data.AccountImporter
fun main() {
    val text = """
# ghi chú
acc1 pass1 Tài khoản chính
acc2,pass2,Acc phụ
acc3;pass3
acc4|pass4|Chính
acc5-pass5
acc6|pass6
acc1 passagain
invalid
""".trimIndent()
    val r = AccountImporter.parse(text)
    check(r.records.size == 6) { "records=${r.records}" }
    check(r.issues.size == 2) { "issues=${r.issues}" }
    check(r.records[0].label == "Tài khoản chính")
    check(r.records[4].password == "pass5")
    check(AccountImporter.parse("a|").issues.size == 1)
    check(AccountImporter.parse("#hello\n \n").issues.isEmpty())
    check(AccountImporter.parse("x|pass|note").records.single().label == "note")
    check(!r.issues.toString().contains("passagain"))
    println("PASS: AccountImporter 8 checks")
}
