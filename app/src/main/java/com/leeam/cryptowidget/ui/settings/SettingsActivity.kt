package com.leeam.cryptowidget.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.data.local.AlertDirection
import com.leeam.cryptowidget.data.local.AlertEntity
import com.leeam.cryptowidget.data.local.AlertMode
import com.leeam.cryptowidget.data.local.AppTheme
import com.leeam.cryptowidget.data.local.ChartStyle
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.model.WalletType
import com.leeam.cryptowidget.ui.ParticleField
import com.leeam.cryptowidget.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val vm: SettingsViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — snackbar handled in compose */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prompt for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            CryptoWidgetTheme(themeColors = state.appTheme.toThemeColors()) {
                SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: SettingsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val saveSuccessMsg = stringResource(R.string.save_success)
    val errorPrefix = stringResource(R.string.error_prefix, "")

    LaunchedEffect(state.saveSuccess, state.saveError) {
        when {
            state.saveSuccess -> {
                snackbar.showSnackbar(saveSuccessMsg)
                vm.clearFeedback()
            }
            state.saveError != null -> {
                snackbar.showSnackbar("${errorPrefix}${state.saveError}")
                vm.clearFeedback()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
        ) {
            // Persistent particle field behind all content
            ParticleField(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Text(stringResource(R.string.app_title), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.xrp_price_tracker), color = TextSecondary, fontSize = 13.sp)

                // ── Theme picker card ─────────────────────────────────────────
                CryptoCard {
                    SectionLabel("THEME")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        val themes = listOf(
                            AppTheme.CYBER    to CyberColors,
                            AppTheme.AMBER    to AmberColors,
                            AppTheme.MATRIX   to MatrixColors,
                            AppTheme.MIDNIGHT to MidnightColors
                        )
                        val labels = listOf("Cyber", "Amber", "Matrix", "Midnight")
                        themes.forEachIndexed { i, (theme, colors) ->
                            ThemeSwatch(
                                label    = labels[i],
                                accent   = colors.accent,
                                secondary = colors.secondary,
                                selected = state.appTheme == theme,
                                onClick  = { vm.onThemeChange(theme) }
                            )
                        }
                    }
                }

                // ── Coin picker card ──────────────────────────────────────────
                CryptoCard {
                    SectionLabel("COIN")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoinRegistry.all.forEach { coin ->
                            CryptoFilterChip(
                                label    = coin.symbol,
                                selected = state.coinId == coin.id,
                                onClick  = { vm.onCoinChange(coin.id) }
                            )
                        }
                    }
                }

                // ── Live price card ───────────────────────────────────────────
                CryptoCard {
                    SectionLabel(stringResource(R.string.label_live_price))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            state.priceLoading -> PriceShimmer(Modifier.weight(1f))
                            state.priceError != null ->
                                Text(stringResource(R.string.error_prefix, state.priceError!!), color = ColorDown, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            state.livePrice != null -> {
                                Column(modifier = Modifier.weight(1f)) {
                                    PriceDisplay(
                                        price    = state.livePrice!!,
                                        change24h = state.change24h
                                    )
                                }
                            }
                        }
                        SpinningRefreshButton(
                            isSpinning = state.priceLoading,
                            onClick    = vm::fetchLivePrice
                        )
                    }
                }

                // ── Wallet card (only for coins with wallet support) ──────────
                AnimatedVisibility(
                    visible = CoinRegistry.byId(state.coinId).walletType != WalletType.NONE,
                    enter = fadeIn() + slideInVertically(),
                    exit  = fadeOut() + slideOutVertically()
                ) {
                    CryptoCard {
                        SectionLabel(stringResource(R.string.label_wallet_address))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.walletAddress,
                            onValueChange = vm::onWalletChange,
                            placeholder = { Text(stringResource(R.string.placeholder_wallet_address), color = TextSecondary, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = LocalThemeColors.current.accent, fontFamily = FontFamily.Monospace, fontSize = 13.sp
                            ),
                            colors = cryptoTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                when {
                                    state.walletTestLoading ->
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LocalThemeColors.current.accent, strokeWidth = 2.dp)
                                    state.walletTestResult != null -> {
                                        val ok = state.walletTestResult!!.startsWith("OK")
                                        Text(state.walletTestResult!!, color = if (ok) ColorUp else ColorDown, fontSize = 11.sp)
                                    }
                                }
                            }
                            CryptoOutlinedButton(stringResource(R.string.btn_test_balance)) { vm.testWallet() }
                        }
                    }
                }

                // ── Refresh interval card ─────────────────────────────────────
                CryptoCard {
                    SectionLabel(stringResource(R.string.label_refresh_interval))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        val intervals = listOf(
                            15 to stringResource(R.string.interval_15m),
                            30 to stringResource(R.string.interval_30m),
                            60 to stringResource(R.string.interval_1h),
                            120 to stringResource(R.string.interval_2h),
                            360 to stringResource(R.string.interval_6h)
                        )
                        intervals.forEach { (min, label) ->
                            CryptoFilterChip(
                                selected = state.refreshIntervalMin == min,
                                label    = label,
                                onClick  = { vm.onIntervalChange(min) }
                            )
                        }
                    }
                }

                // ── Sparkline & chart style card ──────────────────────────────
                CryptoCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_show_sparkline), color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.showSparkline,
                            onCheckedChange = vm::onShowSparklineChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = LocalThemeColors.current.accent,
                                checkedTrackColor   = Surface,
                                checkedBorderColor  = LocalThemeColors.current.accent,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SwitchTrackOff,
                                uncheckedBorderColor = CardBorder
                            )
                        )
                    }
                    AnimatedVisibility(visible = state.showSparkline) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            SectionLabel(stringResource(R.string.label_chart_style))
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(ChartStyle.AREA, ChartStyle.LINE, ChartStyle.CANDLE).forEach { style ->
                                    val label = when (style) {
                                        ChartStyle.LINE   -> stringResource(R.string.chart_style_line)
                                        ChartStyle.CANDLE -> stringResource(R.string.chart_style_candle)
                                        ChartStyle.AREA   -> stringResource(R.string.chart_style_area)
                                    }
                                    CryptoFilterChip(
                                        selected = state.chartStyle == style,
                                        label    = label,
                                        onClick  = { vm.onChartStyleChange(style) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Price alerts card ─────────────────────────────────────────
                CryptoCard {
                    SectionLabel(stringResource(R.string.label_price_alerts))
                    Spacer(Modifier.height(10.dp))

                    // Direction + threshold + add button
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(AlertDirection.ABOVE, AlertDirection.BELOW).forEach { dir ->
                                val label = if (dir == AlertDirection.ABOVE)
                                    stringResource(R.string.alert_direction_above)
                                else
                                    stringResource(R.string.alert_direction_below)
                                FilterChip(
                                    selected = state.newAlertDirection == dir,
                                    onClick  = { vm.onNewAlertDirection(dir) },
                                    label    = { Text(label, fontSize = 11.sp) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LocalThemeColors.current.secondary,
                                        selectedLabelColor     = TextPrimary,
                                        containerColor         = Surface,
                                        labelColor             = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = state.newAlertDirection == dir,
                                        borderColor         = CardBorder,
                                        selectedBorderColor = LocalThemeColors.current.secondary
                                    )
                                )
                            }
                        }
                        OutlinedTextField(
                            value = state.newAlertThreshold,
                            onValueChange = vm::onNewAlertThreshold,
                            placeholder = { Text(stringResource(R.string.placeholder_price), color = TextSecondary, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            singleLine = true,
                            colors = cryptoTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text("\$", color = TextSecondary, fontSize = 12.sp) }
                        )
                        Button(
                            onClick = vm::addAlert,
                            colors = ButtonDefaults.buttonColors(containerColor = LocalThemeColors.current.accent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("+", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Alert mode selector
                    SectionLabel("ALERT MODE")
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            AlertMode.CROSSING  to "Crossing",
                            AlertMode.REPEATING to "Repeating",
                            AlertMode.ONE_SHOT  to "One-shot"
                        ).forEach { (mode, label) ->
                            CryptoFilterChip(
                                selected = state.newAlertMode == mode,
                                label    = label,
                                onClick  = { vm.onNewAlertMode(mode) }
                            )
                        }
                    }

                    AnimatedVisibility(visible = state.newAlertMode == AlertMode.REPEATING) {
                        Column {
                            Spacer(Modifier.height(6.dp))
                            SectionLabel("COOLDOWN")
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf(15 to "15m", 30 to "30m", 60 to "1h", 240 to "4h", 720 to "12h", 1440 to "24h").forEach { (min, label) ->
                                    FilterChip(
                                        selected = state.newAlertCooldownMin == min,
                                        onClick  = { vm.onNewAlertCooldown(min) },
                                        label    = { Text(label, fontSize = 11.sp) },
                                        colors   = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LocalThemeColors.current.secondary,
                                            selectedLabelColor     = TextPrimary,
                                            containerColor         = Surface,
                                            labelColor             = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true, selected = state.newAlertCooldownMin == min,
                                            borderColor = CardBorder, selectedBorderColor = LocalThemeColors.current.secondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (state.newAlertMode) {
                            AlertMode.CROSSING  -> "Fires each time price crosses the threshold. Re-arms automatically."
                            AlertMode.REPEATING -> "Fires repeatedly while condition holds, respecting cooldown."
                            AlertMode.ONE_SHOT  -> "Fires once, then disables. Re-enable manually to reuse."
                        },
                        color = TextSecondary, fontSize = 10.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    if (state.alerts.isEmpty()) {
                        Text(stringResource(R.string.no_alerts_set), color = TextSecondary, fontSize = 12.sp)
                    } else {
                        state.alerts.forEach { alert ->
                            AlertRow(
                                alert    = alert,
                                onToggle = { vm.toggleAlert(alert, it) },
                                onDelete = { vm.deleteAlert(alert) }
                            )
                            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                        }
                    }
                }

                // ── Diagnostics card ──────────────────────────────────────────
                if (state.lastWorkerRunMs > 0L) {
                    CryptoCard {
                        SectionLabel(stringResource(R.string.label_diagnostics))
                        Spacer(Modifier.height(4.dp))
                        val fmt = SimpleDateFormat("HH:mm:ss MMM d", Locale.US)
                        Text(stringResource(R.string.last_update, fmt.format(Date(state.lastWorkerRunMs))), color = TextSecondary, fontSize = 11.sp)
                        if (state.lastErrorMsg != null) {
                            Text(stringResource(R.string.error_prefix, state.lastErrorMsg!!), color = ColorDown, fontSize = 11.sp)
                        } else {
                            Text(stringResource(R.string.status_ok), color = ColorUp, fontSize = 11.sp)
                        }
                    }
                }

            }

            // ── Floating save button ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, BgDark.copy(alpha = 0.97f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = vm::save,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalThemeColors.current.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_save_update), color = BgDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ── Price display with flash + animated digit roll ────────────────────────────

@Composable
private fun PriceDisplay(price: Double, change24h: Double?) {
    val flashAlpha = remember { Animatable(0f) }
    val flashColorState = remember { mutableStateOf(ColorUp) }

    LaunchedEffect(price) {
        flashColorState.value = if ((change24h ?: 0.0) >= 0) ColorUp else ColorDown
        flashAlpha.snapTo(1f)
        flashAlpha.animateTo(0f, animationSpec = tween(900))
    }

    val priceColor = lerp(TextPrimary, flashColorState.value, flashAlpha.value)

    AnimatedContent(
        targetState = price,
        transitionSpec = {
            (slideInVertically { -it } + fadeIn()) togetherWith
                    (slideOutVertically { it } + fadeOut())
        },
        label = "price_digits"
    ) { p ->
        Text(
            "\$${String.format(Locale.US, "%.4f", p)}",
            color = priceColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }

    change24h?.let { change ->
        val upColor = if (change >= 0) ColorUp else ColorDown
        val arrow   = if (change >= 0) "▲" else "▼"
        Text(
            stringResource(R.string.price_change_24h, arrow, abs(change)),
            color = upColor, fontSize = 12.sp
        )
    }
}

// ── Shimmer loading bar ───────────────────────────────────────────────────────

@Composable
private fun PriceShimmer(modifier: Modifier = Modifier) {
    val accent = LocalThemeColors.current.accent
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -400f, targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            accent.copy(alpha = 0.35f),
            Color.Transparent
        ),
        start = Offset(shimmerX, 0f),
        end   = Offset(shimmerX + 400f, 0f)
    )
    Box(
        modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Surface)
            .background(brush)
    )
}

// ── Spinning refresh icon button ──────────────────────────────────────────────

@Composable
private fun SpinningRefreshButton(isSpinning: Boolean, onClick: () -> Unit) {
    val spinTransition = rememberInfiniteTransition(label = "refresh-spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "spin-angle"
    )
    val accent = LocalThemeColors.current.accent

    OutlinedButton(
        onClick = onClick,
        colors  = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        border  = BorderStroke(1.dp, accent),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .rotate(if (isSpinning) spinAngle else 0f),
            colorFilter = ColorFilter.tint(accent)
        )
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.btn_refresh), fontSize = 11.sp)
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

// ── Alert row ─────────────────────────────────────────────────────────────────

@Composable
private fun AlertRow(
    alert: AlertEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = alert.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = LocalThemeColors.current.accent,
                checkedTrackColor    = Surface,
                checkedBorderColor   = LocalThemeColors.current.accent,
                uncheckedThumbColor  = TextSecondary,
                uncheckedTrackColor  = SwitchTrackOff,
                uncheckedBorderColor = CardBorder
            )
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            val direction = if (alert.direction == AlertDirection.ABOVE)
                stringResource(R.string.alert_direction_above)
            else
                stringResource(R.string.alert_direction_below)
            Text(
                "$direction \$${String.format(Locale.US, "%.4f", alert.thresholdUsd)}",
                color = if (alert.isEnabled) TextPrimary else TextSecondary,
                fontSize = 13.sp
            )
            val modeBadge = when (alert.alertMode) {
                AlertMode.CROSSING  -> "crossing"
                AlertMode.REPEATING -> "every ${alert.cooldownMin}m"
                AlertMode.ONE_SHOT  -> "one-shot"
            }
            Text(modeBadge, color = LocalThemeColors.current.accent, fontSize = 10.sp)
        }
        if (alert.firedAtMs != null && alert.alertMode == AlertMode.ONE_SHOT) {
            Text(
                stringResource(R.string.alert_fired),
                color = LocalThemeColors.current.secondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        TextButton(onClick = onDelete, contentPadding = PaddingValues(4.dp)) {
            Text("✕", color = ColorDown, fontSize = 14.sp)
        }
    }
}

// ── Shared composable helpers ─────────────────────────────────────────────────

@Composable
private fun CryptoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)

@Composable
private fun CryptoFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label, fontSize = 12.sp) },
        colors   = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LocalThemeColors.current.accent,
            selectedLabelColor     = BgDark,
            containerColor         = Surface,
            labelColor             = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor         = CardBorder,
            selectedBorderColor = LocalThemeColors.current.accent
        )
    )
}

@Composable
private fun CryptoOutlinedButton(label: String, onClick: () -> Unit) =
    OutlinedButton(
        onClick = onClick,
        colors  = ButtonDefaults.outlinedButtonColors(contentColor = LocalThemeColors.current.accent),
        border  = BorderStroke(1.dp, LocalThemeColors.current.accent),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(label, fontSize = 11.sp) }

@Composable
private fun cryptoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = LocalThemeColors.current.accent,
    unfocusedBorderColor = CardBorder,
    cursorColor          = LocalThemeColors.current.accent,
    focusedLabelColor    = LocalThemeColors.current.accent
)
