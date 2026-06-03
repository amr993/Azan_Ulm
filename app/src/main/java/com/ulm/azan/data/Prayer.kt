package com.ulm.azan.data

/**
 * The six time columns on the mosque sheet, left to right.
 * Sunrise (Sonnenaufgang) is stored for reference but never triggers an azan.
 */
enum class Prayer(val key: String, val displayName: String, val german: String) {
    FAJR("fajr", "Fajr", "Morgengebet"),
    SUNRISE("sunrise", "Sunrise", "Sonnenaufgang"),
    DHUHR("dhuhr", "Dhuhr", "Mittagsgebet"),
    ASR("asr", "Asr", "Nachmittagsgebet"),
    MAGHRIB("maghrib", "Maghrib", "Abendgebet"),
    ISHA("isha", "Isha", "Nachtgebet");

    companion object {
        /** Column order as printed on the sheet. */
        val columns: List<Prayer> = listOf(FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA)

        /** Prayers that play the azan (the five daily prayers). */
        val azanPrayers: List<Prayer> = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)

        fun fromKey(key: String?): Prayer? = entries.firstOrNull { it.key == key }
    }
}
