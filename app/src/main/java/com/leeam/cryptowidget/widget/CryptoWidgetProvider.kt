package com.leeam.cryptowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.worker.PriceUpdateWorker

class CryptoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            val loadingViews = RemoteViews(context.packageName, R.layout.widget_loading)
            appWidgetManager.updateAppWidget(id, loadingViews)
        }
        enqueueRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.leeam.cryptowidget.ACTION_REFRESH") {
            showRefreshSpinner(context)
            enqueueRefresh(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        enqueueRefresh(context)
    }

    /**
     * Swaps btn_refresh to the spinning AVD immediately when the user taps refresh.
     * WidgetUpdater restores the static icon once the update completes.
     */
    private fun showRefreshSpinner(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CryptoWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val spinViews = RemoteViews(context.packageName, R.layout.widget_layout)
        spinViews.setImageViewResource(R.id.btn_refresh, R.drawable.ic_refresh_spin_avd)
        ids.forEach { manager.partiallyUpdateAppWidget(it, spinViews) }
    }

    private fun enqueueRefresh(context: Context) {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<PriceUpdateWorker>().build())
    }
}
