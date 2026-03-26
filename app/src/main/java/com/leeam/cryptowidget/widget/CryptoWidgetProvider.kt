package com.leeam.cryptowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.worker.PriceUpdateWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

    private fun enqueueRefresh(context: Context) {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<PriceUpdateWorker>().build())
    }
}
