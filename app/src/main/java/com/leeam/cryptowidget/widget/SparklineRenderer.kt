package com.leeam.cryptowidget.widget

import android.graphics.*
import com.leeam.cryptowidget.data.local.ChartStyle

object SparklineRenderer {

    fun render(
        prices: List<Double>,
        widthPx: Int,
        heightPx: Int,
        isUp: Boolean,
        chartStyle: ChartStyle = ChartStyle.AREA
    ): Bitmap? {
        if (prices.size < 2 || widthPx <= 0 || heightPx <= 0) return null

        return when (chartStyle) {
            ChartStyle.LINE   -> renderLine(prices, widthPx, heightPx)
            ChartStyle.CANDLE -> renderCandle(prices, widthPx, heightPx)
            ChartStyle.AREA   -> renderArea(prices, widthPx, heightPx, isUp)
        }
    }

    // ── LINE ──────────────────────────────────────────────────────────────
    // Each segment is independently colored green (up) or red (down) based
    // on whether the next price point is higher or lower than the current one.
    private fun renderLine(
        prices: List<Double>,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val minPrice = prices.min()
        val maxPrice = prices.max()
        val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val padding = 4f
        val stepX = widthPx.toFloat() / (prices.size - 1)

        fun priceToY(price: Double): Float =
            padding + ((maxPrice - price) / range * (heightPx - 2 * padding)).toFloat()

        // Reuse Paint instances to avoid per-segment allocation
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
        }

        var lastSegmentUp = true
        for (i in 0 until prices.size - 1) {
            val isSegmentUp = prices[i + 1] >= prices[i]
            lastSegmentUp = isSegmentUp
            val segColor = if (isSegmentUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
            val segGlow  = if (isSegmentUp) 0x4400FF88.toInt() else 0x44FF4466.toInt()

            val x0 = i * stepX
            val y0 = priceToY(prices[i])
            val x1 = (i + 1) * stepX
            val y1 = priceToY(prices[i + 1])

            glowPaint.color = segGlow
            canvas.drawLine(x0, y0, x1, y1, glowPaint)

            linePaint.color = segColor
            canvas.drawLine(x0, y0, x1, y1, linePaint)
        }

        // Endpoint dot — colored by the last segment's direction
        val lastX = (prices.size - 1) * stepX
        val lastY = priceToY(prices.last())
        val dotColor = if (lastSegmentUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
        canvas.drawCircle(lastX, lastY, 3.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dotColor
            style = Paint.Style.FILL
        })

        return bitmap
    }

    // ── CANDLE ────────────────────────────────────────────────────────────
    // Uses each adjacent pair of price points as open/close for a candle.
    // 24 hourly prices → 23 candles (much denser than the old pair-grouping approach).
    private fun renderCandle(
        prices: List<Double>,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val minPrice = prices.min()
        val maxPrice = prices.max()
        val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val padding = 4f
        val upColor   = 0xFF00FF88.toInt()
        val downColor = 0xFFFF4466.toInt()

        fun priceToY(price: Double): Float =
            padding + ((maxPrice - price) / range * (heightPx - 2 * padding)).toFloat()

        // Each candle: open=prices[i], close=prices[i+1]
        val candleCount = prices.size - 1
        if (candleCount <= 0) return bitmap

        val totalWidth = widthPx - 2 * padding
        val candleSlotWidth = totalWidth / candleCount
        val candleBodyWidth = (candleSlotWidth * 0.45f).coerceAtLeast(2f)
        val wickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (i in 0 until candleCount) {
            val open  = prices[i]
            val close = prices[i + 1]
            val high  = maxOf(open, close)
            val low   = minOf(open, close)
            val isUpCandle = close >= open
            val color = if (isUpCandle) upColor else downColor

            val centerX = padding + i * candleSlotWidth + candleSlotWidth / 2f

            // Wick
            wickPaint.color = color
            canvas.drawLine(centerX, priceToY(high), centerX, priceToY(low), wickPaint)

            // Body
            val bodyTop    = priceToY(high)
            val bodyBottom = priceToY(low)
            val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(2f)
            val rect = RectF(
                centerX - candleBodyWidth / 2f,
                bodyTop,
                centerX + candleBodyWidth / 2f,
                bodyTop + bodyHeight
            )
            bodyPaint.color = color
            canvas.drawRoundRect(rect, 1f, 1f, bodyPaint)
        }

        return bitmap
    }

    // ── AREA ──────────────────────────────────────────────────────────────
    // Gradient fill color is driven by the 24h API change direction (same source
    // as the ▲/▼ percentage label) so both always agree. The line on top is
    // per-segment colored, same as LINE, for visual consistency.
    private fun renderArea(
        prices: List<Double>,
        widthPx: Int,
        heightPx: Int,
        isUp: Boolean
    ): Bitmap {
        val minPrice = prices.min()
        val maxPrice = prices.max()
        val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fillTop    = if (isUp) 0x5500FF88.toInt() else 0x55FF4466.toInt()
        val fillBottom = 0x00000000
        val padding = 4f

        val fillPath = Path()
        val stepX = widthPx.toFloat() / (prices.size - 1)

        fun priceToY(price: Double): Float =
            padding + ((maxPrice - price) / range * (heightPx - 2 * padding)).toFloat()

        prices.forEachIndexed { i, price ->
            val x = i * stepX
            val y = priceToY(price)
            if (i == 0) {
                fillPath.moveTo(x, heightPx.toFloat())
                fillPath.lineTo(x, y)
            } else {
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo((prices.size - 1) * stepX, heightPx.toFloat())
        fillPath.close()

        // Gradient fill
        canvas.drawPath(fillPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, 0f, heightPx.toFloat(),
                fillTop, fillBottom,
                Shader.TileMode.CLAMP
            )
        })

        // Per-segment colored line on top
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until prices.size - 1) {
            val isSegmentUp = prices[i + 1] >= prices[i]
            linePaint.color = if (isSegmentUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
            canvas.drawLine(
                i * stepX, priceToY(prices[i]),
                (i + 1) * stepX, priceToY(prices[i + 1]),
                linePaint
            )
        }

        return bitmap
    }
}
