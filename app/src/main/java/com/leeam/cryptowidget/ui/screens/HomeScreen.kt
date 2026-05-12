package com.leeam.cryptowidget.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.ui.ParticleField
import com.leeam.cryptowidget.ui.components.CryptoCard
import com.leeam.cryptowidget.ui.components.CryptoFilterChip
import com.leeam.cryptowidget.ui.components.SectionLabel
import com.leeam.cryptowidget.ui.nav.Screen
import com.leeam.cryptowidget.ui.settings.CoinSnapshot
import com.leeam.cryptowidget.ui.settings.SettingsViewModel
import com.leeam.cryptowidget.ui.theme.*
import com.leeam.cryptowidget.ui.util.CoinFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    vm: SettingsViewModel,
    navController: NavController
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var debugVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        ParticleField(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HomeHeader(onLongPress = { debugVisible = !debugVisible })

            AnimatedVisibility(
                visible = debugVisible,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                DebugSection(
                    vm        = vm,
                    onDismiss = { debugVisible = false }
                )
            }

            // ── Live multi-coin tracker ─────────────────────────────────────
            LiveTrackerSection(
                snapshots     = state.followedSnapshots,
                isRefreshing  = state.isRefreshingAll,
                onRefresh     = vm::refreshAllFollowed,
                onCoinClick   = { coinId -> navController.navigate(Screen.coinDetail(coinId)) },
                onAddCoin     = { navController.navigate(Screen.ADD_COIN) }
            )

            // ── Portfolio quick link ────────────────────────────────────────
            PortfolioLinkCard(
                followedCount = state.followedCoinIds.size,
                onClick       = { navController.navigate(Screen.PORTFOLIO) }
            )

            // ── Coin browser ────────────────────────────────────────────────
            CoinBrowserCard(
                followedIds  = state.followedCoinIds,
                customCoins  = state.customCoins,
                onToggle     = vm::toggleFollow,
                onDeleteCoin = vm::deleteCustomCoin,
                onAddCoin    = { navController.navigate(Screen.ADD_COIN) }
            )

            // ── Widget tabs ────────────────────────────────────────────────
            WidgetTabsCard(state = state, onToggle = vm::toggleWidgetCoin)

            // ── Settings navigation ────────────────────────────────────────
            Spacer(Modifier.height(2.dp))
            SectionLabel("SETTINGS")
            Spacer(Modifier.height(2.dp))

            NavLinkCard(
                title    = "Widget Settings",
                subtitle = "Refresh interval, sparkline, and chart style",
                onClick  = { navController.navigate(Screen.WIDGET_SETTINGS) }
            )
            NavLinkCard(
                title    = "Appearance",
                subtitle = "Theme and accent colors",
                onClick  = { navController.navigate(Screen.APPEARANCE) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeHeader(onLongPress: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.combinedClickable(
            onClick     = {},
            onLongClick = onLongPress
        )
    ) {
        Text(
            "Coinflow",
            color      = TextPrimary,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Text(
            "Multi-coin price tracker",
            color    = TextSecondary,
            fontSize = 13.sp
        )
    }
}

// ── Hidden debug section (long-press header to reveal) ─────────────────────

@Composable
private fun DebugSection(vm: SettingsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val accent  = LocalThemeColors.current.accent
    var dump by remember { mutableStateOf("Loading diagnostics…") }
    var lastCopiedAt by remember { mutableStateOf(0L) }

    // Build the dump whenever the section becomes visible, and after every refresh.
    LaunchedEffect(Unit) {
        dump = vm.diagnosticsDump()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgDark.copy(alpha = 0.92f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "DEBUG  ·  internal",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Hide", color = TextSecondary, fontSize = 11.sp)
            }
        }

        // Scrollable mono dump
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Surface)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                dump,
                color = TextPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch { dump = vm.diagnosticsDump() }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                border = BorderStroke(1.dp, accent),
                shape  = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Reload", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = {
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("Coinflow diagnostics", dump))
                    lastCopiedAt = System.currentTimeMillis()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                border = BorderStroke(1.dp, accent),
                shape  = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (System.currentTimeMillis() - lastCopiedAt < 2000L) "Copied!" else "Copy",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            OutlinedButton(
                onClick = {
                    vm.refreshAllFollowed()
                    scope.launch { dump = vm.diagnosticsDump() }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorUp),
                border = BorderStroke(1.dp, ColorUp.copy(alpha = 0.7f)),
                shape  = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Force refresh", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ColorUp)
            }
        }
    }
}

