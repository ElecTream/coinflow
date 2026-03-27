package com.leeam.cryptowidget.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leeam.cryptowidget.data.local.AlertDirection
import com.leeam.cryptowidget.data.local.AlertEntity
import com.leeam.cryptowidget.data.local.AlertMode
import com.leeam.cryptowidget.data.local.AlertRepository
import com.leeam.cryptowidget.data.local.AppTheme
import com.leeam.cryptowidget.data.local.ChartStyle
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
    val chartStyle: ChartStyle = ChartStyle.LINE,
    val alerts: List<AlertEntity> = emptyList(),
    val newAlertDirection: AlertDirection = AlertDirection.ABOVE,
    val newAlertThreshold: String = "",
    val newAlertMode: AlertMode = AlertMode.CROSSING,
    val newAlertCooldownMin: Int = 60,
    val appTheme: AppTheme = AppTheme.CYBER,
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
        loadTheme()
        fetchLivePrice()
        collectAlerts()
        collectDiagnostics()
    }

    private fun loadPreferences() = viewModelScope.launch {
        combine(
            widgetPrefs.coinId,
            widgetPrefs.walletAddress,
            widgetPrefs.refreshIntervalMin,
            widgetPrefs.showSparkline,
            widgetPrefs.chartStyle
        ) { coinId, wallet, interval, sparkline, chart ->
            _state.update {
                it.copy(
                    coinId = coinId,
                    walletAddress = wallet,
                    refreshIntervalMin = interval,
                    showSparkline = sparkline,
                    chartStyle = chart
                )
            }
        }.collect()
    }

    private fun loadTheme() = viewModelScope.launch {
        widgetPrefs.appTheme.collect { theme ->
            _state.update { it.copy(appTheme = theme) }
        }
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

    /** Returns a human-readable error if the address is structurally invalid, null if it looks OK. */
    private fun validateXrpAddress(address: String): String? {
        if (!address.startsWith("r")) return "Must start with 'r'"
        if (address.length < 25 || address.length > 34)
            return "Length must be 25–34 chars (yours: ${address.length})"
        // Base58Check alphabet — excludes 0, O, I, l
        val invalidChars = address.filter { it !in "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz" }
        if (invalidChars.isNotEmpty()) return "Invalid character(s): $invalidChars"
        return null
    }

    fun testWallet() {
        val address = _state.value.walletAddress.trim()
        if (address.isBlank()) {
            _state.update { it.copy(walletTestResult = "Enter a wallet address first") }
            return
        }
        val formatError = validateXrpAddress(address)
        if (formatError != null) {
            _state.update { it.copy(walletTestResult = "Bad address: $formatError") }
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
                thresholdUsd = threshold,
                alertMode = _state.value.newAlertMode,
                cooldownMin = _state.value.newAlertCooldownMin
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
    fun onChartStyleChange(v: ChartStyle) = _state.update { it.copy(chartStyle = v) }
    fun onNewAlertDirection(d: AlertDirection) = _state.update { it.copy(newAlertDirection = d) }
    fun onNewAlertThreshold(v: String) = _state.update { it.copy(newAlertThreshold = v) }
    fun onNewAlertMode(m: AlertMode) = _state.update { it.copy(newAlertMode = m) }
    fun onNewAlertCooldown(min: Int) = _state.update { it.copy(newAlertCooldownMin = min) }
    fun onThemeChange(theme: AppTheme) {
        _state.update { it.copy(appTheme = theme) }
        viewModelScope.launch { widgetPrefs.setAppTheme(theme) }
    }

    fun save() = viewModelScope.launch {
        try {
            val s = _state.value
            widgetPrefs.setWalletAddress(s.walletAddress.trim())
            widgetPrefs.setRefreshInterval(s.refreshIntervalMin)
            widgetPrefs.setShowSparkline(s.showSparkline)
            widgetPrefs.setChartStyle(s.chartStyle)
            workScheduler.schedulePeriodicRefresh(s.refreshIntervalMin)
            workScheduler.triggerImmediateRefresh()
            _state.update { it.copy(saveSuccess = true, saveError = null) }
        } catch (e: Exception) {
            _state.update { it.copy(saveError = e.message ?: "Save failed") }
        }
    }

    fun clearFeedback() = _state.update { it.copy(saveSuccess = false, saveError = null) }
}
