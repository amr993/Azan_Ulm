package com.ulm.azan.calc

import android.content.Context
import com.ulm.azan.data.DayTimes
import com.ulm.azan.data.Prayer
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Offline prayer-time calculator reproducing the Al-Salam (Friedens) Moschee Ulm
 * timetable (Diyanet method):
 *   Fajr 18°, standard (Shafi) Asr, with fixed safety offsets
 *   (Sunrise −7, Dhuhr +5, Asr +4, Maghrib +7 min) and Isha = Maghrib + 90 min.
 * In the long-daylight window (~2 May – 10 Aug) Fajr follows the mosque's
 * high-latitude dawn rule, bundled as a per-date table (assets/summer_fajr.json).
 * Validated against 350 printed days (Jun–Oct 2026 and Feb, Jul–Dec 2025): every
 * prayer time within 1 minute, ~80–90% to the exact minute.
 */
object PrayerCalculator {

    private const val LAT = 48.3984
    private const val LNG = 9.9916
    /** Sun position is evaluated once per day at this fraction after local midnight
     *  (0.125 = 03:00). Calibrated against 350 printed days (2025 + 2026). */
    private const val SUN_EVAL_DAY_FRACTION = 0.125
    private val ZONE: ZoneId = ZoneId.of("Europe/Berlin")

    @Volatile private var summerFajr: Map<String, String>? = null

    private fun table(context: Context): Map<String, String> {
        summerFajr?.let { return it }
        val m = HashMap<String, String>()
        try {
            val txt = context.assets.open("summer_fajr.json").bufferedReader().use { it.readText() }
            val o = JSONObject(txt)
            val keys = o.keys()
            while (keys.hasNext()) { val k = keys.next(); m[k] = o.getString(k) }
        } catch (_: Exception) { }
        summerFajr = m
        return m
    }

    fun compute(context: Context, date: LocalDate): DayTimes {
        val tz = ZONE.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val decl = sunParams(date).first
        val noon = solarNoon(date, tz)
        val horizon = 0.833

        val sunrise = noon - hourAngle(decl, horizon) - 7.0 / 60.0
        val dhuhr = noon + 5.0 / 60.0
        val asr = noon + asrHourAngle(decl) + 4.0 / 60.0
        val maghrib = noon + hourAngle(decl, horizon) + 7.0 / 60.0
        val isha = maghrib + 90.0 / 60.0

        val key = "%02d-%02d".format(date.monthValue, date.dayOfMonth)
        val fajr: Double = table(context)[key]?.let { toHours(it) }
            ?: (hourAngleOrNull(decl, 18.0)?.let { noon - it } ?: (sunrise - 90.0 / 60.0))

        val m = LinkedHashMap<Prayer, LocalTime>()
        m[Prayer.FAJR] = toTime(fajr)
        m[Prayer.SUNRISE] = toTime(sunrise)
        m[Prayer.DHUHR] = toTime(dhuhr)
        m[Prayer.ASR] = toTime(asr)
        m[Prayer.MAGHRIB] = toTime(maghrib)
        m[Prayer.ISHA] = toTime(isha)
        return DayTimes(date, m)
    }

    // ---- astronomy ----
    private fun julian(date: LocalDate): Double {
        var y = date.year; var mo = date.monthValue; val d = date.dayOfMonth
        if (mo <= 2) { y -= 1; mo += 12 }
        val a = floor(y / 100.0); val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (mo + 1)) + d + b - 1524.5
    }

    private fun sunParams(date: LocalDate): Pair<Double, Double> {
        val jd = julian(date) - LNG / (15.0 * 24.0) + SUN_EVAL_DAY_FRACTION
        val dd = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * dd)
        val q = fixAngle(280.459 + 0.98564736 * dd)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * dd
        val ra = Math.toDegrees(atan2(dcos(e) * dsin(l), dcos(l))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = dasin(dsin(e) * dsin(l))
        return decl to eqt
    }

    private fun solarNoon(date: LocalDate, tz: Double): Double =
        12.0 - sunParams(date).second + tz - LNG / 15.0

    private fun hourAngleOrNull(decl: Double, angle: Double): Double? {
        val x = (-dsin(angle) - dsin(LAT) * dsin(decl)) / (dcos(LAT) * dcos(decl))
        if (x < -1.0 || x > 1.0) return null
        return dacos(x) / 15.0
    }

    private fun hourAngle(decl: Double, angle: Double): Double = hourAngleOrNull(decl, angle) ?: 0.0

    private fun asrHourAngle(decl: Double): Double {
        val g = Math.toDegrees(atan(1.0 / (1.0 + tan(Math.toRadians(abs(LAT - decl))))))
        val x = (dsin(g) - dsin(LAT) * dsin(decl)) / (dcos(LAT) * dcos(decl))
        return dacos(x) / 15.0
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dasin(x: Double) = Math.toDegrees(asin(x))
    private fun dacos(x: Double) = Math.toDegrees(acos(x))
    private fun fixAngle(a: Double) = a - 360.0 * floor(a / 360.0)
    private fun fixHour(h: Double) = h - 24.0 * floor(h / 24.0)
    private fun toHours(hhmm: String): Double {
        val p = hhmm.split(":"); return p[0].toInt() + p[1].toInt() / 60.0
    }
    private fun toTime(hours: Double): LocalTime {
        val norm = ((hours % 24.0) + 24.0) % 24.0
        var mins = Math.round(norm * 60.0).toInt() % (24 * 60)
        if (mins < 0) mins += 24 * 60
        return LocalTime.of(mins / 60, mins % 60)
    }
}