// ── Live tracker ──────────────────────────────────────────────────────────

@Composable
private fun LiveTrackerSection(
    snapshots: List<CoinSnapshot>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCoinClick: (String) -> Unit,
    onAddCoin: () -> Unit
) {
    val mostRecentMs = snapshots.maxOfOrNull { it.updatedMs }?.takeIf { it > 0L }
    val timeLabel = mostRecentMs?.let { formatRelativeTime(it) }

    CryptoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("LIVE")
            Spacer(Modifier.width(8.dp))
            if (timeLabel != null) {
                Text(
                    "· $timeLabel",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.weight(1f))
            CompactRefreshButton(isSpinning = isRefreshing, onClick = onRefresh)
        }
        Spacer(Modifier.height(12.dp))
        when {
            snapshots.isEmpty() -> {
                EmptyTrackerHint(onAddCoin = onAddCoin)
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshots.forEach { snap ->
                        LiveCoinRow(
                            snap    = snap,
                            onClick = { onCoinClick(snap.coin.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCoinRow(snap: CoinSnapshot, onClick: () -> Unit) {
    val themeColors = LocalThemeColors.current
    val hasPrice    = snap.priceUsd > 0.0
    val isFetching  = !hasPrice && snap.updatedMs == 0L
    val isUp        = snap.change24hPct >= 0.0
    val changeColor = if (isUp) ColorUp else ColorDown
    val arrow       = if (isUp) "▲" else "▼"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface.copy(alpha = 0.85f))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Symbol badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            themeColors.accent.copy(alpha = 0.25f),
                            themeColors.secondary.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(1.dp, themeColors.accent.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                snap.coin.symbol.take(4),
                color      = TextPrimary,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))

        // Identity
        Column(modifier = Modifier.width(IntrinsicSize.Min)) {
            Text(
                snap.coin.symbol,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                snap.coin.displayName,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.width(10.dp))

        // Mini sparkline (flexes between identity and price)
        MiniSparkline(
            prices  = snap.sparkline,
            isUp    = isUp,
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
        )

        Spacer(Modifier.width(10.dp))

        // Price + change
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when {
                    hasPrice   -> CoinFormatter.formatPrice(snap.priceUsd)
                    isFetching -> "Fetching…"
                    else       -> "—"
                },
                color = if (isFetching) themeColors.accent else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    hasPrice   -> "$arrow ${String.format(Locale.US, "%.2f", abs(snap.change24hPct))}%"
                    isFetching -> "Pulling live data"
                    else       -> "—"
                },
                color    = when {
                    hasPrice -> changeColor
                    else     -> TextSecondary
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MiniSparkline(prices: List<Double>, isUp: Boolean, modifier: Modifier = Modifier) {
    if (prices.size < 2) {
        Box(modifier) { /* empty placeholder */ }
        return
    }
    val color = if (isUp) ColorUp else ColorDown
    val minP  = prices.min()
    val maxP  = prices.max()
    val range = (maxP - minP).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier) {
        val n     = prices.size
        val stepX = size.width / (n - 1)
        val padY  = 2f
        fun y(p: Double) = padY + ((maxP - p) / range * (size.height - 2 * padY)).toFloat()

        for (i in 0 until n - 1) {
            val segUp = prices[i + 1] >= prices[i]
            val segColor = if (segUp) ColorUp else ColorDown
            drawLine(
                color       = segColor.copy(alpha = 0.25f),
                start       = Offset(i * stepX, y(prices[i])),
                end         = Offset((i + 1) * stepX, y(prices[i + 1])),
                strokeWidth = 4f,
                cap         = StrokeCap.Round
            )
            drawLine(
                color       = segColor,
                start       = Offset(i * stepX, y(prices[i])),
                end         = Offset((i + 1) * stepX, y(prices[i + 1])),
                strokeWidth = 1.5f,
                cap         = StrokeCap.Round
            )
        }
        // endpoint dot
        drawCircle(
            color  = color,
            radius = 2.5f,
            center = Offset((n - 1) * stepX, y(prices.last()))
        )
    }
}

@Composable
private fun EmptyTrackerHint(onAddCoin: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text("Follow a coin to start tracking", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onAddCoin,
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = LocalThemeColors.current.accent),
            border  = BorderStroke(1.dp, LocalThemeColors.current.accent),
            shape   = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add a coin", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Coin browser ──────────────────────────────────────────────────────────

@Composable
private fun CoinBrowserCard(
    followedIds: List<String>,
    customCoins: List<com.leeam.cryptowidget.data.model.CoinDefinition>,
    onToggle: (String) -> Unit,
    onDeleteCoin: (String) -> Unit,
    onAddCoin: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<com.leeam.cryptowidget.data.model.CoinDefinition?>(null) }

    CryptoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("FOLLOW")
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = onAddCoin,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = LocalThemeColors.current.accent
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Add coin",
                    color = LocalThemeColors.current.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "BUILT-IN",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            CoinRegistry.all.forEach { coin ->
                CryptoFilterChip(
                    label    = coin.symbol,
                    selected = coin.id in followedIds,
                    onClick  = { onToggle(coin.id) }
                )
            }
        }
        if (customCoins.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                "CUSTOM  ·  long-press to remove",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                customCoins.forEach { coin ->
                    CustomCoinChip(
                        symbol      = coin.symbol,
                        selected    = coin.id in followedIds,
                        onClick     = { onToggle(coin.id) },
                        onLongClick = { pendingDelete = coin }
                    )
                }
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text("Remove ${toDelete.symbol}?", color = TextPrimary)
            },
            text = {
                Text(
                    "Deletes the custom coin definition and removes it from your followed and widget lists.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCoin(toDelete.id)
                    pendingDelete = null
                }) {
                    Text("Remove", color = ColorDown, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = LocalThemeColors.current.accent)
                }
            },
            containerColor = Surface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CustomCoinChip(
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    val bg          = if (selected) themeColors.accent else Surface
    val labelColor  = if (selected) BgDark else TextSecondary
    val borderColor = if (selected) themeColors.accent else themeColors.secondary.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            symbol,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Widget tabs ───────────────────────────────────────────────────────────

@Composable
private fun WidgetTabsCard(
    state: com.leeam.cryptowidget.ui.settings.SettingsUiState,
    onToggle: (String) -> Unit
) {
    CryptoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("WIDGET TABS")
            Spacer(Modifier.weight(1f))
            Text(
                "${state.widgetCoinIds.size} / 5",
                color    = if (state.widgetCoinIds.size >= 5) ColorDown else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick up to 5 followed coins for the home-screen widget tab strip.",
            color = TextSecondary,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))
        if (state.followedCoinIds.isEmpty()) {
            Text(
                "Follow at least one coin first.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                state.followedCoinIds.forEach { coinId ->
                    val coin     = CoinRegistry.byId(coinId)
                    val onWidget = coinId in state.widgetCoinIds
                    val canAdd   = !onWidget && state.widgetCoinIds.size < 5
                    CryptoFilterChip(
                        label    = coin.symbol,
                        selected = onWidget,
                        onClick  = { if (onWidget || canAdd) onToggle(coinId) }
                    )
                }
            }
            AnimatedVisibility(
                visible = state.widgetCoinIds.size >= 5,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Widget tab limit reached (5 max).",
                        color = ColorDown,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ── Shared cards ──────────────────────────────────────────────────────────

@Composable
private fun PortfolioLinkCard(followedCount: Int, onClick: () -> Unit) {
    val themeColors = LocalThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        themeColors.secondary.copy(alpha = 0.22f),
                        themeColors.accent.copy(alpha = 0.18f)
                    )
                )
            )
            .border(1.dp, themeColors.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Portfolio",
                color      = TextPrimary,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (followedCount == 0) "Follow coins to start tracking value"
                else "Total value, 24h delta, and allocation across $followedCount coin${if (followedCount == 1) "" else "s"}",
                color    = TextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint     = themeColors.accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun NavLinkCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface.copy(alpha = 0.85f))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color      = TextPrimary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint     = themeColors.accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CompactRefreshButton(isSpinning: Boolean, onClick: () -> Unit) {
    val accent = LocalThemeColors.current.accent
    val spinTransition = rememberInfiniteTransition(label = "home-spin")
    val angle by spinTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "home-spin-angle"
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Refresh",
            tint     = accent,
            modifier = Modifier
                .size(16.dp)
                .rotate(if (isSpinning) angle else 0f)
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

private fun formatRelativeTime(ms: Long): String {
    val deltaSec = (System.currentTimeMillis() - ms) / 1000
    return when {
        deltaSec < 60       -> "just now"
        deltaSec < 3600     -> "${deltaSec / 60}m ago"
        deltaSec < 86_400   -> "${deltaSec / 3600}h ago"
        else                -> SimpleDateFormat("MMM d", Locale.US).format(Date(ms))
    }
}

