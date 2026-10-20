package com.ulm.azan.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.Times
import com.ulm.azan.location.LocationGate
import java.time.LocalDate

/**
 * Receives the exact alarms: the pre-Fajr du'a, the azan (or a notification if
 * away from home), then re-arms.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PrayerScheduler.ACTION_AZAN) {
            val prayer = Prayer.fromKey(intent.getStringExtra(PrayerScheduler.EXTRA_PRAYER))
                ?: Prayer.DHUHR
            val gate = LocationGate.evaluate(context)
            if (gate.away) {
                val time = Times.forDate(context, LocalDate.now())?.time(prayer)
                AwayNotifier.notifyAway(context, prayer, time)
            } else {
                val svc = Intent(context, AzanService::class.java).apply {
                    action = AzanService.ACTION_PLAY
                    putExtra(AzanService.EXTRA_PRAYER, prayer.key)
                    putExtra(AzanService.EXTRA_SCHEDULED, true)
                }
                ContextCompat.startForegroundService(context, svc)
            }
        }
        if (intent.action == PrayerScheduler.ACTION_PRE_FAJR) {
            val fajrAt = intent.getLongExtra(PrayerScheduler.EXTRA_FAJR_AT, 0L)
            // Away from home: stay silent (the Fajr alarm will send its own reminder).
            if (fajrAt > System.currentTimeMillis() && !LocationGate.evaluate(context).away) {
                val svc = Intent(context, AzanService::class.java).apply {
                    action = AzanService.ACTION_PRE_FAJR
                    putExtra(AzanService.EXTRA_FAJR_AT, fajrAt)
                }
                ContextCompat.startForegroundService(context, svc)
            }
        }
        PrayerScheduler.rescheduleAll(context)
    }
}
