package com.ulm.azan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ulm.azan.data.DayTimes
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.PrayerStore
import com.ulm.azan.data.Settings
import com.ulm.azan.util.AppPermissions
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

@Composable
fun HomeScreen(
    store: PrayerStore,
    dataVersion: Int,
    loading: Boolean,
    message: String?,
    onDismissMessage: () -> Unit,
    onScanGallery: () -> Unit,
    onScanCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
    onTestAzan: (Prayer) -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val all = remember(dataVersion) { store.loadAll() }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val today = now.toLocalDate()
    val todayTimes = all[today]
    val next = computeNextAzan(all, settings, now)

    val canExact = AppPermissions.canScheduleExactAlarms(context)
    val ignoringBattery = AppPermissions.isIgnoringBatteryOptimizations(context)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ---- Header ----
            Surface(color = MaterialTheme.colorScheme.primary) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        "Azan Ulm",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Al-Salam (Friedens) Moschee — Ulm",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {

                if (loading) {
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.height(24.dp))
                            Spacer(Modifier.fillMaxWidth(0.05f))
                            Text("  Reading the image…")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                message?.let {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(it, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            TextButton(onClick = onDismissMessage, modifier = Modifier.align(Alignment.End)) {
                                Text("Dismiss")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ---- Next prayer banner ----
                if (next != null) {
                    val (p, dt) = next
                    val remaining = Duration.between(now, dt)
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                "Next prayer",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${p.displayName} · ${dt.toLocalTime().format(TIME_FMT)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "in ${formatDuration(remaining)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ---- Today's times ----
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Today — ${today.format(DATE_FMT)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        if (todayTimes == null) {
                            Text(
                                "No times stored for today. Scan this month's sheet to add them.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            for (p in Prayer.columns) {
                                val t = todayTimes.time(p) ?: continue
                                val isNext = next?.first == p &&
                                    next.second.toLocalDate() == today
                                val muted = p != Prayer.SUNRISE && !settings.isPrayerEnabled(p)
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        buildString {
                                            append(p.displayName)
                                            if (p == Prayer.SUNRISE) append("  (no azan)")
                                            else if (muted) append("  (muted)")
                                        },
                                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isNext) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        t.format(TIME_FMT),
                                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isNext) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ---- Permission prompts ----
                if (!canExact) {
                    WarningCard(
                        text = "Exact alarms are off. Without them the azan may be delayed. " +
                            "Open Settings to allow exact alarms.",
                        actionLabel = "Fix in Settings",
                        onAction = onOpenSettings
                    )
                }
                if (AppPermissions.needsNotificationPermission()) {
                    WarningCard(
                        text = "Allow notifications so the azan banner can show.",
                        actionLabel = "Allow notifications",
                        onAction = onRequestNotifications
                    )
                }
                if (!ignoringBattery) {
                    WarningCard(
                        text = "Battery optimization may stop alarms when the phone sleeps. " +
                            "Exempt this app for reliable azan.",
                        actionLabel = "Fix in Settings",
                        onAction = onOpenSettings
                    )
                }

                // ---- Actions ----
                Spacer(Modifier.height(4.dp))
                Button(onClick = onScanGallery, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan a sheet from gallery")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onScanCamera, modifier = Modifier.fillMaxWidth()) {
                    Text("Take a photo of the sheet")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Settings")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onTestAzan(Prayer.DHUHR) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test azan now")
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Stored: ${all.size} day(s)" + (store.dateRange()?.let {
                        " (${it.first.format(TIME_RANGE)} – ${it.second.format(TIME_RANGE)})"
                    } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private val TIME_RANGE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
private fun WarningCard(text: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onSecondary)
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(actionLabel, color = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** Finds the next enabled azan prayer at or after [now], scanning up to 2 days ahead. */
private fun computeNextAzan(
    all: Map<LocalDate, DayTimes>,
    settings: Settings,
    now: LocalDateTime
): Pair<Prayer, LocalDateTime>? {
    if (!settings.enabled) return null
    var best: Pair<Prayer, LocalDateTime>? = null
    for (addDays in 0..2L) {
        val date = now.toLocalDate().plusDays(addDays)
        val day = all[date] ?: continue
        for (p in Prayer.azanPrayers) {
            if (!settings.isPrayerEnabled(p)) continue
            val t = day.time(p) ?: continue
            val dt = LocalDateTime.of(date, t)
            if (dt.isAfter(now) && (best == null || dt.isBefore(best!!.second))) {
                best = p to dt
            }
        }
        if (best != null) break // earliest day with a hit wins
    }
    return best
}

private fun formatDuration(d: Duration): String {
    val total = d.seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, s) else "%dm %02ds".format(m, s)
}
                                  