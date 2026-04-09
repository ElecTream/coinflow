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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text(
                "Coinflow",
                color      = TextPrimary,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Crypto price tracker",
                color    = TextSecondary,
                fontSize = 13.sp
            )

            // ── Follow management ──────────────────────────────────────────
            CryptoCard {
                SectionLabel("FOLLOW")
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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

            // ── Widget tab management ──────────────────────────────────────
            CryptoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("WIDGET TABS")
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${state.widgetCoinIds.size}/5",
                        color    = if (state.widgetCoinIds.size >= 5) ColorDown else TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    Spacer(Modifier.height(4.dp))
                    Text("Widget tab limit reached (5 max)", color = ColorDown, fontSize = 10.sp)
                }
            }

            // Live price card
            CryptoCard {
                SectionLabel("LIVE PRICE")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            state.priceLoading   -> HomePriceShimmer(Modifier.fillMaxWidth())
                            state.priceError != null ->
                                Text("Error: ${state.priceError}", color = ColorDown, fontSize = 12.sp)
                            state.livePrice != null ->
                                HomePriceDisplay(
                                    price    = state.livePrice!!,
                                    change24h = state.change24h
                                )
                        }
                    }
                    HomePriceRefreshButton(isSpinning = state.priceLoading, onClick = vm::fetchLivePrice)
                }
            }

            // Navigation links
            SectionLabel("SETTINGS")
            Spacer(Modifier.height(0.dp))

            NavLinkCard(
                title    = "Price Alerts & Wallet",
                subtitle = "Alerts, thresholds, and wallet balance for ${CoinRegistry.byId(state.coinId).symbol}",
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
                title    = "Portfolio",
                subtitle = "Multi-coin portfolio view",
                onClick  = { navController.navigate(Screen.PORTFOLIO) }
            )
            NavLinkCard(
                title    = "Add Coin",
                subtitle = "Track a Kraken pair or any custom REST endpoint",
                onClick  = { navController.navigate(Screen.ADD_COIN) }
            )
        }
    }
}

@Composable
private fun NavLinkCard(title: String, subtitle: String, onClick: () -> Unit) {
    val themeColors = LocalThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
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
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }

    change24h?.let { change ->
        val upColor = if (change >= 0) ColorUp else ColorDown
        val arrow   = if (change >= 0) "▲" else "▼"
        Text(
            "$arrow ${String.format(Locale.US, "%.2f", abs(change))}%  24h",
            color    = upColor,
            fontSize = 12.sp
        )
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
    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(Color.Transparent, accent.copy(alpha = 0.35f), Color.Transparent),
        start  = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
        end    = androidx.compose.ui.geometry.Offset(shimmerX + 400f, 0f)
    )
    Box(
        modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
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
        border  = BorderStroke(1.dp, accent),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Refresh",
            modifier = Modifier
                .size(16.dp)
                .rotate(if (isSpinning) spinAngle else 0f),
            tint = accent
        )
        Spacer(Modifier.width(4.dp))
        Text("Refresh", fontSize = 11.sp)
    }
}
