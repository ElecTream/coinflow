package com.leeam.cryptowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.data.model.CryptoWidgetData
import com.leeam.cryptowidget.ui.settings.SettingsActivity
import java.util.Locale
import kotlin.math.abs

object WidgetUpdater {

    fun updateAllWidgets(context: Context, data: CryptoWidgetData) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, CryptoWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return

        for (widgetId in ids) {
            val options = manager.getAppWidgetOptions(widgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val density = context.resources.displayMetrics.density
            val widthPx  = (minW  * density).toInt().coerceAtLeast(200)
            val heightPx = ((minH * density) * 0.30f).toInt().coerceAtLeast(40)

            val views = buildRemoteViews(context, data, widthPx, heightPx)
            manager.updateAppWidget(widgetId, views)
        }
    }

    fun buildRemoteViews(
        context: Context,
        data: CryptoWidgetData,
        sparklineWidthPx: Int,
        sparklineHeightPx: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Coin symbol
        views.setTextViewText(R.id.tv_coin_symbol, data.symbol)

        // Price
        if (data.errorMessage != null) {
            views.setTextViewText(R.id.tv_price, "Error")
            views.setTextColor(R.id.tv_price, 0xFFFF4466.toInt())
        } else {
            val priceText = "$${String.format(Locale.US, "%.4f", data.priceUsd)}"
            views.setTextViewText(R.id.tv_price, priceText)
            views.setTextColor(R.id.tv_price, 0xFFFFFFFF.toInt())
        }

        // 24h change
        val isUp = data.change24hPct >= 0
        val changeText = "${if (isUp) "▲" else "▼"} ${String.format(Locale.US, "%.2f", abs(data.change24hPct))}%"
        views.setTextViewText(R.id.tv_change, changeText)
        views.setTextColor(
            R.id.tv_change,
            if (isUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
        )

        // Sparkline
        if (data.sparklinePrices.size >= 2) {
            val bitmap = SparklineRenderer.render(
                data.sparklinePrices, sparklineWidthPx, sparklineHeightPx, isUp
            )
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.iv_sparkline, bitmap)
            }
        }

        // Wallet row
        if (data.walletBalanceXrp > 0.0) {
            views.setViewVisibility(R.id.wallet_row, View.VISIBLE)
            views.setTextViewText(
                R.id.tv_balance,
                String.format(Locale.US, "%.4f XRP", data.walletBalanceXrp)
            )
            views.setTextViewText(
                R.id.tv_portfolio_value,
                "≈ $${String.format(Locale.US, "%.2f", data.walletValueUsd)}"
            )
        } else {
            views.setViewVisibility(R.id.wallet_row, View.GONE)
        }

        // Timestamp
        val ageMin = if (data.lastUpdatedMs > 0L) {
            ((System.currentTimeMillis() - data.lastUpdatedMs) / 60_000L).toInt()
        } else -1
        val ageText = when {
            ageMin < 0  -> "Never updated"
            ageMin < 1  -> "Just now"
            ageMin < 60 -> "${ageMin}m ago"
            else        -> "${ageMin / 60}h ago"
        }
        views.setTextViewText(R.id.tv_last_updated, ageText)

        // Refresh button PendingIntent
        val refreshIntent = Intent(context, CryptoWidgetProvider::class.java).apply {
            action = "com.leeam.cryptowidget.ACTION_REFRESH"
        }
        val refreshPi = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPi)

        // Settings icon PendingIntent
        val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val settingsPi = PendingIntent.getActivity(
            context, 1, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_settings, settingsPi)

        return views
    }
}
