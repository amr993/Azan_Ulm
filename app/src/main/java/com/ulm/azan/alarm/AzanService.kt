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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ulm.azan.MainActivity
import com.ulm.azan.R
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.Settings
import java.time.Instant
import java.time.ZoneId

/**
 * Foreground service that plays the azan over the alarm audio stream
 * (so it sounds even on silent / Do-Not-Disturb), with a Stop action.
 * Two seconds after the azan ends it plays the du'a (res/raw/doaa.mp3);
 * Stop cancels both.
 *
 * Before Fajr it first plays the weekday's du'a (see [FajrDua]), timed to end
 * just before Fajr, and then starts the Fajr azan itself exactly at Fajr time.
 * The regular Fajr alarm then sees the azan is already handled and stays quiet.
 *
 * Battery: the service only lives for the length of the azan. It uses
 * START_NOT_STICKY so the system never re-creates it after the audio ends or
 * if it is killed, and the wake lock is released as soon as playback stops.
 */
class AzanService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pendingDua: Runnable? = null
    private var pendingFajrAzan: Runnable? = null
    /** Fajr time (epoch ms) this service will start the azan for; 0 if none. */
    private var awaitingFajrAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_PRE_FAJR -> {
                val fajrAt = intent.getLongExtra(EXTRA_FAJR_AT, 0L)
                AzanPlaybackState.setPlaying(Prayer.FAJR.key)
                startInForeground(PRE_TITLE, PRE_TEXT)
                playPreFajrDua(fajrAt)
            }
            else -> {
                val prayer = Prayer.fromKey(intent?.getStringExtra(EXTRA_PRAYER)) ?: Prayer.DHUHR
                val scheduled = intent?.getBooleanExtra(EXTRA_SCHEDULED, false) ?: false
                if (scheduled && prayer == Prayer.FAJR && fajrAlreadyHandled()) {
                    // The pre-Fajr du'a already started (or is about to start) this azan.
                    // A foreground-service start must still call startForeground.
                    if (awaitingFajrAt != 0L) startInForeground(PRE_TITLE, PRE_TEXT)
                    else startInForeground(prayer)
                    if (player == null && pendingDua == null && pendingFajrAzan == null) stopSelf()
                } else {
                    AzanPlaybackState.setPlaying(prayer.key)
                    startInForeground(prayer)
                    playAzan(prayer)
                }
            }
        }
        // Do not recreate this short-lived service if the system kills it.
        return START_NOT_STICKY
    }

    /** True if the Fajr azan for (about) now was/will be started by the pre-Fajr du'a. */
    private fun fajrAlreadyHandled(): Boolean {
        if (awaitingFajrAt != 0L) return true
        val handled = Settings(this).fajrAzanHandledAt
        return handled != 0L && Math.abs(System.currentTimeMillis() - handled) < FAJR_GUARD_MS
    }

    private fun playPreFajrDua(fajrAt: Long) {
        cancelPending()
        acquireWakeLock()
        val date = Instant.ofEpochMilli(fajrAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val resId = FajrDua.resId(this, date)
        awaitingFajrAt = fajrAt
        if (resId == 0) { scheduleFajrAzan(fajrAt); return }
        startPlayer(
            Uri.parse("android.resource://$packageName/$resId"),
            onDone = { scheduleFajrAzan(fajrAt) }
        )
    }

    /** Start the Fajr azan exactly at [fajrAt] (at least GAP_MS after the du'a). */
    private fun scheduleFajrAzan(fajrAt: Long) {
        player?.release()
        player = null
        val delay = (fajrAt - System.currentTimeMillis()).coerceAtLeast(FajrDua.GAP_MS)
        if (delay > MAX_WAIT_MS) {          // clock changed; let the Fajr alarm handle it
            awaitingFajrAt = 0L
            stopSelf()
            return
        }
        val r = Runnable {
            pendingFajrAzan = null
            awaitingFajrAt = 0L
            // Remember it, so the regular Fajr alarm (firing now or a bit late) won't repeat it.
            Settings(this).fajrAzanHandledAt = fajrAt
            AzanPlaybackState.setPlaying(Prayer.FAJR.key)
            startInForeground(Prayer.FAJR)
            playAzan(Prayer.FAJR)
        }
        pendingFajrAzan = r
        handler.postDelayed(r, delay)
    }

    private fun startPlayer(uri: Uri, onDone: () -> Unit) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@AzanService, uri)
                setOnCompletionListener { onDone() }
                setOnErrorListener { _, _, _ -> onDone(); true }
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (_: Exception) {
            onDone()
        }
    }

    private fun startInForeground(prayer: Prayer) = startInForeground(
        "Azan — ${prayer.displayName}",
        "It is time for ${prayer.displayName} (${prayer.german})."
    )

    private fun startInForeground(title: String, text: String) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(title, text), type)
    }

    private fun playAzan(prayer: Prayer) {
        cancelPending()
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
                setOnCompletionListener { onAzanFinished() }
                setOnErrorListener { _, _, _ -> stopSelf(); true }
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (_: Exception) {
            stopSelf()
        }
    }

    /** After the azan: pause DUA_DELAY_MS, then play the du'a (if bundled). */
    private fun onAzanFinished() {
        player?.release()
        player = null
        val duaId = resources.getIdentifier("doaa", "raw", packageName)
        if (duaId == 0) { stopSelf(); return }
        val r = Runnable { playDua(duaId) }
        pendingDua = r
        handler.postDelayed(r, DUA_DELAY_MS)
    }

    private fun playDua(resId: Int) {
        pendingDua = null
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@AzanService, Uri.parse("android.resource://$packageName/$resId"))
                setOnCompletionListener { stopSelf() }
                setOnErrorListener { _, _, _ -> stopSelf(); true }
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun cancelPending() {
        pendingDua?.let { handler.removeCallbacks(it) }
        pendingDua = null
        pendingFajrAzan?.let { handler.removeCallbacks(it) }
        pendingFajrAzan = null
        awaitingFajrAt = 0L
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
        val wl = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AzanUlm:azan")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        // Safety cap (pre-Fajr du'a + azan + du'a ≈ 9 min max); re-acquiring just
        // extends it, and it is released in onDestroy.
        wl.acquire(15 * 60 * 1000L)
    }

    private fun buildNotification(title: String, text: String): Notification {
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
            .setContentTitle(title)
            .setContentText(text)
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
        cancelPending()
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
        const val ACTION_PRE_FAJR = "com.ulm.azan.service.PRE_FAJR"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_SCHEDULED = "scheduled"
        const val EXTRA_FAJR_AT = "fajr_at"
        private const val PRE_TITLE = "Before Fajr"
        private const val PRE_TEXT = "Du'a before the Fajr azan"
        private const val FAJR_GUARD_MS = 10 * 60 * 1000L
        private const val MAX_WAIT_MS = 5 * 60 * 1000L
        private const val CHANNEL_ID = "azan_channel"
        private const val NOTIF_ID = 4242
        private const val DUA_DELAY_MS = 2_000L
    }
}
