package com.avatarbot.mobile.data

/** Known game destinations. Unknown/corrupt saved city codes fail closed on login. */
object GameCities {
    const val HOAN_MY = "112.213.85.102:19128"
    const val DIEU_KY = "112.213.85.101:19128"

    fun endpoint(city: String): String = when (city) {
        "hoanmy" -> HOAN_MY
        "dieuky" -> DIEU_KY
        else -> throw IllegalArgumentException("Mã thành phố không hợp lệ")
    }

    /** Display unknown values without crashing the account list. */
    fun label(city: String): String = when (city) {
        "hoanmy" -> "Hoàn Mỹ"
        "dieuky" -> "Diệu Kỳ"
        else -> "Thành phố không hợp lệ"
    }
}
