package com.ulm.azan.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ulm.azan.MainActivity
import com.ulm.azan.R
import com.ulm.azan.data.Prayer

/**
 * Foreground service that plays the azan over the alarm audio stream
 * (so it sounds even on silent / Do-Not-Disturb), with a Stop action.
 *
 * Battery: the service only lives for the length of the azan. It uses
 * START_NOT_STICKY so the system never re-creates it after the audio ends or
 * if it is killed, and the wake lock is released as soon as playback stops.
 */
class AzanService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
            else -> {
                val prayer = Prayer.fromKey(intent?.getStringExtra(EXTRA_PRAYER)) ?: Prayer.DHUHR
                AzanPlaybackState.setPlaying(prayer.key)
                startInForeground(prayer)
                playAzan(prayer)
            }
        }
        // Do not recreate this short-lived service if the system kills it.
        return START_NOT_STICKY
    }

    private fun startInForeground(prayer: Prayer) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(prayer), type)
    }

    private fun playAzan(prayer: Prayer) {
        acquireWakeLock()
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@AzanService, resolveAzanUri(prayer))
                setOnCompletionListener { stopSelf() }
                setOnErrorListener { _, _, _ -> stopSelf(); true }
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (_: Exception) {
            stopSelf()
        }
    }

    /**
     * Uses res/raw/azan_fajr.mp3 for Fajr and res/raw/azan.mp3 otherwise.
     * Resolved by name so the app still compiles if the files are absent,
     * falling back to the system alarm sound.
     */
    private fun resolveAzanUri(prayer: Prayer): Uri {
        val preferred = if (prayer == Prayer.FAJR) "azan_fajr" else "azan"
        val id = resources.getIdentifier(preferred, "raw", packageName)
        val fallbackId = resources.getIdentifier("azan", "raw", packageName)
        return when {
            id != 0 -> Uri.parse("android.resource://$packageName/$id")
            fallbackId != 0 -> Uri.parse("android.resource://$packageName/$fallbackId")
            else -> RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AzanUlm:azan").apply {
            setReferenceCounted(false)
            // Safety cap; released earlier in onDestroy when the azan ends.
            acquire(3 * 60 * 1000L)
        }
    }

    private fun buildNotification(prayer: Prayer): Notification {
        createChannel()
        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, AzanService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Azan — ${prayer.displayName}")
            .setContentText("It is time for ${prayer.displayName} (${prayer.german}).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, "Stop", stopPi)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID, "Azan", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Plays the call to prayer at prayer times"
                    setSound(null, null) // audio is handled by MediaPlayer, not the channel
                    enableVibration(true)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    override fun onDestroy() {
        AzanPlaybackState.setPlaying(null)
        player?.release()
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.ulm.azan.service.PLAY"
        const val ACTION_STOP = "com.ulm.azan.service.STOP"
        const val EXTRA_PRAYER = "prayer"
        private const val CHANNEL_ID = "azan_channel"
        private const val NOTIF_ID = 4242
    }
}
