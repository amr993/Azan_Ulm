package com.ulm.azan.data

import java.time.LocalTime

/**
 * Which part of the day it is, derived from today's prayer times. Drives the
 * sky colours of the app and the home-screen widget.
 */
enum class DayPhase(val label: String) {
    NIGHT("Night"),
    DAWN("Dawn"),
    DAY("Day"),
    AFTERNOON("Afternoon"),
    SUNSET("Sunset");

    companion object {
        fun of(day: DayTimes?, now: LocalTime): DayPhase {
            val fajr = day?.time(Prayer.FAJR) ?: LocalTime.of(5, 0)
            val sunrise = day?.time(Prayer.SUNRISE) ?: LocalTime.of(7, 0)
            val asr = day?.time(Prayer.ASR) ?: LocalTime.of(15, 30)
            val maghrib = day?.time(Prayer.MAGHRIB) ?: LocalTime.of(19, 0)
            val isha = day?.time(Prayer.ISHA) ?: LocalTime.of(20, 30)
            return when {
                now.isBefore(fajr) -> NIGHT
                now.isBefore(sunrise.plusMinutes(40)) -> DAWN
                now.isBefore(asr) -> DAY
                now.isBefore(maghrib.minusMinutes(30)) -> AFTERNOON
                now.isBefore(isha) -> SUNSET
                else -> NIGHT
            }
        }
    }
}
