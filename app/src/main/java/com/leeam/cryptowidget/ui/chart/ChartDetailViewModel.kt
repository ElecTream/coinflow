package com.leeam.cryptowidget.ui.chart

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.repository.CoinRepository
import com.leeam.cryptowidget.widget.CoinflowWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val prefs: WidgetPreferences,
    private val coinRepository: CoinRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChartDetailUiState())
    val state: StateFlow<ChartDetailUiState> = _state.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeCoinDataFlow = prefs.coinId.flatMapLatest { coinId ->
        if (coinId.isBlank()) flowOf(ChartDetailUiState()) else {
            combine(
                prefs.priceUsdFor(coinId),
                prefs.changePctFor(coinId),
                prefs.sparklineFor(coinId),
                prefs.sparklineTsFor(coinId)
            ) { price, change, sparkline, timestamps ->
                val coin = coinRepository.coinById(coinId) ?: CoinRegistry.byId(coinId)
                ChartDetailUiState(
                    symbol       = coin.symbol,
                    priceUsd     = price,
                    change24hPct = change,
                    prices       = sparkline,
                    timestamps   = timestamps
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            activeCoinDataFlow.collect { _state.value = it }
        }
    }

    /** Broadcasts ACTION_REFRESH — same path as the widget's manual refresh button. */
    fun refresh() {
        context.sendBroadcast(
            Intent(context, CoinflowWidgetProvider::class.java).apply {
                action = "com.leeam.cryptowidget.ACTION_REFRESH"
            }
        )
    }
}
