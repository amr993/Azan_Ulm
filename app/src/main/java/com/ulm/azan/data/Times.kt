package com.ulm.azan.data

import android.content.Context
import com.ulm.azan.calc.PrayerCalculator
import java.time.LocalDate

/**
 * Single source of prayer times: computed on-device when auto-calculation is on
 * (the default), otherwise read from scanned/stored sheets.
 */
object Times {
    fun forDate(context: Context, date: LocalDate): DayTimes? =
        if (Settings(context).autoCalcEnabled) PrayerCalculator.compute(context, date)
        else PrayerStore(context).forDate(date)

    fun window(context: Context, start: LocalDate, days: Int): Map<LocalDate, DayTimes> {
        val m = LinkedHashMap<LocalDate, DayTimes>()
        if (Settings(context).autoCalcEnabled) {
            for (i in 0 until days) {
                val d = start.plusDays(i.toLong())
                m[d] = PrayerCalculator.compute(context, d)
            }
        } else {
            val all = PrayerStore(context).loadAll()
            for (i in 0 until days) {
                val d = start.plusDays(i.toLong())
                all[d]?.let { m[d] = it }
            }
        }
        return m
    }
}
