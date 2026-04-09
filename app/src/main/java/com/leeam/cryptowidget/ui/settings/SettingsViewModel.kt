package com.leeam.cryptowidget.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leeam.cryptowidget.data.local.AlertDirection
import com.leeam.cryptowidget.data.local.AlertEntity
import com.leeam.cryptowidget.data.local.AlertMode
import com.leeam.cryptowidget.data.local.AlertRepository
import com.leeam.cryptowidget.data.local.AppTheme
import com.leeam.cryptowidget.data.local.ChartStyle
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.model.WalletConfig
import com.leeam.cryptowidget.data.repository.CryptoRepository
import com.leeam.cryptowidget.ui.util.CoinFormatter
import com.leeam.cryptowidget.widget.CoinflowWidgetProvider
import com.leeam.cryptowidget.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val livePrice: Double? = null,
    val change24h: Double? = null,
    val priceLoading: Boolean = true,
    val priceError: String? = null,
    val coinId: String = CoinRegistry.default.id,
    val walletAddress: String = "",
    val walletTestResult: String? = null,
    val walletTestLoading: Boolean = false,
    val refreshIntervalMin: Int = 15,
    val showSparkline: Boolean = true,
    val chartStyle: ChartStyle = ChartStyle.AREA,
    val alerts: List<AlertEntity> = emptyList(),
    val newAlertDirection: AlertDirection = AlertDirection.ABOVE,
    val newAlertThreshold: String = "",
    val newAlertMode: AlertMode = AlertMode.CROSSING,
    val newAlertCooldownMin: Int = 60,
    val appTheme: AppTheme = AppTheme.CYBER,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val lastWorkerRunMs: Long = 0L,
    val lastErrorMsg: String? = null,
    val followedCoinIds: List<String> = CoinRegistry.all.map { it.id },
    val widgetCoinIds: List<String>   = listOf(CoinRegistry.default.id),
    val customAccentArgb: Int = 0xFF00D4FF.toInt(),
    val customSecondaryArgb: Int = 0xFF7B2FFF.toInt()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
        collectMultiCoinPrefs()
        collectCustomColors()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadPreferences() {
        // Load coinId + other scalar prefs
        viewModelScope.launch {
            combine(
                widgetPrefs.coinId,
                widgetPrefs.refreshIntervalMin,
                widgetPrefs.showSparkline,
                widgetPrefs.chartStyle
            ) { coinId, interval, sparkline, chart ->
                _state.update {
                    it.copy(
                        coinId = coinId,
                        refreshIntervalMin = interval,
                        showSparkline = sparkline,
                        chartStyle = chart
                    )
                }
            }.collect()
        }
        // Reactively reload wallet address when coin changes
        viewModelScope.launch {
            widgetPrefs.coinId
                .flatMapLatest { coinId -> widgetPrefs.walletAddressFor(coinId) }
                .collect { wallet ->
                    _state.update { it.copy(walletAddress = wallet, walletTestResult = null) }
                }
        }
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

    fun onCoinChange(coinId: String) {
        viewModelScope.launch {
            widgetPrefs.setCoinId(coinId)
            // walletAddress auto-reloads via the flatMapLatest in loadPreferences()
            fetchLivePrice()
        }
    }

    /** Returns a human-readable error for the wallet address, or null if it looks OK. */
    private fun validateWalletAddress(coinId: String, address: String): String? {
        val coin = CoinRegistry.byId(coinId)
        return when (coin.walletConfig) {
            is WalletConfig.Xrpl        -> validateXrplAddress(address)
            is WalletConfig.Bitcoin     -> if (Regex("^(1|3|bc1)[a-zA-HJ-NP-Z0-9]{25,62}$").matches(address)) null else "Invalid Bitcoin address"
            is WalletConfig.Ethereum    -> if (Regex("^0x[0-9a-fA-F]{40}$").matches(address)) null else "Must be 0x + 40 hex chars"
            is WalletConfig.Solana      -> if (Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$").matches(address)) null else "Invalid Solana address"
            is WalletConfig.GenericRest -> if (address.isNotBlank()) null else "Address required"
            is WalletConfig.None        -> null
        }
    }

    private fun validateXrplAddress(address: String): String? {
        if (!address.startsWith("r")) return "Must start with 'r'"
        if (address.length < 25 || address.length > 34)
            return "Length must be 25–34 chars (yours: ${address.length})"
        val invalidChars = address.filter { it !in "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz" }
        if (invalidChars.isNotEmpty()) return "Invalid character(s): $invalidChars"
        return null
    }

    fun testWallet() {
        val address = _state.value.walletAddress.trim()
        val coinId = _state.value.coinId
        if (address.isBlank()) {
            _state.update { it.copy(walletTestResult = "Enter a wallet address first") }
            return
        }
        val formatError = validateWalletAddress(coinId, address)
        if (formatError != null) {
            _state.update { it.copy(walletTestResult = "Bad address: $formatError") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(walletTestLoading = true, walletTestResult = null) }
            cryptoRepository.fetchWalletBalance(coinId, address).fold(
                onSuccess = { bal ->
                    val price = _state.value.livePrice ?: 0.0
                    val symbol = CoinRegistry.byId(coinId).symbol
                    _state.update {
                        it.copy(
                            walletTestLoading = false,
                            walletTestResult = "OK  ${CoinFormatter.formatAmount(bal, price)} $symbol ≈ ${CoinFormatter.formatValueUsd(bal * price)}"
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
        val coinId = _state.value.coinId
        val symbol = CoinRegistry.byId(coinId).symbol
        viewModelScope.launch {
            alertRepository.addAlert(
                coinId = coinId,
                symbol = symbol,
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

    fun onCustomAccentChange(argb: Int) {
        _state.update { it.copy(customAccentArgb = argb) }
        viewModelScope.launch { widgetPrefs.setCustomAccentArgb(argb) }
    }

    fun onCustomSecondaryChange(argb: Int) {
        _state.update { it.copy(customSecondaryArgb = argb) }
        viewModelScope.launch { widgetPrefs.setCustomSecondaryArgb(argb) }
    }

    fun save() = viewModelScope.launch {
        try {
            val s = _state.value
            widgetPrefs.setWalletAddress(s.coinId, s.walletAddress.trim())
            widgetPrefs.setRefreshInterval(s.refreshIntervalMin)
            widgetPrefs.setShowSparkline(s.showSparkline)
            widgetPrefs.setChartStyle(s.chartStyle)
            workScheduler.schedulePeriodicRefresh(s.refreshIntervalMin)
            context.sendBroadcast(
                Intent(context, CoinflowWidgetProvider::class.java).apply {
                    action = "com.leeam.cryptowidget.ACTION_REFRESH"
                }
            )
            _state.update { it.copy(saveSuccess = true, saveError = null) }
        } catch (e: Exception) {
            _state.update { it.copy(saveError = e.message ?: "Save failed") }
        }
    }

    fun clearFeedback() = _state.update { it.copy(saveSuccess = false, saveError = null) }

    private fun collectMultiCoinPrefs() {
        viewModelScope.launch {
            widgetPrefs.followedCoinIds.collect { ids ->
                _state.update { it.copy(followedCoinIds = ids) }
            }
        }
        viewModelScope.launch {
            widgetPrefs.widgetCoinIds.collect { ids ->
                _state.update { it.copy(widgetCoinIds = ids) }
            }
        }
    }

    private fun collectCustomColors() {
        viewModelScope.launch {
            widgetPrefs.customAccentArgb.collect { argb ->
                _state.update { it.copy(customAccentArgb = argb) }
            }
        }
        viewModelScope.launch {
            widgetPrefs.customSecondaryArgb.collect { argb ->
                _state.update { it.copy(customSecondaryArgb = argb) }
            }
        }
    }

    /** Adds or removes a coin from the followed list. */
    fun toggleFollow(coinId: String) = viewModelScope.launch {
        val current = _state.value.followedCoinIds.toMutableList()
        if (coinId in current) {
            current.remove(coinId)
            // Auto-remove from widget tabs too
            val widgetIds = _state.value.widgetCoinIds.filter { it != coinId }
            widgetPrefs.setWidgetCoinIds(widgetIds)
        } else {
            current.add(coinId)
        }
        widgetPrefs.setFollowedCoinIds(current)
    }

    /** Adds or removes a coin from the widget tab list (max 5, must be in followed list). */
    fun toggleWidgetCoin(coinId: String) = viewModelScope.launch {
        val current = _state.value.widgetCoinIds.toMutableList()
        if (coinId in current) {
            current.remove(coinId)
            // If we removed the active coin, switch to the first remaining tab
            if (coinId == _state.value.coinId && current.isNotEmpty()) {
                widgetPrefs.setCoinId(current.first())
            }
        } else if (current.size < 5) {
            current.add(coinId)
        }
        widgetPrefs.setWidgetCoinIds(current)
    }
}
