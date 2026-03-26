package com.leeam.cryptowidget.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leeam.cryptowidget.data.local.AlertDirection
import com.leeam.cryptowidget.data.local.AlertEntity
import com.leeam.cryptowidget.data.local.AlertRepository
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.data.repository.CryptoRepository
import com.leeam.cryptowidget.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val livePrice: Double? = null,
    val change24h: Double? = null,
    val priceLoading: Boolean = true,
    val priceError: String? = null,
    val coinId: String = "ripple",
    val walletAddress: String = "",
    val walletTestResult: String? = null,
    val walletTestLoading: Boolean = false,
    val refreshIntervalMin: Int = 15,
    val showSparkline: Boolean = true,
    val alerts: List<AlertEntity> = emptyList(),
    val newAlertDirection: AlertDirection = AlertDirection.ABOVE,
    val newAlertThreshold: String = "",
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val lastWorkerRunMs: Long = 0L,
    val lastErrorMsg: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val widgetPrefs: WidgetPreferences,
    private val cryptoRepository: CryptoRepository,
    private val alertRepository: AlertRepository,
    private val workScheduler: WorkScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        loadPreferences()
        fetchLivePrice()
        collectAlerts()
        collectDiagnostics()
    }

    private fun loadPreferences() = viewModelScope.launch {
        combine(
            widgetPrefs.coinId,
            widgetPrefs.walletAddress,
            widgetPrefs.refreshIntervalMin,
            widgetPrefs.showSparkline
        ) { coinId, wallet, interval, sparkline ->
            _state.update {
                it.copy(
                    coinId = coinId,
                    walletAddress = wallet,
                    refreshIntervalMin = interval,
                    showSparkline = sparkline
                )
            }
        }.collect()
    }

    fun fetchLivePrice() = viewModelScope.launch {
        _state.update { it.copy(priceLoading = true, priceError = null) }
        cryptoRepository.fetchWidgetData(_state.value.coinId, "").fold(
            onSuccess = { data ->
                _state.update {
                    it.copy(
                        livePrice = data.priceUsd,
                        change24h = data.change24hPct,
                        priceLoading = false
                    )
                }
            },
            onFailure = { err ->
                _state.update {
                    it.copy(priceLoading = false, priceError = err.message)
                }
            }
        )
    }

    fun testWallet() {
        val address = _state.value.walletAddress.trim()
        if (address.isBlank()) {
            _state.update { it.copy(walletTestResult = "Enter a wallet address first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(walletTestLoading = true, walletTestResult = null) }
            cryptoRepository.fetchWalletBalance(address).fold(
                onSuccess = { bal ->
                    val price = _state.value.livePrice ?: 0.0
                    _state.update {
                        it.copy(
                            walletTestLoading = false,
                            walletTestResult = String.format(
                                Locale.US, "OK  %.4f XRP ≈ \$%.2f", bal, bal * price
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            walletTestLoading = false,
                            walletTestResult = "Error: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    private fun collectAlerts() = viewModelScope.launch {
        alertRepository.getAllAlerts().collect { alerts ->
            _state.update { it.copy(alerts = alerts) }
        }
    }

    private fun collectDiagnostics() {
        viewModelScope.launch {
            widgetPrefs.lastWorkerRunMs.collect { ms ->
                _state.update { it.copy(lastWorkerRunMs = ms) }
            }
        }
        viewModelScope.launch {
            widgetPrefs.lastErrorMsg.collect { msg ->
                _state.update { it.copy(lastErrorMsg = msg) }
            }
        }
    }

    fun addAlert() {
        val threshold = _state.value.newAlertThreshold.toDoubleOrNull()
        if (threshold == null || threshold <= 0) {
            _state.update { it.copy(saveError = "Enter a valid price threshold") }
            return
        }
        viewModelScope.launch {
            alertRepository.addAlert(
                coinId = _state.value.coinId,
                symbol = "XRP",
                direction = _state.value.newAlertDirection,
                thresholdUsd = threshold
            )
            _state.update { it.copy(newAlertThreshold = "") }
        }
    }

    fun deleteAlert(alert: AlertEntity) = viewModelScope.launch {
        alertRepository.deleteAlert(alert)
    }

    fun toggleAlert(alert: AlertEntity, enabled: Boolean) = viewModelScope.launch {
        alertRepository.setAlertEnabled(alert.id, enabled)
    }

    fun onWalletChange(v: String) = _state.update { it.copy(walletAddress = v, walletTestResult = null) }
    fun onIntervalChange(v: Int) = _state.update { it.copy(refreshIntervalMin = v) }
    fun onShowSparklineChange(v: Boolean) = _state.update { it.copy(showSparkline = v) }
    fun onNewAlertDirection(d: AlertDirection) = _state.update { it.copy(newAlertDirection = d) }
    fun onNewAlertThreshold(v: String) = _state.update { it.copy(newAlertThreshold = v) }

    fun save() = viewModelScope.launch {
        try {
            val s = _state.value
            widgetPrefs.setWalletAddress(s.walletAddress.trim())
            widgetPrefs.setRefreshInterval(s.refreshIntervalMin)
            widgetPrefs.setShowSparkline(s.showSparkline)
            workScheduler.schedulePeriodicRefresh(s.refreshIntervalMin)
            workScheduler.triggerImmediateRefresh()
            _state.update { it.copy(saveSuccess = true, saveError = null) }
        } catch (e: Exception) {
            _state.update { it.copy(saveError = e.message ?: "Save failed") }
        }
    }

    fun clearFeedback() = _state.update { it.copy(saveSuccess = false, saveError = null) }
}
