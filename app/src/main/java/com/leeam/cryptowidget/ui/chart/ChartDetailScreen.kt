package com.leeam.cryptowidget.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leeam.cryptowidget.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailScreen(
    vm: ChartDetailViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.symbol.isNotEmpty()) "${state.symbol} · 24h Chart" else "24h Chart",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
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
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = themeColors.accent
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = themeColors.accent
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
                .padding(padding)
        ) {
            ChartHeader(
                symbol       = state.symbol,
                priceUsd     = state.priceUsd,
                change24hPct = state.change24hPct
            )

            Spacer(Modifier.height(12.dp))

            if (state.prices.size >= 2) {
                InteractiveChart(
                    prices     = state.prices,
                    timestamps = state.timestamps,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.72f)
                        .padding(horizontal = 16.dp)
                )
            } else {
                Box(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.72f),
                    contentAlignment    = Alignment.Center
                ) {
                    CircularProgressIndicator(color = themeColors.accent)
                }
            }
        }
    }
}

@Composable
private fun ChartHeader(symbol: String, priceUsd: Double, change24hPct: Double) {
    val isUp = change24hPct >= 0
    val changeColor = if (isUp) ColorUp else ColorDown
    val arrow = if (isUp) "▲" else "▼"

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text         = symbol,
            color        = LocalThemeColors.current.accent,
            fontSize     = 13.sp,
            fontWeight   = FontWeight.Medium,
            letterSpacing = 1.5.sp
        )
        Text(
            text       = "$${String.format(Locale.US, "%.4f", priceUsd)}",
            color      = TextPrimary,
            fontSize   = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text     = "$arrow ${String.format(Locale.US, "%.2f", abs(change24hPct))}%  24h",
            color    = changeColor,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun InteractiveChart(
    prices: List<Double>,
    timestamps: List<Long>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val n = prices.size
    if (n < 2) return

    val minPrice  = prices.min()
    val maxPrice  = prices.max()
    val priceRange = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0

    Box(modifier = modifier) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(n) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            when (event.type) {
                                PointerEventType.Press,
                                PointerEventType.Move -> {
                                    val stepX = size.width.toFloat() / (n - 1)
                                    selectedIndex = (change.position.x / stepX)
                                        .roundToInt()
                                        .coerceIn(0, n - 1)
                                    change.consume()
                                }
                                PointerEventType.Release -> {
                                    selectedIndex = -1
                                    change.consume()
                                }
                                else -> {}
                            }
                        }
                    }
                }
        ) {
            drawSparkline(prices, n, minPrice, priceRange, selectedIndex)
        }

        // Tooltip overlay — Compose Box above the Canvas
        if (selectedIndex >= 0) {
            ChartTooltip(
                price      = prices[selectedIndex],
                timestamp  = timestamps.getOrNull(selectedIndex),
                isUp       = segmentIsUp(prices, selectedIndex),
                index      = selectedIndex,
                totalPoints = n,
                modifier   = Modifier.fillMaxSize()
            )
        } else if (prices.size >= 2) {
            // Resting state: current price label at bottom-right
            val lastUp = prices.last() >= prices[n - 2]
            Box(
                modifier         = Modifier.fillMaxSize().padding(end = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text     = "$${String.format(Locale.US, "%.4f", prices.last())}",
                    color    = if (lastUp) ColorUp else ColorDown,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun DrawScope.drawSparkline(
    prices: List<Double>,
    n: Int,
    minPrice: Double,
    priceRange: Double,
    selectedIndex: Int
) {
    val padding = 8f
    val stepX   = size.width / (n - 1)

    fun priceToY(price: Double): Float =
        padding + ((prices.max() - price) / priceRange * (size.height - 2 * padding)).toFloat()

    // Per-segment colored line — same algorithm as SparklineRenderer.renderLine()
    for (i in 0 until n - 1) {
        val segUp    = prices[i + 1] >= prices[i]
        val segColor = if (segUp) ColorUp else ColorDown
        val x0 = i * stepX
        val y0 = priceToY(prices[i])
        val x1 = (i + 1) * stepX
        val y1 = priceToY(prices[i + 1])

        // Glow underlay
        drawLine(
            color       = segColor.copy(alpha = 0.20f),
            start       = Offset(x0, y0),
            end         = Offset(x1, y1),
            strokeWidth = 8f,
            cap         = StrokeCap.Round
        )
        // Crisp line
        drawLine(
            color       = segColor,
            start       = Offset(x0, y0),
            end         = Offset(x1, y1),
            strokeWidth = 2.5f,
            cap         = StrokeCap.Round
        )
    }

    // Endpoint dot
    val lastX  = (n - 1) * stepX
    val lastY  = priceToY(prices.last())
    val lastUp = prices.last() >= prices[n - 2]
    drawCircle(
        color  = if (lastUp) ColorUp else ColorDown,
        radius = 4f,
        center = Offset(lastX, lastY)
    )

    // Scrubber line + dot
    if (selectedIndex >= 0) {
        val scrubX = selectedIndex * stepX
        drawLine(
            color       = Color.White.copy(alpha = 0.55f),
            start       = Offset(scrubX, 0f),
            end         = Offset(scrubX, size.height),
            strokeWidth = 1.5f
        )
        val scrubY = priceToY(prices[selectedIndex])
        val dotUp  = segmentIsUp(prices, selectedIndex)
        drawCircle(
            color  = if (dotUp) ColorUp else ColorDown,
            radius = 6f,
            center = Offset(scrubX, scrubY)
        )
    }
}

/** True if the price at [index] is >= the previous price (or the next, if index == 0). */
private fun segmentIsUp(prices: List<Double>, index: Int): Boolean = when {
    index > 0              -> prices[index] >= prices[index - 1]
    prices.size >= 2       -> prices[1] >= prices[0]
    else                   -> true
}

@Composable
private fun ChartTooltip(
    price: Double,
    timestamp: Long?,
    isUp: Boolean,
    index: Int,
    totalPoints: Int,
    modifier: Modifier = Modifier
) {
    val tooltipColor = if (isUp) ColorUp else ColorDown
    val timeText  = timestamp?.let { formatTimestamp(it) } ?: "--"
    val priceText = "$${String.format(Locale.US, "%.4f", price)}"

    // Pin tooltip to left side when scrubber is in the right half, and vice versa
    val alignment = if (index > totalPoints / 2) Alignment.TopStart else Alignment.TopEnd

    Box(
        modifier         = modifier.padding(8.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(Surface.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                .border(1.dp, tooltipColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(timeText,  color = TextSecondary, fontSize = 11.sp)
            Text(
                priceText,
                color      = tooltipColor,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a")

private fun formatTimestamp(unixSeconds: Long): String {
    val zoned = Instant.ofEpochSecond(unixSeconds).atZone(ZoneId.systemDefault())
    return timeFormatter.format(zoned)
}
