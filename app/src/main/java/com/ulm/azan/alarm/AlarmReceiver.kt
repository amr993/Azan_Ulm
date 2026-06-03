package com.ulm.azan.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ulm.azan.data.Prayer

/** Receives the exact alarms: plays the azan, then re-arms upcoming alarms. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PrayerScheduler.ACTION_AZAN) {
            val prayer = Prayer.fromKey(intent.getStringExtra(PrayerScheduler.EXTRA_PRAYER))
                ?: Prayer.DHUHR
            val svc = Intent(context, AzanService::class.java).apply {
                action = AzanService.ACTION_PLAY
                putExtra(AzanService.EXTRA_PRAYER, prayer.key)
            }
            ContextCompat.startForegroundService(context, svc)
        }
        // Re-arm after any alarm (azan or the post-midnight re-arm).
        PrayerScheduler.rescheduleAll(context)
    }
}
