package com.leeam.cryptowidget.ui.chart

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.widget.CryptoWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChartDetailUiState(
    val symbol: String = "",
    val priceUsd: Double = 0.0,
    val change24hPct: Double = 0.0,
    val prices: List<Double> = emptyList(),
    val timestamps: List<Long> = emptyList()   // Unix seconds from OHLC
)

@HiltViewModel
class ChartDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: WidgetPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ChartDetailUiState())
    val state: StateFlow<ChartDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.coinId,
                prefs.cachedPriceUsd,
                prefs.cachedChangePct,
                prefs.cachedSparkline,
                prefs.cachedSparklineTimestamps
            ) { coinId, price, change, sparkline, timestamps ->
                ChartDetailUiState(
                    symbol       = CoinRegistry.byId(coinId).symbol,
                    priceUsd     = price,
                    change24hPct = change,
                    prices       = sparkline,
                    timestamps   = timestamps
                )
            }.collect { _state.value = it }
        }
    }

    /** Broadcasts ACTION_REFRESH — same path as the widget's manual refresh button. */
    fun refresh() {
        context.sendBroadcast(
            Intent(context, CryptoWidgetProvider::class.java).apply {
                action = "com.leeam.cryptowidget.ACTION_REFRESH"
            }
        )
    }
}
