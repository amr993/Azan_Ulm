package com.ulm.azan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ulm.azan.R
import com.ulm.azan.data.DayPhase
import com.ulm.azan.data.Prayer
import com.ulm.azan.data.Times
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.random.Random

/** Arabic name of each prayer, shown under the English one. */
val Prayer.arabic: String
    get() = when (this) {
        Prayer.FAJR -> "الفجر"
        Prayer.SUNRISE -> "الشروق"
        Prayer.DHUHR -> "الظهر"
        Prayer.ASR -> "العصر"
        Prayer.MAGHRIB -> "المغرب"
        Prayer.ISHA -> "العشاء"
    }

/**
 * Current time that refreshes every [periodMs] — but only while the screen is
 * visible, so the app does no work at all in the background.
 */
@Composable
fun rememberTicker(periodMs: Long): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, periodMs) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = LocalDateTime.now()
                delay(periodMs)
            }
        }
    }
    return now
}

/** Day phase for screens that don't otherwise need prayer times. */
@Composable
fun rememberSkyPhase(): DayPhase {
    val context = LocalContext.current
    val now = rememberTicker(30_000)
    val date = now.toLocalDate()
    val day = remember(date) { Times.forDate(context, date) }
    return DayPhase.of(day, now.toLocalTime())
}

private val HIJRI_MONTHS = listOf(
    "Muharram", "Safar", "Rabiʿ al-Awwal", "Rabiʿ al-Thani", "Jumada al-Ula",
    "Jumada al-Akhirah", "Rajab", "Shaʿban", "Ramadan", "Shawwal",
    "Dhu al-Qaʿdah", "Dhu al-Hijjah"
)

/** e.g. "28 Rabiʿ al-Thani 1448" (Umm al-Qura calendar, may differ locally by a day). */
fun hijriLabel(date: LocalDate): String? = try {
    val h = HijrahDate.from(date)
    "${h.get(ChronoField.DAY_OF_MONTH)} ${HIJRI_MONTHS[h.get(ChronoField.MONTH_OF_YEAR) - 1]} " +
        "${h.get(ChronoField.YEAR)}"
} catch (_: Exception) {
    null
}

private data class Star(val x: Float, val y: Float, val r: Float, val a: Float)

/** Full-screen sky gradient that fades smoothly when the phase changes. */
@Composable
fun SkyBackground(phase: DayPhase, content: @Composable BoxScope.() -> Unit) {
    val p = phase.palette()
    val anim = tween<Color>(1200)
    val top by animateColorAsState(p.top, anim, label = "skyTop")
    val mid by animateColorAsState(p.mid, anim, label = "skyMid")
    val bottom by animateColorAsState(p.bottom, anim, label = "skyBottom")
    val glow by animateColorAsState(p.accent, anim, label = "skyGlow")
    val stars = remember {
        val rnd = Random(7)
        List(70) { Star(rnd.nextFloat(), rnd.nextFloat() * 0.6f, 0.5f + rnd.nextFloat() * 1.3f, 0.25f + rnd.nextFloat() * 0.7f) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, mid, bottom)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Soft sun / moon glow in the upper corner.
            val c = Offset(size.width * 0.85f, size.height * 0.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.38f), Color.Transparent),
                    center = c, radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f, center = c
            )
            if (p.stars) {
                for (s in stars) {
                    drawCircle(
                        Color.White.copy(alpha = s.a * (if (phase == DayPhase.DAWN) 0.5f else 1f)),
                        radius = s.r * density,
                        center = Offset(s.x * size.width, s.y * size.height)
                    )
                }
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            LocalSkyPalette provides p
        ) { content() }
    }
}

/** Translucent "frosted glass" card. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalSkyPalette.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(p.glass)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))))
            .border(1.dp, if (highlight) p.accent.copy(alpha = 0.7f) else p.glassBorder, shape)
            .padding(contentPadding),
        content = content
    )
}

/** Pill button: [primary] is filled with the sky accent, otherwise glass. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    primary: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val p = LocalSkyPalette.current
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) p.accent else Color.White.copy(alpha = 0.14f),
            contentColor = if (primary) Sky.OnAccent else Color.White
        ),
        border = if (primary) null else BorderStroke(1.dp, p.glassBorder),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
fun skySwitchColors(): SwitchColors {
    val p = LocalSkyPalette.current
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = p.accent.copy(alpha = 0.9f),
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
        uncheckedTrackColor = Color.White.copy(alpha = 0.14f),
        uncheckedBorderColor = Color.White.copy(alpha = 0.45f),
    )
}

/** Thin translucent rule. */
@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.14f)))
}

/** Transparent top bar sitting on the sky, under the status bar. */
@Composable
fun SkyTopBar(
    title: String,
    arabic: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val p = LocalSkyPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = if (onBack != null) 6.dp else 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
        } else {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_crescent_star),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(p.accent)
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (arabic != null) {
                Text(arabic, style = MaterialTheme.typography.bodyMedium, color = p.accent)
            }
        }
        actions()
    }
}
