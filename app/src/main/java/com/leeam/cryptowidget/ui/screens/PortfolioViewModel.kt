package com.leeam.cryptowidget.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.data.model.CoinDefinition
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.model.WalletConfig
import com.leeam.cryptowidget.data.repository.CoinRepository
import com.leeam.cryptowidget.data.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class PortfolioRow(
    val coin: CoinDefinition,
    val priceUsd: Double,
    val change24hPct: Double,
    val walletAddress: String,
    val balance: Double,
    val valueUsd: Double,
    val updatedMs: Long
) {
    val hasWallet: Boolean get() = coin.walletConfig !is WalletConfig.None && walletAddress.isNotBlank() && balance > 0.0
}

data class PortfolioUiState(
    val rows: List<PortfolioRow> = emptyList(),
    val totalValueUsd: Double = 0.0,
    val totalChange24hUsd: Double = 0.0,
    val totalChange24hPct: Double = 0.0,
    val isRefreshing: Boolean = false,
    val refreshError: String? = null
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val widgetPrefs: WidgetPreferences,
    private val cryptoRepository: CryptoRepository,
    coinRepository: CoinRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PortfolioUiState())
    val state: StateFlow<PortfolioUiState> = _state.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rowsFlow = combine(
        widgetPrefs.followedCoinIds,
        coinRepository.allCoins()
    ) { ids, coins -> ids to coins }
        .flatMapLatest { (ids, allCoins) ->
            val coinsById = allCoins.associateBy { it.id }
            val orderedCoins = ids.mapNotNull { coinsById[it] ?: CoinRegistry.all.firstOrNull { c -> c.id == it } }

            if (orderedCoins.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                val perCoinFlows = orderedCoins.map { coin ->
                    combine(
                        widgetPrefs.priceUsdFor(coin.id),
                        widgetPrefs.changePctFor(coin.id),
                        widgetPrefs.walletAddressFor(coin.id),
                        widgetPrefs.balanceFor(coin.id),
                        widgetPrefs.updatedMsFor(coin.id)
                    ) { price, change, wallet, balance, updated ->
                        PortfolioRow(
                            coin          = coin,
                            priceUsd      = price,
                            change24hPct  = change,
                            walletAddress = wallet,
                            balance       = balance,
                            valueUsd      = balance * price,
                            updatedMs     = updated
                        )
                    }
                }
                combine(perCoinFlows) { it.toList() }
            }
        }

    init {
        viewModelScope.launch {
            rowsFlow.collect { rows ->
                val totalValue   = rows.sumOf { it.valueUsd }
                // Compute portfolio 24h change in USD using each coin's price 24h ago.
                val totalDeltaUsd = rows.sumOf { row ->
                    if (row.valueUsd <= 0.0) 0.0
                    else {
                        val priorPrice = if (row.change24hPct == 0.0) row.priceUsd
                                         else row.priceUsd / (1.0 + row.change24hPct / 100.0)
                        row.balance * (row.priceUsd - priorPrice)
                    }
                }
                val priorTotal = totalValue - totalDeltaUsd
                val totalPct   = if (priorTotal > 0.0) (totalDeltaUsd / priorTotal) * 100.0 else 0.0

                _state.update {
                    it.copy(
                        rows               = rows,
                        totalValueUsd      = totalValue,
                        totalChange24hUsd  = totalDeltaUsd,
                        totalChange24hPct  = totalPct
                    )
                }
            }
        }

        // Auto-refresh on first open if data is stale or missing.
        viewModelScope.launch {
            val ids = widgetPrefs.followedCoinIds.first()
            val anyStale = ids.any { id ->
                val ts = widgetPrefs.updatedMsFor(id).first()
                ts == 0L || (System.currentTimeMillis() - ts) > 5 * 60_000L
            }
            if (anyStale) refresh()
        }
    }

    /** Re-fetch price + balance for every followed coin and persist to per-coin cache. */
    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, refreshError = null) }
            val rows = _state.value.rows
            val errors = mutableListOf<String>()
            try {
                coroutineScope {
                    rows.map { row ->
                        async {
                            cryptoRepository.fetchWidgetData(row.coin.id, row.walletAddress).fold(
                                onSuccess = { data ->
                                    widgetPrefs.cacheCoinData(
                                        coinId              = row.coin.id,
                                        priceUsd            = data.priceUsd,
                                        changePct           = data.change24hPct,
                                        balance             = data.walletBalance,
                                        sparkline           = data.sparklinePrices,
                                        sparklineTimestamps = data.sparklineTimestamps
                                    )
                                },
                                onFailure = { e ->
                                    errors.add("${row.coin.symbol}: ${e.message ?: "fetch failed"}")
                                }
                            )
                        }
                    }.awaitAll()
                }
            } catch (e: Exception) {
                errors.add(e.message ?: "Refresh failed")
            }
            _state.update {
                it.copy(
                    isRefreshing = false,
                    refreshError = if (errors.isEmpty()) null else errors.joinToString(" · ")
                )
            }
        }
    }
}
