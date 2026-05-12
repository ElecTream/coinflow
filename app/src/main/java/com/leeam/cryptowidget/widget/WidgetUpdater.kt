package com.leeam.cryptowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.data.local.ChartStyle
import com.leeam.cryptowidget.data.model.CoinDefinition
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.model.WidgetData
import com.leeam.cryptowidget.ui.chart.ChartDetailActivity
import com.leeam.cryptowidget.ui.settings.SettingsActivity
import com.leeam.cryptowidget.ui.theme.CyberColors
import com.leeam.cryptowidget.ui.theme.ThemeColors
import com.leeam.cryptowidget.ui.util.CoinFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

object WidgetUpdater {

    fun updateAllWidgets(
        context: Context,
        data: WidgetData,
        chartStyle: ChartStyle = ChartStyle.LINE,
        themeColors: ThemeColors = CyberColors,
        widgetCoinIds: List<String> = listOf(data.coinId),
        activeCoinId: String = data.coinId,
        coinLookup: Map<String, CoinDefinition> = emptyMap()
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, CoinflowWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return

        for (widgetId in ids) {
            try {
                val options  = manager.getAppWidgetOptions(widgetId)
                val minW     = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                val minH     = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                val density  = context.resources.displayMetrics.density
                val widthPx  = (minW  * density).toInt().coerceAtLeast(200)
                val heightPx = ((minH * density) * 0.30f).toInt().coerceAtLeast(40)

                val views = buildRemoteViews(
                    context, data, widthPx, heightPx, chartStyle, themeColors,
                    widgetCoinIds = widgetCoinIds,
                    activeCoinId  = activeCoinId,
                    coinLookup    = coinLookup
                )
                manager.updateAppWidget(widgetId, views)
            } catch (e: Exception) {
                // If render fails, fall back to a minimal text-only view so the launcher
                // doesn't show "Problem loading widget". The exception detail is captured
                // by the caller's outer try/catch (which logs to DebugLog).
                runCatching {
                    val fallback = RemoteViews(context.packageName, R.layout.widget_loading)
                    manager.updateAppWidget(widgetId, fallback)
                }
                throw e
            }
        }

        // Price flash: highlight the price text green/red briefly, then reset to white
        if (data.errorMessage == null) {
            val isUp      = data.change24hPct >= 0
            val flashColor = if (isUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
            CoroutineScope(Dispatchers.Main).launch {
                delay(400)
                val resetViews = RemoteViews(context.packageName, R.layout.widget_layout)
                resetViews.setTextColor(R.id.tv_price, 0xFFFFFFFF.toInt())
                ids.forEach { manager.partiallyUpdateAppWidget(it, resetViews) }
            }
            val flashViews = RemoteViews(context.packageName, R.layout.widget_layout)
            flashViews.setTextColor(R.id.tv_price, flashColor)
            ids.forEach { manager.partiallyUpdateAppWidget(it, flashViews) }
        }
    }

    fun buildRemoteViews(
        context: Context,
        data: WidgetData,
        sparklineWidthPx: Int,
        sparklineHeightPx: Int,
        chartStyle: ChartStyle = ChartStyle.LINE,
        themeColors: ThemeColors = CyberColors,
        widgetCoinIds: List<String> = listOf(data.coinId),
        activeCoinId: String = data.coinId,
        coinLookup: Map<String, CoinDefinition> = emptyMap()
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        fun resolveCoin(id: String): CoinDefinition =
            coinLookup[id] ?: CoinRegistry.byId(id)

        // ── Coin tabs ─────────────────────────────────────────────────────────
        val tabIds = listOf(
            R.id.tab_coin_1, R.id.tab_coin_2, R.id.tab_coin_3,
            R.id.tab_coin_4, R.id.tab_coin_5
        )
        val showTabs = widgetCoinIds.isNotEmpty()

        tabIds.forEachIndexed { index, tabId ->
            if (index < widgetCoinIds.size) {
                val tabCoinId = widgetCoinIds[index]
                val tabCoin   = resolveCoin(tabCoinId)
                val isActive  = tabCoinId == activeCoinId
                val tabColor  = if (isActive) themeColors.accentArgb else 0xFF5A6A7A.toInt()

                views.setViewVisibility(tabId, View.VISIBLE)
                views.setTextViewText(tabId, tabCoin.symbol)
                views.setTextColor(tabId, tabColor)

                // Tap switches the active coin
                val selectIntent = Intent(context, CoinflowWidgetProvider::class.java).apply {
                    action = CoinflowWidgetProvider.ACTION_SELECT_COIN
                    putExtra(CoinflowWidgetProvider.EXTRA_COIN_ID, tabCoinId)
                }
                val selectPi = PendingIntent.getBroadcast(
                    context,
                    100 + index,
                    selectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(tabId, selectPi)
            } else {
                views.setViewVisibility(tabId, View.GONE)
            }
        }
        views.setViewVisibility(R.id.tab_divider, if (showTabs) View.VISIBLE else View.GONE)

        // ── Price ─────────────────────────────────────────────────────────────
        if (data.errorMessage != null) {
            views.setTextViewText(R.id.tv_price, "Error")
            views.setTextColor(R.id.tv_price, 0xFFFF4466.toInt())
        } else {
            views.setTextViewText(R.id.tv_price, CoinFormatter.formatPrice(data.priceUsd))
            views.setTextColor(R.id.tv_price, 0xFFFFFFFF.toInt())
        }

        // ── 24h change ────────────────────────────────────────────────────────
        val changeIsUp  = data.change24hPct >= 0
        val changeText  = "${if (changeIsUp) "▲" else "▼"} ${String.format(Locale.US, "%.2f", abs(data.change24hPct))}%"
        views.setTextViewText(R.id.tv_change, changeText)
        views.setTextColor(
            R.id.tv_change,
            if (changeIsUp) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()
        )

        // ── Sparkline ─────────────────────────────────────────────────────────
        if (data.sparklinePrices.size >= 2) {
            val bitmap = SparklineRenderer.render(
                data.sparklinePrices, sparklineWidthPx, sparklineHeightPx, changeIsUp, chartStyle
            )
            if (bitmap != null) views.setImageViewBitmap(R.id.iv_sparkline, bitmap)
        }

        // ── Wallet row ────────────────────────────────────────────────────────
        if (data.walletBalance > 0.0) {
            views.setViewVisibility(R.id.wallet_row, View.VISIBLE)
            views.setTextViewText(
                R.id.tv_balance,
                CoinFormatter.formatBalanceWithSymbol(data.walletBalance, data.priceUsd, data.symbol)
            )
            views.setTextColor(R.id.tv_balance, themeColors.accentArgb)
            views.setTextViewText(
                R.id.tv_portfolio_value,
                "≈ ${CoinFormatter.formatValueUsd(data.walletValueUsd)}"
            )
        } else {
            views.setViewVisibility(R.id.wallet_row, View.GONE)
        }

        // ── Timestamp ─────────────────────────────────────────────────────────
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

        // Cancel any in-flight spin frames before restoring the static icon
        CoinflowWidgetProvider.cancelSpinner()
        views.setImageViewResource(R.id.btn_refresh, R.drawable.ic_refresh)
        views.setInt(R.id.btn_refresh, "setImageAlpha", 255)

        // ── PendingIntents ────────────────────────────────────────────────────
        val refreshIntent = Intent(context, CoinflowWidgetProvider::class.java).apply {
            action = "com.leeam.cryptowidget.ACTION_REFRESH"
        }
        views.setOnClickPendingIntent(
            R.id.btn_refresh,
            PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val chartIntent = Intent(context, ChartDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        views.setOnClickPendingIntent(
            R.id.iv_sparkline,
            PendingIntent.getActivity(
                context, 2, chartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        views.setOnClickPendingIntent(
            R.id.btn_settings,
            PendingIntent.getActivity(
                context, 1, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return views
    }
}
