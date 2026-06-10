package com.ulm.azan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.ulm.azan.alarm.AzanPlaybackState
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
private val RANGE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

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
    val playing by AzanPlaybackState.playing.collectAsState()

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
            BrandHeader(
                title = "Al-Salam Mosque",
                arabic = "مسجد السلام",
                subtitle = "Friedensmoschee · Ulm"
            )

            Column(Modifier.padding(16.dp)) {

                if (loading) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.height(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Text("Reading the image…")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                message?.let {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(it, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            TextButton(onClick = onDismissMessage, modifier = Modifier.align(Alignment.End)) {
                                Text("Dismiss")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ---- Next prayer (gold-framed) ----
                if (next != null) {
                    val (p, dt) = next
                    val remaining = Duration.between(now, dt)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Brand.Gold, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StarBullet(12)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "NEXT PRAYER",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                StarBullet(12)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${p.displayName} · ${dt.toLocalTime().format(TIME_FMT)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "in ${formatDuration(remaining)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // ---- Today's times ----
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Today · ${today.format(DATE_FMT)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        OrnamentalDivider()
                        Spacer(Modifier.height(6.dp))
                        if (todayTimes == null) {
                            Text(
                                "No times stored for today. Scan this month's sheet to add them.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val cols = Prayer.columns
                            for ((index, p) in cols.withIndex()) {
                                val t = todayTimes.time(p) ?: continue
                                val isNext = next?.first == p &&
                                    next.second.toLocalDate() == today
                                val muted = p != Prayer.SUNRISE && !settings.isPrayerEnabled(p)
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StarBullet(if (isNext) 11 else 8,
                                            tint = if (isNext) MaterialTheme.colorScheme.primary else Brand.Gold)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            buildString {
                                                append(p.displayName)
                                                if (p == Prayer.SUNRISE) append("  · no azan")
                                                else if (muted) append("  · muted")
                                            },
                                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isNext) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        t.format(TIME_FMT),
                                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isNext) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (index < cols.size - 1) {
                                    Box(
                                        Modifier.fillMaxWidth().height(1.dp)
                                            .background(Brand.Gold.copy(alpha = 0.20f))
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                if (!canExact) {
                    WarningCard(
                        "Exact alarms are off — the azan may be delayed. Open Settings to allow them.",
                        "Fix in Settings", onOpenSettings
                    )
                }
                if (AppPermissions.needsNotificationPermission()) {
                    WarningCard(
                        "Allow notifications so the azan banner can show.",
                        "Allow notifications", onRequestNotifications
                    )
                }
                if (!ignoringBattery) {
                    WarningCard(
                        "Battery optimization may delay alarms. Exempt this app for reliable azan.",
                        "Fix in Settings", onOpenSettings
                    )
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onScanGallery,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Scan a sheet from gallery") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onScanCamera,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Take a photo of the sheet") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Settings") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onTestAzan(Prayer.DHUHR) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (playing == Prayer.DHUHR.key) "Stop azan" else "Test azan now")
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Stored: ${all.size} day(s)" + (store.dateRange()?.let {
                        " (${it.first.format(RANGE_FMT)} – ${it.second.format(RANGE_FMT)})"
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

@Composable
private fun WarningCard(text: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onSecondaryContainer)
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(actionLabel, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

private fun computeNextAzan(
    all: Map<LocalDate, DayTimes>,
    settings: Settings,
    now: LocalDateTime
): Pair<Prayer, LocalDateTime>? {
    if (!settings.enabled) return null
    for (addDays in 0..2L) {
        val date = now.toLocalDate().plusDays(addDays)
        val day = all[date] ?: continue
        var best: Pair<Prayer, LocalDateTime>? = null
        for (p in Prayer.azanPrayers) {
            if (!settings.isPrayerEnabled(p)) continue
            val t = day.time(p) ?: continue
            val dt = LocalDateTime.of(date, t)
            if (dt.isAfter(now) && (best == null || dt.isBefore(best!!.second))) best = p to dt
        }
        if (best != null) return best
    }
    return null
}

private fun formatDuration(d: Duration): String {
    val total = d.seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, s) else "%dm %02ds".format(m, s)
}
