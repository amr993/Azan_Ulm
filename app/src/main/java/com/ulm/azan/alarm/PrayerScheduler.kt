package com.ulm.azan.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.PrayerStore
import com.ulm.azan.data.Settings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules one exact alarm per enabled prayer (for its next upcoming occurrence)
 * plus a re-arm alarm just after midnight so the next day gets scheduled.
 * Call [rescheduleAll] on app open, on boot, and after every alarm fires.
 */
object PrayerScheduler {

    const val ACTION_AZAN = "com.ulm.azan.action.AZAN"
    const val ACTION_REARM = "com.ulm.azan.action.REARM"
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

    fun rescheduleAll(context: Context) {
        val store = PrayerStore(context)
        val settings = Settings(context)
        val am = alarmManager(context)
        val zone = ZoneId.systemDefault()

        // Clear all prayer alarms first.
        for (p in Prayer.azanPrayers) am.cancel(prayerPendingIntent(context, p))

        if (settings.enabled) {
            val now = LocalDateTime.now()
            for (p in Prayer.azanPrayers) {
                if (!settings.isPrayerEnabled(p)) continue
                val dt = nextOccurrence(store, p, now) ?: continue
                val triggerMs = dt.atZone(zone).toInstant().toEpochMilli()
                setExact(am, context, triggerMs, prayerPendingIntent(context, p))
            }
        }
        scheduleRearm(context, am, zone)
    }

    /** Find the soonest future date/time for [p] across today..+2 days. */
    private fun nextOccurrence(store: PrayerStore, p: Prayer, now: LocalDateTime): LocalDateTime? {
        for (addDays in 0..2L) {
            val date = now.toLocalDate().plusDays(addDays)
            val t = store.forDate(date)?.time(p) ?: continue
            val dt = LocalDateTime.of(date, t)
            if (dt.isAfter(now)) return dt
        }
        return null
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
                // Falls back to an inexact (but still doze-friendly) alarm.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    private const val REQUEST_BASE = 1000
    private const val REQUEST_REARM = 999
}
