package com.ulm.azan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ulm.azan.data.DayPhase

/** Fixed colours shared by every sky. */
object Sky {
    val Gold = Color(0xFFF4C977)
    val Text = Color.White
    val TextSoft = Color(0xC7FFFFFF)
    val TextFaint = Color(0x8CFFFFFF)
    val NightBase = Color(0xFF070B1F)
    val OnAccent = Color(0xFF1E1404)
    val Danger = Color(0xFFFFB4AB)
}

/** Gradient + glass colours for one part of the day. */
data class SkyPalette(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val accent: Color,
    val glass: Color,
    val glassBorder: Color,
    val stars: Boolean,
)

fun DayPhase.palette(): SkyPalette = when (this) {
    DayPhase.NIGHT -> SkyPalette(
        top = Color(0xFF050816), mid = Color(0xFF101A44), bottom = Color(0xFF2A2F72),
        accent = Color(0xFFF4C977), glass = Color(0x1AFFFFFF), glassBorder = Color(0x29FFFFFF),
        stars = true
    )
    DayPhase.DAWN -> SkyPalette(
        top = Color(0xFF1B1F4E), mid = Color(0xFF5B3F86), bottom = Color(0xFFC97A86),
        accent = Color(0xFFFFD3B5), glass = Color(0x1FFFFFFF), glassBorder = Color(0x33FFFFFF),
        stars = true
    )
    DayPhase.DAY -> SkyPalette(
        top = Color(0xFF0A3F8C), mid = Color(0xFF1F6FC9), bottom = Color(0xFF5AA2E3),
        accent = Color(0xFFFFE9A8), glass = Color(0x2A06285A), glassBorder = Color(0x38FFFFFF),
        stars = false
    )
    DayPhase.AFTERNOON -> SkyPalette(
        top = Color(0xFF6E3410), mid = Color(0xFFB0611C), bottom = Color(0xFFD98F3E),
        accent = Color(0xFFFFE2A6), glass = Color(0x2E3A1600), glassBorder = Color(0x38FFFFFF),
        stars = false
    )
    DayPhase.SUNSET -> SkyPalette(
        top = Color(0xFF2A1745), mid = Color(0xFF8C2F4A), bottom = Color(0xFFE0703A),
        accent = Color(0xFFFFC98A), glass = Color(0x29200A20), glassBorder = Color(0x33FFFFFF),
        stars = false
    )
}

/** The palette of the sky currently behind the content. */
val LocalSkyPalette = staticCompositionLocalOf { DayPhase.NIGHT.palette() }

private val SkyColors = darkColorScheme(
    primary = Sky.Gold,
    onPrimary = Sky.OnAccent,
    primaryContainer = Color(0x33FFFFFF),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFBFD4FF),
    onSecondary = Color(0xFF0B1A3A),
    secondaryContainer = Color(0x29FFFFFF),
    onSecondaryContainer = Color.White,
    background = Sky.NightBase,
    onBackground = Color.White,
    surface = Color(0xFF151C45),
    onSurface = Color.White,
    surfaceVariant = Color(0x1FFFFFFF),
    onSurfaceVariant = Sky.TextSoft,
    outline = Color(0x73FFFFFF),
    outlineVariant = Color(0x33FFFFFF),
    error = Sky.Danger,
    onError = Color(0xFF690005),
    errorContainer = Color(0x66B3261E),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** The whole app sits on a sky, so it always uses the light-on-dark scheme. */
@Composable
fun AzanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SkyColors, content = content)
}
