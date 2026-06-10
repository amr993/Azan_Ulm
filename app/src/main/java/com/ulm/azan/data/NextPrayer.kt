package com.ulm.azan.data

import java.time.LocalDate
import java.time.LocalDateTime

/** Computes the next upcoming, enabled azan prayer from stored times. */
object NextPrayer {
    fun compute(
        all: Map<LocalDate, DayTimes>,
        settings: Settings,
        now: LocalDateTime
    ): Pair<Prayer, LocalDateTime>? {
        if (!settings.enabled) return null
        for (addDays in 0..2L) {
            val date = now.toLocalDate().plusDays(addDays)
            val day = all[date] ?: continue
            var best: Pair<Prayer, LocalDateTime>? = null
            for (p in Prayer.azanPrayers) {
                if (!settings.isPrayerEnabled(p)) continue
                val t = day.time(p) ?: continue
                val dt = LocalDateTime.of(date, t)
                if (dt.isAfter(now) && (best == null || dt.isBefore(best!!.second))) {
                    best = p to dt
                }
            }
            if (best != null) return best
        }
        return null
    }
}
