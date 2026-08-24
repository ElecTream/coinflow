package com.electream.cryptowidget.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.electream.cryptowidget.data.local.AppTheme
import com.electream.cryptowidget.ui.components.CryptoCard
import com.electream.cryptowidget.ui.components.SectionLabel
import com.electream.cryptowidget.ui.settings.SettingsViewModel
import com.electream.cryptowidget.ui.theme.*

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun hueToArgb(hue: Float): Int =
    AndroidColor.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), 1f, 1f))

private fun argbToHue(argb: Int): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(argb, hsv)
    return hsv[0]
}

private fun argbToHex(argb: Int): String = String.format("%06X", argb and 0xFFFFFF)

private fun hexToArgb(hex: String): Int? =
    if (hex.length == 6) try { (0xFF000000.toInt()) or hex.toInt(16) } catch (_: Exception) { null } else null

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LocalThemeColors.current.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Theme presets ───────────────────────────────────────────────
            CryptoCard {
                SectionLabel("THEME")
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        Triple(AppTheme.CYBER,    "Cyber",    CyberColors),
                        Triple(AppTheme.AMBER,    "Amber",    AmberColors),
                        Triple(AppTheme.MATRIX,   "Matrix",   MatrixColors),
                        Triple(AppTheme.MIDNIGHT, "Midnight", MidnightColors),
                    ).forEach { (theme, label, colors) ->
                        ThemeSwatch(
                            label     = label,
                            accent    = colors.accent,
                            secondary = colors.secondary,
                            selected  = state.appTheme == theme,
                            onClick   = { vm.onThemeChange(theme) }
                        )
                    }
                    // Custom swatch — preview current custom colors
                    ThemeSwatch(
                        label     = "Custom",
                        accent    = Color(state.customAccentArgb),
                        secondary = Color(state.customSecondaryArgb),
                        selected  = state.appTheme == AppTheme.CUSTOM,
                        onClick   = { vm.onThemeChange(AppTheme.CUSTOM) }
                    )
                }
            }

            // ── Custom color editor (only visible when CUSTOM theme selected) ─
            AnimatedVisibility(
                visible = state.appTheme == AppTheme.CUSTOM,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                CryptoCard {
                    SectionLabel("CUSTOM COLORS")
                    Spacer(Modifier.height(12.dp))
                    ColorPickerRow(
                        label   = "ACCENT",
                        argb    = state.customAccentArgb,
                        onChange = vm::onCustomAccentChange
                    )
                    Spacer(Modifier.height(16.dp))
                    ColorPickerRow(
                        label   = "SECONDARY",
                        argb    = state.customSecondaryArgb,
                        onChange = vm::onCustomSecondaryChange
                    )
                }
            }
        }
    }
}

// ── Color picker row ──────────────────────────────────────────────────────────

@Composable
private fun ColorPickerRow(
    label: String,
    argb: Int,
    onChange: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current

    // Derived local state for the hex field (kept in sync with argb)
    var hexText by remember(argb) { mutableStateOf(argbToHex(argb)) }
    var hexError by remember { mutableStateOf(false) }

    val currentHue = remember(argb) { argbToHue(argb) }

    val rainbowColors = remember {
        (0..12).map { i -> Color(hueToArgb(i * 30f)) }
    }
    val rainbowBrush = Brush.horizontalGradient(rainbowColors)

    Column {
        Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Color preview swatch
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(argb))
                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(10.dp))

            // HEX input
            OutlinedTextField(
                value         = hexText,
                onValueChange = { raw ->
                    val upper = raw.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                    hexText   = upper
                    hexError  = false
                    val parsed = hexToArgb(upper)
                    if (parsed != null) onChange(parsed) else hexError = upper.length == 6
                },
                modifier      = Modifier.width(120.dp),
                singleLine    = true,
                isError       = hexError,
                prefix        = { Text("#", color = TextSecondary, fontSize = 13.sp) },
                placeholder   = { Text("RRGGBB", color = TextSecondary, fontSize = 12.sp) },
                textStyle     = LocalTextStyle.current.copy(
                    color    = TextPrimary,
                    fontSize = 13.sp
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType   = KeyboardType.Ascii,
                    imeAction      = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = LocalThemeColors.current.accent,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = LocalThemeColors.current.accent,
                    errorBorderColor     = ColorDown
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        // Hue slider with rainbow track
        Text("Hue", color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            // Rainbow background behind the slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
                    .background(rainbowBrush)
            )
            Slider(
                value         = currentHue,
                onValueChange = { hue ->
                    val newArgb = hueToArgb(hue)
                    hexText = argbToHex(newArgb)
                    hexError = false
                    onChange(newArgb)
                },
                valueRange    = 0f..360f,
                modifier      = Modifier.fillMaxWidth(),
                colors        = SliderDefaults.colors(
                    thumbColor            = Color(argb),
                    activeTrackColor      = Color.Transparent,
                    inactiveTrackColor    = Color.Transparent,
                    activeTickColor       = Color.Transparent,
                    inactiveTickColor     = Color.Transparent
                )
            )
        }
    }
}

// ── Theme swatch ──────────────────────────────────────────────────────────────

@Composable
private fun ThemeSwatch(
    label: String,
    accent: Color,
    secondary: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(accent, secondary)))
                .border(
                    width = if (selected) 2.dp else 0.5.dp,
                    color = if (selected) TextPrimary else CardBorder,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text("✓", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (selected) TextPrimary else TextSecondary, fontSize = 10.sp)
    }
}
