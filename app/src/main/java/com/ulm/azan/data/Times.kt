package com.ulm.azan.data

import android.content.Context
import com.ulm.azan.calc.PrayerCalculator
import java.time.LocalDate

/**
 * Single source of prayer times.
 *
 * Scanned timetable values always win: for every day and every prayer the
 * scanned time is used when one exists, otherwise the on-device calculation
 * (the mosque's own method) fills the gap. Without any scan, the app runs
 * entirely on the calculation.
 */
object Times {

    fun forDate(context: Context, date: LocalDate): DayTimes? =
        merge(PrayerStore(context).forDate(date), PrayerCalculator.compute(context, date))

    fun window(context: Context, start: LocalDate, days: Int): Map<LocalDate, DayTimes> {
        val stored = PrayerStore(context).loadAll()
        val m = LinkedHashMap<LocalDate, DayTimes>()
        for (i in 0 until days) {
            val d = start.plusDays(i.toLong())
            m[d] = merge(stored[d], PrayerCalculator.compute(context, d))
        }
        return m
    }

    private fun merge(scanned: DayTimes?, calculated: DayTimes): DayTimes {
        if (scanned == null || scanned.times.isEmpty()) return calculated
        val times = LinkedHashMap(calculated.times)
        times.putAll(scanned.times)          // scanned values override calculated ones
        return DayTimes(calculated.date, times)
    }
}
