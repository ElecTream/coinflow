package com.leeam.cryptowidget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark        = Color(0xFF0D0D1A)
val AccentCyan    = Color(0xFF00D4FF)
val AccentPurple  = Color(0xFF7B2FFF)
val ColorUp       = Color(0xFF00FF88)
val ColorDown     = Color(0xFFFF4466)
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8899BB)
val Surface       = Color(0xFF141428)
val CardBorder    = Color(0xFF1E1E3C)

private val CryptoColorScheme = darkColorScheme(
    primary          = AccentCyan,
    onPrimary        = BgDark,
    secondary        = AccentPurple,
    onSecondary      = TextPrimary,
    background       = BgDark,
    onBackground     = TextPrimary,
    surface          = Surface,
    onSurface        = TextPrimary,
    error            = ColorDown,
    onError          = TextPrimary
)

@Composable
fun CryptoWidgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CryptoColorScheme,
        content     = content
    )
}
