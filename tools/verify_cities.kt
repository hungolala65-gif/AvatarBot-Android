import com.avatarbot.mobile.data.GameCities
fun main() {
    check(GameCities.endpoint("hoanmy") == GameCities.HOAN_MY)
    check(GameCities.endpoint("dieuky") == GameCities.DIEU_KY)
    for (bad in listOf("", "HOANMY", "hanoi", "null", "hoanmy ")) {
        check(runCatching { GameCities.endpoint(bad) }.isFailure) { "Accepted unknown server: $bad" }
        check(GameCities.label(bad).contains("không hợp lệ"))
    }
    println("PASS: game destination strict selection, 5 invalid city codes fail closed")
}
