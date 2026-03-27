package com.leeam.cryptowidget.widget

import android.graphics.*
import com.leeam.cryptowidget.data.local.ChartStyle

object SparklineRenderer {

    fun render(
        prices: List<Double>,
        widthPx: Int,
        heightPx: Int,
        isUp: Boolean,
        chartStyle: ChartStyle = ChartStyle.LINE
    ): Bitmap? {
        if (prices.size < 2 || widthPx <= 0 || heightPx <= 0) return null

        return when (chartStyle) {
            ChartStyle.LINE   -> renderLine(prices, widthPx, heightPx, isUp)
            ChartStyle.CANDLE -> renderCandle(prices, widthPx, heightPx)
            ChartStyle.AREA   -> renderArea(prices, widthPx, heightPx, isUp)
        }
    }

    // ── LINE ──────────────────────────────────────────────────────────────
    private fun renderLine(
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

        val lineColor  = if (isUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
        val glowColor  = if (isUp) 0x4400FF88.toInt() else 0x44FF4466.toInt()
        val padding = 4f

        val linePath = Path()
        val stepX = widthPx.toFloat() / (prices.size - 1)

        fun priceToY(price: Double): Float =
            padding + ((maxPrice - price) / range * (heightPx - 2 * padding)).toFloat()

        prices.forEachIndexed { i, price ->
            val x = i * stepX
            val y = priceToY(price)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        // Glow
        canvas.drawPath(linePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        })

        // Crisp line
        canvas.drawPath(linePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })

        // Endpoint dot
        val lastX = (prices.size - 1) * stepX
        val lastY = priceToY(prices.last())
        canvas.drawCircle(lastX, lastY, 3.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.FILL
        })

        return bitmap
    }

    // ── CANDLE ────────────────────────────────────────────────────────────
    // Groups hourly price points into pairs to form OHLC-style candles.
    // Each candle: open = first price in pair, close = second price in pair,
    //              high = max of pair, low = min of pair.
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

        // Group prices into candle pairs
        val candleCount = prices.size / 2
        if (candleCount == 0) return bitmap

        val totalWidth = widthPx - 2 * padding
        val candleSlotWidth = totalWidth / candleCount
        val candleBodyWidth = (candleSlotWidth * 0.6f).coerceAtLeast(2f)
        val wickWidth = 1.5f

        for (i in 0 until candleCount) {
            val idx = i * 2
            val open = prices[idx]
            val close = prices[(idx + 1).coerceAtMost(prices.size - 1)]
            val high = maxOf(open, close)
            val low = minOf(open, close)
            val isUpCandle = close >= open

            val centerX = padding + i * candleSlotWidth + candleSlotWidth / 2f
            val color = if (isUpCandle) upColor else downColor

            // Wick (high-low line)
            canvas.drawLine(
                centerX, priceToY(high),
                centerX, priceToY(low),
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    style = Paint.Style.STROKE
                    strokeWidth = wickWidth
                }
            )

            // Body (open-close rectangle)
            val bodyTop = priceToY(maxOf(open, close))
            val bodyBottom = priceToY(minOf(open, close))
            val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(1.5f) // min 1.5px body
            val rect = RectF(
                centerX - candleBodyWidth / 2f,
                bodyTop,
                centerX + candleBodyWidth / 2f,
                bodyTop + bodyHeight
            )

            canvas.drawRoundRect(rect, 1f, 1f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = if (isUpCandle) Paint.Style.FILL else Paint.Style.FILL
            })
        }

        return bitmap
    }

    // ── AREA ──────────────────────────────────────────────────────────────
    // Smooth filled area with gradient — like LINE but with heavier fill
    // and no endpoint dot.
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

        val lineColor  = if (isUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
        val fillTop    = if (isUp) 0x5500FF88.toInt() else 0x55FF4466.toInt()
        val fillBottom = 0x00000000
        val padding = 4f

        val linePath = Path()
        val fillPath = Path()
        val stepX = widthPx.toFloat() / (prices.size - 1)

        fun priceToY(price: Double): Float =
            padding + ((maxPrice - price) / range * (heightPx - 2 * padding)).toFloat()

        prices.forEachIndexed { i, price ->
            val x = i * stepX
            val y = priceToY(price)
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, heightPx.toFloat())
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
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

        // Crisp line on top
        canvas.drawPath(linePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })

        return bitmap
    }
}
