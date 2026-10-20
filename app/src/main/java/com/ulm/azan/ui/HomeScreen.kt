package com.ulm.azan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ulm.azan.data.DayPhase
import com.ulm.azan.data.DayTimes
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.PrayerStore
import com.ulm.azan.data.Settings
import com.ulm.azan.data.Times
import com.ulm.azan.util.AppPermissions
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val DAY_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

private enum class RowState { PAST, NEXT, FUTURE }

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
) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val now = rememberTicker(1000)

    val today = now.toLocalDate()
    // Yesterday..+2 so the ring can measure from last night's Isha.
    val all = remember(dataVersion, today) { Times.window(context, today.minusDays(1), 4) }
    val todayTimes = all[today]
    val phase = DayPhase.of(todayTimes, now.toLocalTime())

    val azanOn = settings.enabled && Prayer.azanPrayers.any { settings.isPrayerEnabled(it) }
    val (prev, next) = neighbours(all, now) { !azanOn || settings.isPrayerEnabled(it) }

    val canExact = AppPermissions.canScheduleExactAlarms(context)
    val ignoringBattery = AppPermissions.isIgnoringBatteryOptimizations(context)

    SkyBackground(phase) {
        val p = LocalSkyPalette.current
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            SkyTopBar(title = "Al-Salam Mosque", arabic = "مسجد السلام · Ulm") {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            Text(
                buildString {
                    append(today.format(DAY_FMT))
                    hijriLabel(today)?.let { append("  ·  ").append(it) }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Sky.TextSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(18.dp))

            // ---- Countdown ring ----
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val progress = if (prev != null && next != null) {
                    val total = Duration.between(prev.second, next.second).seconds.toFloat()
                    val done = Duration.between(prev.second, now).seconds.toFloat()
                    if (total > 0f) (done / total).coerceIn(0f, 1f) else 0f
                } else 0f
                CountdownRing(
                    progress = progress,
                    prayer = next?.first,
                    at = next?.second,
                    remaining = next?.let { Duration.between(now, it.second) },
                    note = if (!azanOn) "Azan is off" else null
                )
            }

            Spacer(Modifier.height(18.dp))

            Column(Modifier.padding(horizontal = 16.dp)) {

                if (loading) {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                Modifier.size(22.dp), color = p.accent, strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(14.dp))
                            Text("Reading the image…")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                message?.let {
                    GlassCard(highlight = true) {
                        Text(it)
                        TextButton(onClick = onDismissMessage, modifier = Modifier.align(Alignment.End)) {
                            Text("Dismiss", color = p.accent)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ---- Today's timeline ----
                GlassCard(contentPadding = 18.dp) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(phase.label, style = MaterialTheme.typography.labelLarge, color = p.accent)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (todayTimes == null) {
                        Text(
                            "No times available for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Sky.TextSoft
                        )
                    } else {
                        val cols = Prayer.columns.filter { todayTimes.time(it) != null }
                        for ((index, pr) in cols.withIndex()) {
                            val t = todayTimes.time(pr)!!
                            val dt = LocalDateTime.of(today, t)
                            val state = when {
                                next != null && next.first == pr && next.second.toLocalDate() == today -> RowState.NEXT
                                !dt.isAfter(now) -> RowState.PAST
                                else -> RowState.FUTURE
                            }
                            val note = when {
                                pr == Prayer.SUNRISE -> "no azan"
                                !settings.enabled || !settings.isPrayerEnabled(pr) -> "muted"
                                else -> null
                            }
                            TimelineRow(
                                prayer = pr,
                                time = t.format(TIME_FMT),
                                state = state,
                                note = note,
                                first = index == 0,
                                last = index == cols.size - 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (!canExact) {
                    WarningCard(
                        "Exact alarms are off — the azan may be delayed.",
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
                        "Battery optimization may delay alarms. Exempt this app for a reliable azan.",
                        "Fix in Settings", onOpenSettings
                    )
                }

                // ---- Timetable scanning ----
                val scannedRange = remember(dataVersion) { store.dateRange() }
                val todayScanned = scannedRange != null &&
                    !today.isBefore(scannedRange.first) && !today.isAfter(scannedRange.second)
                GlassCard(contentPadding = 18.dp) {
                    Text("Monthly timetable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (todayScanned)
                            "Using the scanned timetable until ${scannedRange!!.second.format(DATE_FMT)}. " +
                                "Other days are calculated automatically."
                        else
                            "Times are calculated on this phone with the mosque's method. " +
                                "Scan a timetable to use its exact times.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sky.TextSoft
                    )
                    Spacer(Modifier.height(14.dp))
                    GlassButton(onClick = onScanGallery, modifier = Modifier.fillMaxWidth(), primary = true) {
                        Text("Scan the timetable from gallery", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    GlassButton(onClick = onScanCamera, modifier = Modifier.fillMaxWidth()) {
                        Text("Photograph the timetable")
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun CountdownRing(
    progress: Float,
    prayer: Prayer?,
    at: LocalDateTime?,
    remaining: Duration?,
    note: String?,
) {
    val p = LocalSkyPalette.current
    Box(Modifier.size(272.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2 + 10.dp.toPx()
            val arc = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val radius = arc.width / 2

            drawCircle(Color.White.copy(alpha = 0.06f), radius = radius - stroke)
            drawArc(
                Color.White.copy(alpha = 0.16f), -90f, 360f, false, topLeft, arc,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            val sweep = 360f * progress
            if (sweep > 0.5f) {
                // soft glow underneath, then the crisp arc
                drawArc(
                    p.accent.copy(alpha = 0.22f), -90f, sweep, false, topLeft, arc,
                    style = Stroke(stroke * 2.2f, cap = StrokeCap.Round)
                )
                drawArc(
                    p.accent, -90f, sweep, false, topLeft, arc,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                val a = Math.toRadians((sweep - 90f).toDouble())
                val head = Offset(
                    center.x + radius * cos(a).toFloat(),
                    center.y + radius * sin(a).toFloat()
                )
                drawCircle(Color.White, radius = stroke * 0.62f, center = head)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "NEXT PRAYER",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = Sky.TextSoft
            )
            Spacer(Modifier.height(6.dp))
            if (prayer == null || at == null || remaining == null) {
                Text("—", style = MaterialTheme.typography.headlineMedium)
            } else {
                Text(prayer.arabic, style = MaterialTheme.typography.titleLarge, color = p.accent)
                Text(
                    prayer.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatCountdown(remaining),
                    style = Tabular.copy(fontSize = 40.sp, fontWeight = FontWeight.Light),
                    color = Color.White
                )
                Text(
                    "at ${at.toLocalTime().format(TIME_FMT)}" +
                        if (at.toLocalDate() != LocalDate.now()) " · tomorrow" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sky.TextSoft
                )
            }
            note?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = Sky.OnAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(p.accent)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    prayer: Prayer,
    time: String,
    state: RowState,
    note: String?,
    first: Boolean,
    last: Boolean,
) {
    val p = LocalSkyPalette.current
    val textAlpha = if (state == RowState.PAST) 0.55f else 1f
    val rowBg = if (state == RowState.NEXT) p.accent.copy(alpha = 0.18f) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rail with marker
        Box(Modifier.width(26.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width / 2
                val line = Color.White.copy(alpha = 0.18f)
                val w = 2.dp.toPx()
                if (!first) drawLine(line, Offset(x, 0f), Offset(x, size.height / 2), w)
                if (!last) drawLine(line, Offset(x, size.height / 2), Offset(x, size.height), w)
            }
            when (state) {
                RowState.PAST -> Box(
                    Modifier.size(18.dp).clip(CircleShape).background(p.accent.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Passed", tint = Sky.OnAccent, modifier = Modifier.size(12.dp))
                }
                RowState.NEXT -> Box(
                    Modifier.size(18.dp).clip(CircleShape).background(p.accent.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(p.accent))
                }
                RowState.FUTURE -> Box(
                    Modifier.size(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f))
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    prayer.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (state == RowState.NEXT) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White.copy(alpha = textAlpha)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    prayer.arabic,
                    style = MaterialTheme.typography.bodySmall,
                    color = (if (state == RowState.NEXT) p.accent else Sky.TextFaint)
                )
            }
            note?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Sky.TextFaint)
            }
        }
        Text(
            time,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = if (state == RowState.NEXT) FontWeight.Bold else FontWeight.Normal,
            color = if (state == RowState.NEXT) p.accent else Color.White.copy(alpha = textAlpha)
        )
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun WarningCard(text: String, actionLabel: String, onAction: () -> Unit) {
    val p = LocalSkyPalette.current
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
            Text(actionLabel, color = p.accent, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** The last azan at or before [now] and the first one after it. */
private fun neighbours(
    all: Map<LocalDate, DayTimes>,
    now: LocalDateTime,
    include: (Prayer) -> Boolean,
): Pair<Pair<Prayer, LocalDateTime>?, Pair<Prayer, LocalDateTime>?> {
    var prev: Pair<Prayer, LocalDateTime>? = null
    var next: Pair<Prayer, LocalDateTime>? = null
    for ((date, day) in all) {
        for (p in Prayer.azanPrayers) {
            if (!include(p)) continue
            val t = day.time(p) ?: continue
            val dt = LocalDateTime.of(date, t)
            if (!dt.isAfter(now)) {
                if (prev == null || dt.isAfter(prev.second)) prev = p to dt
            } else if (next == null || dt.isBefore(next.second)) {
                next = p to dt
            }
        }
    }
    return prev to next
}

private fun formatCountdown(d: Duration): String {
    val total = d.seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
