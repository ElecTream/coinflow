package com.leeam.cryptowidget.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.ui.ParticleField
import com.leeam.cryptowidget.ui.components.CryptoCard
import com.leeam.cryptowidget.ui.components.CryptoFilterChip
import com.leeam.cryptowidget.ui.components.SectionLabel
import com.leeam.cryptowidget.ui.nav.Screen
import com.leeam.cryptowidget.ui.settings.SettingsViewModel
import com.leeam.cryptowidget.ui.theme.*
import com.leeam.cryptowidget.ui.util.CoinFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    vm: SettingsViewModel,
    navController: NavController
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val activeCoin = CoinRegistry.byId(state.coinId)

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
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HomeHeader()

            // ── Live price hero ─────────────────────────────────────────────
            CryptoCard {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("LIVE · ${activeCoin.symbol}")
                    Spacer(Modifier.weight(1f))
                    HomePriceRefreshButton(isSpinning = state.priceLoading, onClick = vm::fetchLivePrice)
                }
                Spacer(Modifier.height(10.dp))
                when {
                    state.priceLoading && state.livePrice == null ->
                        HomePriceShimmer(Modifier.fillMaxWidth())
                    state.priceError != null && state.livePrice == null ->
                        Text(
                            "Error: ${state.priceError}",
                            color = ColorDown,
                            fontSize = 12.sp
                        )
                    state.livePrice != null ->
                        HomePriceDisplay(
                            price     = state.livePrice!!,
                            change24h = state.change24h
                        )
                    else ->
                        HomePriceShimmer(Modifier.fillMaxWidth())
                }
            }

            // ── Portfolio quick link (prominent) ────────────────────────────
            PortfolioLinkCard(
                followedCount = state.followedCoinIds.size,
                onClick       = { navController.navigate(Screen.PORTFOLIO) }
            )

            // ── Follow management ───────────────────────────────────────────
            CryptoCard {
                SectionLabel("FOLLOW")
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    CoinRegistry.all.forEach { coin ->
                        CryptoFilterChip(
                            label    = coin.symbol,
                            selected = coin.id in state.followedCoinIds,
                            onClick  = { vm.toggleFollow(coin.id) }
                        )
                    }
                }
            }

            // ── Widget tabs ────────────────────────────────────────────────
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
                Spacer(Modifier.height(10.dp))
                if (state.followedCoinIds.isEmpty()) {
                    Text(
                        "Follow at least one coin to add it to the widget.",
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
                                onClick  = { if (onWidget || canAdd) vm.toggleWidgetCoin(coinId) }
                            )
                        }
                    }
                    if (state.widgetCoinIds.size >= 5) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Widget tab limit reached (5 max).",
                            color = ColorDown,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // ── Settings navigation ────────────────────────────────────────
            Spacer(Modifier.height(2.dp))
            SectionLabel("SETTINGS")
            Spacer(Modifier.height(2.dp))

            NavLinkCard(
                title    = "Price Alerts & Wallet",
                subtitle = "Thresholds and balance for ${activeCoin.symbol}",
                onClick  = { navController.navigate(Screen.coinDetail(state.coinId)) }
            )
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
            NavLinkCard(
                title    = "Add Coin",
                subtitle = "Track a Kraken pair or any custom REST endpoint",
                leadingIcon = Icons.Default.Add,
                onClick  = { navController.navigate(Screen.ADD_COIN) }
            )
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "Coinflow",
            color      = TextPrimary,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Text(
            "Crypto price tracker",
            color    = TextSecondary,
            fontSize = 13.sp
        )
    }
}

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
                if (followedCount == 0) "Add coins to start tracking"
                else "Track $followedCount coin${if (followedCount == 1) "" else "s"} in one place",
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
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColors.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint     = themeColors.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color      = TextPrimary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color    = TextSecondary,
                fontSize = 12.sp
            )
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
private fun HomePriceDisplay(price: Double, change24h: Double?) {
    val flashAlpha = remember { Animatable(0f) }
    val flashColorState = remember { mutableStateOf(ColorUp) }

    LaunchedEffect(price) {
        flashColorState.value = if ((change24h ?: 0.0) >= 0) ColorUp else ColorDown
        flashAlpha.snapTo(1f)
        flashAlpha.animateTo(0f, animationSpec = tween(900))
    }

    val priceColor = lerp(TextPrimary, flashColorState.value, flashAlpha.value)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AnimatedContent(
            targetState = price,
            transitionSpec = {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "price_digits"
        ) { p ->
            Text(
                CoinFormatter.formatPrice(p),
                color      = priceColor,
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }

        change24h?.let { change ->
            val upColor = if (change >= 0) ColorUp else ColorDown
            val arrow   = if (change >= 0) "▲" else "▼"
            Text(
                "$arrow ${String.format(Locale.US, "%.2f", abs(change))}%   ·   24h",
                color    = upColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HomePriceShimmer(modifier: Modifier = Modifier) {
    val accent = LocalThemeColors.current.accent
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -400f, targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, accent.copy(alpha = 0.35f), Color.Transparent),
        start  = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
        end    = androidx.compose.ui.geometry.Offset(shimmerX + 400f, 0f)
    )
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .background(brush)
    )
}

@Composable
private fun HomePriceRefreshButton(isSpinning: Boolean, onClick: () -> Unit) {
    val spinTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "spin-angle"
    )
    val accent = LocalThemeColors.current.accent
    OutlinedButton(
        onClick = onClick,
        colors  = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        border  = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
        shape   = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Refresh",
            modifier = Modifier
                .size(14.dp)
                .rotate(if (isSpinning) spinAngle else 0f),
            tint = accent
        )
        Spacer(Modifier.width(6.dp))
        Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
