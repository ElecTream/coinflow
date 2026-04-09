package com.leeam.cryptowidget.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.leeam.cryptowidget.data.local.AppTheme

data class ThemeColors(
    val accent: Color,
    val secondary: Color,
    /** Pre-computed ARGB int for RemoteViews (widget) color operations. */
    val accentArgb: Int,
    val secondaryArgb: Int
)

// ── Presets ──────────────────────────────────────────────────────────────────

val CyberColors = ThemeColors(
    accent       = Color(0xFF00D4FF),
    secondary    = Color(0xFF7B2FFF),
    accentArgb   = 0xFF00D4FF.toInt(),
    secondaryArgb = 0xFF7B2FFF.toInt()
)

val AmberColors = ThemeColors(
    accent        = Color(0xFFFFB300),
    secondary     = Color(0xFFFF1744),
    accentArgb    = 0xFFFFB300.toInt(),
    secondaryArgb = 0xFFFF1744.toInt()
)

val MatrixColors = ThemeColors(
    accent        = Color(0xFF00FF41),
    secondary     = Color(0xFFFF6D00),
    accentArgb    = 0xFF00FF41.toInt(),
    secondaryArgb = 0xFFFF6D00.toInt()
)

val MidnightColors = ThemeColors(
    accent        = Color(0xFFC77DFF),
    secondary     = Color(0xFF00E5FF),
    accentArgb    = 0xFFC77DFF.toInt(),
    secondaryArgb = 0xFF00E5FF.toInt()
)

fun AppTheme.toThemeColors(
    customAccentArgb: Int = 0xFF00D4FF.toInt(),
    customSecondaryArgb: Int = 0xFF7B2FFF.toInt()
): ThemeColors = when (this) {
    AppTheme.CYBER    -> CyberColors
    AppTheme.AMBER    -> AmberColors
    AppTheme.MATRIX   -> MatrixColors
    AppTheme.MIDNIGHT -> MidnightColors
    AppTheme.CUSTOM   -> ThemeColors(
        accent        = Color(customAccentArgb),
        secondary     = Color(customSecondaryArgb),
        accentArgb    = customAccentArgb,
        secondaryArgb = customSecondaryArgb
    )
}

// ── CompositionLocal ─────────────────────────────────────────────────────────

val LocalThemeColors = staticCompositionLocalOf { CyberColors }
