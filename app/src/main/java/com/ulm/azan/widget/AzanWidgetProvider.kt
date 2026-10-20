package com.ulm.azan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.ulm.azan.MainActivity
import com.ulm.azan.R
import com.ulm.azan.data.DayPhase
import com.ulm.azan.data.NextPrayer
import com.ulm.azan.data.Times
import com.ulm.azan.data.Settings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home-screen widget showing the next prayer and a live countdown.
 *
 * The countdown uses a system [android.widget.Chronometer] in count-down mode:
 * the launcher ticks it every second on its own, so the app is NOT woken each
 * second. The static text (prayer name/time) is refreshed only when something
 * changes — app open, boot, settings/data change, or when an azan alarm fires.
 */
class AzanWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    companion object {
        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

        /** Refresh every placed instance of this widget. Safe to call anytime. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, AzanWidgetProvider::class.java)
            )
            for (id in ids) updateWidget(context, manager, id)
        }

        private fun skyBackground(phase: DayPhase): Int = when (phase) {
            DayPhase.NIGHT -> R.drawable.widget_sky_night
            DayPhase.DAWN -> R.drawable.widget_sky_dawn
            DayPhase.DAY -> R.drawable.widget_sky_day
            DayPhase.AFTERNOON -> R.drawable.widget_sky_afternoon
            DayPhase.SUNSET -> R.drawable.widget_sky_sunset
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_next_prayer)

            val settings = Settings(context)

            // Same sky as the app: background follows the part of the day.
            val phase = DayPhase.of(Times.forDate(context, LocalDate.now()), LocalTime.now())
            views.setInt(R.id.widget_root, "setBackgroundResource", skyBackground(phase))
            val next = NextPrayer.compute(Times.window(context, LocalDate.now(), 3), settings, LocalDateTime.now())

            if (next == null) {
                views.setViewVisibility(R.id.widget_countdown, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                views.setTextViewText(R.id.widget_prayer, "Azan Ulm")
                views.setTextViewText(
                    R.id.widget_empty,
                    if (!settings.enabled) "Azan is turned off"
                    else "No prayer is selected for the azan"
                )
            } else {
                val (prayer, dateTime) = next
                views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty, View.GONE)
                views.setTextViewText(
                    R.id.widget_prayer,
                    "${prayer.displayName} · ${dateTime.toLocalTime().format(TIME)}"
                )
                val millisLeft = Duration.between(LocalDateTime.now(), dateTime)
                    .toMillis().coerceAtLeast(0L)
                views.setChronometer(
                    R.id.widget_countdown,
                    SystemClock.elapsedRealtime() + millisLeft,
                    null,
                    true
                )
                views.setChronometerCountDown(R.id.widget_countdown, true)
            }

            val openApp = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            manager.updateAppWidget(id, views)
        }
    }
}
