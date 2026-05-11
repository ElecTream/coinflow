package com.leeam.cryptowidget.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leeam.cryptowidget.ui.theme.*
import com.leeam.cryptowidget.ui.util.CoinFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onBack: () -> Unit,
    onCoinClick: (String) -> Unit = {}
) {
    val vm: PortfolioViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Portfolio", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = themeColors.accent
                        )
                    }
                },
                actions = {
                    PortfolioRefreshButton(
                        isRefreshing = state.isRefreshing,
                        onClick      = vm::refresh
                    )
                    Spacer(Modifier.width(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(padding)
        ) {
            when {
                state.rows.isEmpty() -> PortfolioEmptyState(modifier = Modifier.fillMaxSize())
                else -> PortfolioContent(
                    state        = state,
                    onCoinClick  = onCoinClick
                )
            }
        }
    }
}

@Composable
private fun PortfolioContent(
    state: PortfolioUiState,
    onCoinClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { PortfolioHero(state = state) }

        if (state.rows.any { it.hasWallet }) {
            item { AllocationBar(rows = state.rows) }
        }

        item {
            Text(
                "ASSETS",
                color    = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        items(state.rows, key = { it.coin.id }) { row ->
            PortfolioRowItem(
                row     = row,
                onClick = { onCoinClick(row.coin.id) }
            )
        }

        if (state.refreshError != null) {
            item {
                Text(
                    "Refresh error: ${state.refreshError}",
                    color    = ColorDown,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PortfolioHero(state: PortfolioUiState) {
    val themeColors = LocalThemeColors.current
    val hasValue   = state.totalValueUsd > 0.0
    val isUp       = state.totalChange24hUsd >= 0.0
    val deltaColor = if (isUp) ColorUp else ColorDown
    val arrow      = if (isUp) "▲" else "▼"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        themeColors.secondary.copy(alpha = 0.22f),
                        themeColors.accent.copy(alpha = 0.10f),
                        Surface.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, themeColors.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Text(
            "TOTAL VALUE",
            color    = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasValue) CoinFormatter.formatValueUsd(state.totalValueUsd) else "$0.00",
            color      = TextPrimary,
            fontSize   = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        if (hasValue) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$arrow ${CoinFormatter.formatValueUsd(abs(state.totalChange24hUsd))}",
                    color      = deltaColor,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(deltaColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${if (isUp) "+" else "-"}${String.format(Locale.US, "%.2f", abs(state.totalChange24hPct))}%",
                        color    = deltaColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("24h", color = TextSecondary, fontSize = 11.sp)
            }
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                "Add a wallet address to a followed coin to see its value here.",
                color    = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AllocationBar(rows: List<PortfolioRow>) {
    val themeColors = LocalThemeColors.current
    val walletRows = rows.filter { it.hasWallet }.sortedByDescending { it.valueUsd }
    val total = walletRows.sumOf { it.valueUsd }
    if (total <= 0.0) return

    // Stable color palette per coin slot — uses accent / secondary plus a few derived tints
    val palette = listOf(
        themeColors.accent,
        themeColors.secondary,
        themeColors.accent.copy(alpha = 0.55f),
        themeColors.secondary.copy(alpha = 0.55f),
        themeColors.accent.copy(alpha = 0.35f),
        themeColors.secondary.copy(alpha = 0.35f)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "ALLOCATION",
            color    = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Surface)
        ) {
            walletRows.forEachIndexed { idx, row ->
                val fraction = (row.valueUsd / total).toFloat().coerceIn(0f, 1f)
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(fraction)
                            .background(palette[idx % palette.size])
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            walletRows.forEachIndexed { idx, row ->
                val pct = (row.valueUsd / total) * 100.0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(palette[idx % palette.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        row.coin.symbol,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${String.format(Locale.US, "%.1f", pct)}%",
                        color    = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioRowItem(row: PortfolioRow, onClick: () -> Unit) {
    val themeColors = LocalThemeColors.current
    val isUp        = row.change24hPct >= 0.0
    val changeColor = if (isUp) ColorUp else ColorDown
    val arrow       = if (isUp) "▲" else "▼"
    val hasPrice    = row.priceUsd > 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface.copy(alpha = 0.85f))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Symbol badge
        Box(
            modifier = Modifier
                .size(40.dp)
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
                row.coin.symbol.take(4),
                color      = TextPrimary,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))

        // Identity + price
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.coin.symbol,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (hasPrice) CoinFormatter.formatPrice(row.priceUsd) else "—",
                    color    = TextSecondary,
                    fontSize = 12.sp
                )
                if (hasPrice && row.change24hPct != 0.0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$arrow ${String.format(Locale.US, "%.2f", abs(row.change24hPct))}%",
                        color    = changeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Holding value (if wallet) / placeholder
        Column(horizontalAlignment = Alignment.End) {
            if (row.hasWallet) {
                Text(
                    CoinFormatter.formatValueUsd(row.valueUsd),
                    color      = TextPrimary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${CoinFormatter.formatAmount(row.balance, row.priceUsd)} ${row.coin.symbol}",
                    color    = TextSecondary,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    "No wallet",
                    color    = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint     = themeColors.accent,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PortfolioEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text("📊", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "No coins followed yet",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Follow a coin from the home screen, then return here to track your portfolio.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PortfolioRefreshButton(isRefreshing: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "portfolio-spin")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "portfolio-spin-angle"
    )
    val accent = LocalThemeColors.current.accent
    IconButton(onClick = onClick, enabled = !isRefreshing) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Refresh portfolio",
            tint     = accent,
            modifier = Modifier
                .size(22.dp)
                .rotate(if (isRefreshing) angle else 0f)
        )
    }
}

