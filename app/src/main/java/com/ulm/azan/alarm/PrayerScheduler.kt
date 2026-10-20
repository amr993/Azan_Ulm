package com.ulm.azan.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.PrayerStore
import com.ulm.azan.data.Settings
import com.ulm.azan.data.Times
import com.ulm.azan.widget.AzanWidgetProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules one exact alarm per enabled prayer (for its next upcoming occurrence)
 * plus a re-arm alarm just after midnight so the next day gets scheduled.
 * Call rescheduleAll on app open, on boot, and after every alarm fires.
 */
object PrayerScheduler {

    const val ACTION_AZAN = "com.ulm.azan.action.AZAN"
    const val ACTION_REARM = "com.ulm.azan.action.REARM"
    const val ACTION_PRE_FAJR = "com.ulm.azan.action.PRE_FAJR"
    const val EXTRA_FAJR_AT = "fajr_at"
    const val EXTRA_PRAYER = "prayer"

    private fun alarmManager(c: Context) =
        c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun prayerPendingIntent(c: Context, p: Prayer): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).apply {
            action = ACTION_AZAN
            putExtra(EXTRA_PRAYER, p.key)
        }
        return PendingIntent.getBroadcast(
            c, REQUEST_BASE + p.ordinal, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun rearmPendingIntent(c: Context): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).apply { action = ACTION_REARM }
        return PendingIntent.getBroadcast(
            c, REQUEST_REARM, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun preFajrPendingIntent(c: Context, fajrAtMs: Long): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).apply {
            action = ACTION_PRE_FAJR
            putExtra(EXTRA_FAJR_AT, fajrAtMs)
        }
        return PendingIntent.getBroadcast(
            c, REQUEST_PRE_FAJR, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun rescheduleAll(context: Context) {
        val settings = Settings(context)
        val am = alarmManager(context)
        val zone = ZoneId.systemDefault()

        for (p in Prayer.azanPrayers) am.cancel(prayerPendingIntent(context, p))
        am.cancel(preFajrPendingIntent(context, 0L))

        if (settings.enabled) {
            val now = LocalDateTime.now()
            for (p in Prayer.azanPrayers) {
                if (!settings.isPrayerEnabled(p)) continue
                val dt = nextOccurrence(context, p, now) ?: continue
                val triggerMs = dt.atZone(zone).toInstant().toEpochMilli()
                setExact(am, context, triggerMs, prayerPendingIntent(context, p))
                if (p == Prayer.FAJR && settings.fajrDuaEnabled) {
                    schedulePreFajr(context, am, dt.toLocalDate(), triggerMs)
                }
            }
        }
        scheduleRearm(context, am, zone)
        AzanWidgetProvider.updateAll(context)
    }

    private fun nextOccurrence(context: Context, p: Prayer, now: LocalDateTime): LocalDateTime? {
        for (addDays in 0..2L) {
            val date = now.toLocalDate().plusDays(addDays)
            val t = Times.forDate(context, date)?.time(p) ?: continue
            val dt = LocalDateTime.of(date, t)
            if (dt.isAfter(now)) return dt
        }
        return null
    }

    /** Start the weekday's du'a so it ends [FajrDua.GAP_MS] before Fajr. */
    private fun schedulePreFajr(context: Context, am: AlarmManager, date: LocalDate, fajrMs: Long) {
        val resId = FajrDua.resId(context, date)
        if (resId == 0) return
        val startMs = fajrMs - FajrDua.durationMs(context, resId) - FajrDua.GAP_MS
        // Already inside the du'a window (e.g. app opened minutes before Fajr):
        // skip today's du'a; the Fajr azan alarm is still set.
        if (startMs <= System.currentTimeMillis()) return
        setExact(am, context, startMs, preFajrPendingIntent(context, fajrMs))
    }

    private fun scheduleRearm(context: Context, am: AlarmManager, zone: ZoneId) {
        val next = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 5))
        val ms = next.atZone(zone).toInstant().toEpochMilli()
        setExact(am, context, ms, rearmPendingIntent(context))
    }

    private fun setExact(am: AlarmManager, context: Context, triggerMs: Long, pi: PendingIntent) {
        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                am.canScheduleExactAlarms() else true
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    private const val REQUEST_BASE = 1000
    private const val REQUEST_REARM = 999
    private const val REQUEST_PRE_FAJR = 998
}
