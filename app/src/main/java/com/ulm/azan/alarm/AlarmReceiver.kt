package com.ulm.azan.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.PrayerStore
import com.ulm.azan.location.LocationGate
import java.time.LocalDate

/** Receives the exact alarms: plays the azan (or notifies if away), then re-arms. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PrayerScheduler.ACTION_AZAN) {
            val prayer = Prayer.fromKey(intent.getStringExtra(PrayerScheduler.EXTRA_PRAYER))
                ?: Prayer.DHUHR
            val gate = LocationGate.evaluate(context)
            if (gate.away) {
                val time = PrayerStore(context).forDate(LocalDate.now())?.time(prayer)
                AwayNotifier.notifyAway(context, prayer, time)
            } else {
                val svc = Intent(context, AzanService::class.java).apply {
                    action = AzanService.ACTION_PLAY
                    putExtra(AzanService.EXTRA_PRAYER, prayer.key)
                }
                ContextCompat.startForegroundService(context, svc)
            }
        }
        PrayerScheduler.rescheduleAll(context)
    }
}
