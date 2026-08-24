package com.electream.cryptowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.electream.cryptowidget.R
import com.electream.cryptowidget.data.local.ChartStyle
import com.electream.cryptowidget.data.local.DebugLog
import com.electream.cryptowidget.data.model.CoinDefinition
import com.electream.cryptowidget.data.model.CoinRegistry
import com.electream.cryptowidget.data.model.WidgetData
import dagger.hilt.android.EntryPointAccessors
import com.electream.cryptowidget.ui.chart.ChartDetailActivity
import com.electream.cryptowidget.ui.settings.SettingsActivity
import com.electream.cryptowidget.ui.theme.CyberColors
import com.electream.cryptowidget.ui.theme.ThemeColors
import com.electream.cryptowidget.ui.util.CoinFormatter
import java.util.Locale
import kotlin.math.abs

object WidgetUpdater {

    private const val MAX_SPARKLINE_WIDTH_PX  = 600
    private const val MAX_SPARKLINE_HEIGHT_PX = 80

    private fun debugLog(context: Context): DebugLog? = runCatching {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CoinflowWidgetEntryPoint::class.java
        ).debugLog()
    }.getOrNull()

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
        val log = debugLog(context)
        if (ids.isEmpty()) {
            log?.warn("WidgetUpdater", "updateAllWidgets called with zero installed widgets")
            return
        }

        log?.info(
            "WidgetUpdater",
            "rendering ids=${ids.toList()} active=$activeCoinId tabs=$widgetCoinIds " +
                "symbol=${data.symbol} price=${data.priceUsd} err=${data.errorMessage}"
        )

        for (widgetId in ids) {
            try {
                val options  = manager.getAppWidgetOptions(widgetId)
                val minW     = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                val minH     = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                val maxW     = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 180)
                val maxH     = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110)
                val density  = context.resources.displayMetrics.density
                // Cap the sparkline bitmap so a single RemoteViews parcel never gets close to the
                // binder transaction limit (~1MB hard, much smaller in practice). The sparkline is
                // displayed scaled (fitXY) so the source resolution can be modest without looking
                // bad. 600x80 → ~190KB ARGB_8888.
                val widthPx  = (minW  * density).toInt().coerceIn(200, MAX_SPARKLINE_WIDTH_PX)
                val heightPx = ((minH * density) * 0.30f).toInt().coerceIn(40, MAX_SPARKLINE_HEIGHT_PX)

                log?.info(
                    "WidgetUpdater",
                    "id=$widgetId dp=(${minW}x${minH}..${maxW}x${maxH}) density=$density bmp=${widthPx}x${heightPx}"
                )

                val views = buildRemoteViews(
                    context, data, widthPx, heightPx, chartStyle, themeColors,
                    widgetCoinIds = widgetCoinIds,
                    activeCoinId  = activeCoinId,
                    coinLookup    = coinLookup
                )
                manager.updateAppWidget(widgetId, views)
                log?.info("WidgetUpdater", "updateAppWidget OK id=$widgetId")
            } catch (e: Exception) {
                log?.error("WidgetUpdater", "render failed id=$widgetId", e)
                runCatching {
                    val fallback = RemoteViews(context.packageName, R.layout.widget_loading)
                    manager.updateAppWidget(widgetId, fallback)
                }
                throw e
            }
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

        wireTabs(views, context, widgetCoinIds, activeCoinId, themeColors, coinLookup)

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

        wireControlPendingIntents(views, context)
        return views
    }

    /**
     * Loading skeleton: same `widget_layout.xml` as [buildRemoteViews] (so tabs and controls
     * are present and tappable) but with placeholder text instead of real data. Used when the
     * user has picked coins but the cache hasn't been populated yet (typically the first few
     * seconds after a reinstall, before the worker writes any per-coin keys).
     */
    fun buildLoadingSkeletonRemoteViews(
        context: Context,
        widgetCoinIds: List<String>,
        activeCoinId: String,
        coinLookup: Map<String, CoinDefinition>,
        themeColors: ThemeColors = CyberColors
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        wireTabs(views, context, widgetCoinIds, activeCoinId, themeColors, coinLookup)

        views.setTextViewText(R.id.tv_price, "—")
        views.setTextColor(R.id.tv_price, 0xFFFFFFFF.toInt())

        views.setTextViewText(R.id.tv_change, "…")
        views.setTextColor(R.id.tv_change, 0xFF8899BB.toInt())

        views.setViewVisibility(R.id.wallet_row, View.GONE)
        views.setTextViewText(R.id.tv_last_updated, "Fetching…")

        CoinflowWidgetProvider.cancelSpinner()
        views.setImageViewResource(R.id.btn_refresh, R.drawable.ic_refresh)
        views.setInt(R.id.btn_refresh, "setImageAlpha", 255)

        wireControlPendingIntents(views, context)
        return views
    }

    /**
     * Empty-state CTA: shown when no coins have been picked yet. Whole body is a single
     * tappable surface that opens [SettingsActivity] so the user can choose coins to track.
     * No refresh/settings buttons — the layout has only the CTA text.
     */
    fun buildEmptyRemoteViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_empty)
        val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        views.setOnClickPendingIntent(
            R.id.widget_empty_root,
            PendingIntent.getActivity(
                context, 3, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        return views
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun wireTabs(
        views: RemoteViews,
        context: Context,
        widgetCoinIds: List<String>,
        activeCoinId: String,
        themeColors: ThemeColors,
        coinLookup: Map<String, CoinDefinition>
    ) {
        val tabIds = listOf(
            R.id.tab_coin_1, R.id.tab_coin_2, R.id.tab_coin_3,
            R.id.tab_coin_4, R.id.tab_coin_5
        )
        val showTabs = widgetCoinIds.isNotEmpty()

        tabIds.forEachIndexed { index, tabId ->
            if (index < widgetCoinIds.size) {
                val tabCoinId = widgetCoinIds[index]
                val tabCoin   = coinLookup[tabCoinId] ?: CoinRegistry.byId(tabCoinId)
                val isActive  = tabCoinId == activeCoinId
                val tabColor  = if (isActive) themeColors.accentArgb else 0xFF5A6A7A.toInt()

                views.setViewVisibility(tabId, View.VISIBLE)
                views.setTextViewText(tabId, tabCoin.symbol)
                views.setTextColor(tabId, tabColor)

                val selectIntent = Intent(context, CoinflowWidgetProvider::class.java).apply {
                    action = CoinflowWidgetProvider.ACTION_SELECT_COIN
                    putExtra(CoinflowWidgetProvider.EXTRA_COIN_ID, tabCoinId)
                }
                views.setOnClickPendingIntent(
                    tabId,
                    PendingIntent.getBroadcast(
                        context, 100 + index, selectIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                views.setViewVisibility(tabId, View.GONE)
            }
        }
        views.setViewVisibility(R.id.tab_divider, if (showTabs) View.VISIBLE else View.GONE)
    }

    private fun wireControlPendingIntents(views: RemoteViews, context: Context) {
        val refreshIntent = Intent(context, CoinflowWidgetProvider::class.java).apply {
            action = "com.electream.cryptowidget.ACTION_REFRESH"
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
    }
}
