package com.leeam.cryptowidget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// ── Structural colors (unchanged across themes) ───────────────────────────────
val BgDark         = Color(0xFF0D0D1A)
val ColorUp        = Color(0xFF00FF88)
val ColorDown      = Color(0xFFFF4466)
val TextPrimary    = Color(0xFFFFFFFF)
val TextSecondary  = Color(0xFF8899BB)
val Surface        = Color(0xFF141428)
val CardBorder     = Color(0xFF1E1E3C)
val SwitchTrackOff = Color(0xFF2A2A44)

// Legacy aliases — kept so existing widget XML / RemoteViews code still compiles.
val AccentCyan    = CyberColors.accent
val AccentPurple  = CyberColors.secondary

// ── Theme ─────────────────────────────────────────────────────────────────────

@Composable
fun CoinflowTheme(
    themeColors: ThemeColors = CyberColors,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary      = themeColors.accent,
        onPrimary    = BgDark,
        secondary    = themeColors.secondary,
        onSecondary  = TextPrimary,
        background   = BgDark,
        onBackground = TextPrimary,
        surface      = Surface,
        onSurface    = TextPrimary,
        error        = ColorDown,
        onError      = TextPrimary
    )
    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
