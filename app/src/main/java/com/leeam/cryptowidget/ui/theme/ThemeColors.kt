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
    accent       = Color(0xFFFFB300),
    secondary    = Color(0xFFFF6B00),
    accentArgb   = 0xFFFFB300.toInt(),
    secondaryArgb = 0xFFFF6B00.toInt()
)

val MatrixColors = ThemeColors(
    accent       = Color(0xFF00FF41),
    secondary    = Color(0xFF00CC33),
    accentArgb   = 0xFF00FF41.toInt(),
    secondaryArgb = 0xFF00CC33.toInt()
)

val MidnightColors = ThemeColors(
    accent       = Color(0xFFC77DFF),
    secondary    = Color(0xFFE040FB),
    accentArgb   = 0xFFC77DFF.toInt(),
    secondaryArgb = 0xFFE040FB.toInt()
)

fun AppTheme.toThemeColors(): ThemeColors = when (this) {
    AppTheme.CYBER    -> CyberColors
    AppTheme.AMBER    -> AmberColors
    AppTheme.MATRIX   -> MatrixColors
    AppTheme.MIDNIGHT -> MidnightColors
}

// ── CompositionLocal ─────────────────────────────────────────────────────────

val LocalThemeColors = staticCompositionLocalOf { CyberColors }
