package com.ulm.azan.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ulm.azan.MainActivity
import com.ulm.azan.R
import com.ulm.azan.data.Prayer
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Posts a silent reminder (no azan) when the user is away from home at prayer time. */
object AwayNotifier {
    private const val CHANNEL = "away_channel"
    private val HM = DateTimeFormatter.ofPattern("HH:mm")

    fun notifyAway(context: Context, prayer: Prayer, time: LocalTime?) {
        createChannel(context)
        val openPi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val timeStr = time?.format(HM)?.let { " · $it" } ?: ""
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${prayer.displayName}$timeStr")
            .setContentText("It is time to pray — the azan stayed silent because you are out.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(5000 + prayer.ordinal, notif)
        } catch (_: SecurityException) { /* notifications not permitted */ }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(
                    CHANNEL, "Away reminders", NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Silent reminders when you are away from home at prayer time"
                }
                nm.createNotificationChannel(ch)
            }
        }
    }
}
