package com.leeam.cryptowidget.widget

import android.graphics.*

object SparklineRenderer {

    fun render(
        prices: List<Double>,
        widthPx: Int,
        heightPx: Int,
        isUp: Boolean
    ): Bitmap? {
        if (prices.size < 2 || widthPx <= 0 || heightPx <= 0) return null

        val minPrice = prices.min()
        val maxPrice = prices.max()
        val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val lineColor  = if (isUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
        val glowColor  = if (isUp) 0x4400FF88.toInt() else 0x44FF4466.toInt()
        val fillTop    = if (isUp) 0x3300FF88.toInt() else 0x33FF4466.toInt()
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

        // 1. Gradient fill under the line
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, 0f, heightPx.toFloat(),
                fillTop, fillBottom,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(fillPath, fillPaint)

        // 2. Glow layer
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(linePath, glowPaint)

        // 3. Crisp line
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(linePath, linePaint)

        // 4. Endpoint dot
        val lastX = (prices.size - 1) * stepX
        val lastY = priceToY(prices.last())
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(lastX, lastY, 3.5f, dotPaint)

        return bitmap
    }
}
