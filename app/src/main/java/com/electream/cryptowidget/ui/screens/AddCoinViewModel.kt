package com.electream.cryptowidget.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.electream.cryptowidget.data.local.CustomCoinEntity
import com.electream.cryptowidget.data.local.WidgetPreferences
import com.electream.cryptowidget.data.remote.KrakenService
import com.electream.cryptowidget.data.repository.CoinRepository
import com.electream.cryptowidget.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddCoinMode { KRAKEN, CUSTOM }

data class KrakenPairUi(
    val pairId: String,   // e.g. "XBTUSD" — stored as krakenPair in Room
    val wsName: String,   // e.g. "XBT/USD" — shown in search list
    val base: String      // e.g. "XXBT"
)

data class AddCoinUiState(
    val mode: AddCoinMode = AddCoinMode.KRAKEN,
    // Kraken search
    val pairsLoading: Boolean = false,
    val pairsError: String? = null,
    val allPairs: List<KrakenPairUi> = emptyList(),
    val searchQuery: String = "",
    val selectedPair: KrakenPairUi? = null,
    // Common name fields
    val coinSymbol: String = "",
    val coinDisplayName: String = "",
    // Custom REST price fields
    val priceUrl: String = "",
    val priceJsonPath: String = "",
    val changeUrl: String = "",
    val changeJsonPath: String = "",
    val sparklineUrl: String = "",
    val sparklineJsonPath: String = "",
    // Optional wallet fields (both modes)
    val walletUrlTemplate: String = "",
    val walletJsonPath: String = "",
    val walletDivisor: String = "1.0",
    val showWalletSection: Boolean = false,
    // Feedback
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class AddCoinViewModel @Inject constructor(
    private val coinRepository: CoinRepository,
    private val widgetPrefs: WidgetPreferences,
    private val workScheduler: WorkScheduler,
    private val krakenService: KrakenService
) : ViewModel() {

    private val _state = MutableStateFlow(AddCoinUiState())
    val state: StateFlow<AddCoinUiState> = _state.asStateFlow()

    /** Filtered + sorted pair list derived from state — used by the search LazyColumn. */
    val filteredPairs: StateFlow<List<KrakenPairUi>> = _state
        .map { s ->
            val q = s.searchQuery.trim().lowercase()
            val source = if (q.isEmpty()) s.allPairs.take(60)
            else s.allPairs.filter {
                it.pairId.lowercase().contains(q) || it.wsName.lowercase().contains(q)
            }.take(100)
            source
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadKrakenPairs()
    }

    private fun loadKrakenPairs() = viewModelScope.launch {
        _state.update { it.copy(pairsLoading = true, pairsError = null) }
        try {
            val response = krakenService.getAssetPairs()
            val pairs = response.result.entries
                .filter { (_, info) -> info.wsname.isNotEmpty() }
                .map { (pairId, info) ->
                    KrakenPairUi(pairId = pairId, wsName = info.wsname, base = info.base)
                }
                .sortedWith(
                    // USD pairs first, then EUR, then everything else alphabetically
                    compareByDescending<KrakenPairUi> {
                        when {
                            it.wsName.endsWith("/USD") -> 2
                            it.wsName.endsWith("/EUR") -> 1
                            else -> 0
                        }
                    }.thenBy { it.wsName }
                )
            _state.update { it.copy(pairsLoading = false, allPairs = pairs) }
        } catch (e: Exception) {
            _state.update { it.copy(pairsLoading = false, pairsError = e.message ?: "Failed to load pairs") }
        }
    }

    fun retryLoadPairs() = loadKrakenPairs()

    fun onModeChange(mode: AddCoinMode) =
        _state.update { it.copy(mode = mode, selectedPair = null, coinSymbol = "", coinDisplayName = "") }

    fun onSearchQueryChange(q: String) = _state.update { it.copy(searchQuery = q) }

    fun onPairSelected(pair: KrakenPairUi) {
        val parts = pair.wsName.split("/")
        _state.update {
            it.copy(
                selectedPair    = pair,
                coinSymbol      = parts.firstOrNull() ?: pair.pairId,
                coinDisplayName = pair.wsName
            )
        }
    }

    fun onPairCleared() =
        _state.update { it.copy(selectedPair = null, coinSymbol = "", coinDisplayName = "", searchQuery = "") }

    fun onSymbolChange(v: String)       = _state.update { it.copy(coinSymbol = v) }
    fun onDisplayNameChange(v: String)  = _state.update { it.copy(coinDisplayName = v) }
    fun onPriceUrlChange(v: String)     = _state.update { it.copy(priceUrl = v) }
    fun onPricePathChange(v: String)    = _state.update { it.copy(priceJsonPath = v) }
    fun onChangeUrlChange(v: String)    = _state.update { it.copy(changeUrl = v) }
    fun onChangePathChange(v: String)   = _state.update { it.copy(changeJsonPath = v) }
    fun onSparklineUrlChange(v: String) = _state.update { it.copy(sparklineUrl = v) }
    fun onSparklinePathChange(v: String)= _state.update { it.copy(sparklineJsonPath = v) }
    fun onWalletUrlChange(v: String)    = _state.update { it.copy(walletUrlTemplate = v) }
    fun onWalletPathChange(v: String)   = _state.update { it.copy(walletJsonPath = v) }
    fun onWalletDivisorChange(v: String)= _state.update { it.copy(walletDivisor = v) }
    fun toggleWalletSection()           = _state.update { it.copy(showWalletSection = !it.showWalletSection) }
    fun clearError()                    = _state.update { it.copy(saveError = null) }

    fun save(onSuccess: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        val symbol      = s.coinSymbol.trim().uppercase()
        val displayName = s.coinDisplayName.trim()

        // Validation
        if (symbol.isBlank()) {
            _state.update { it.copy(saveError = "Symbol is required") }; return@launch
        }
        if (displayName.isBlank()) {
            _state.update { it.copy(saveError = "Display name is required") }; return@launch
        }
        if (s.mode == AddCoinMode.KRAKEN && s.selectedPair == null) {
            _state.update { it.copy(saveError = "Select a Kraken pair first") }; return@launch
        }
        if (s.mode == AddCoinMode.CUSTOM && s.priceUrl.isBlank()) {
            _state.update { it.copy(saveError = "Price URL is required") }; return@launch
        }
        if (s.mode == AddCoinMode.CUSTOM && s.priceJsonPath.isBlank()) {
            _state.update { it.copy(saveError = "Price JSON path is required") }; return@launch
        }

        _state.update { it.copy(isSaving = true, saveError = null) }

        val id = "${symbol.lowercase()}_${System.currentTimeMillis()}"

        val entity = CustomCoinEntity(
            id          = id,
            symbol      = symbol,
            displayName = displayName,
            krakenPair  = if (s.mode == AddCoinMode.KRAKEN) s.selectedPair?.pairId else null,
            customPriceUrl          = if (s.mode == AddCoinMode.CUSTOM) s.priceUrl.trim() else null,
            customPriceJsonPath     = if (s.mode == AddCoinMode.CUSTOM) s.priceJsonPath.trim() else null,
            customChangeUrl         = if (s.mode == AddCoinMode.CUSTOM) s.changeUrl.trim().ifBlank { null } else null,
            customChangeJsonPath    = if (s.mode == AddCoinMode.CUSTOM) s.changeJsonPath.trim().ifBlank { null } else null,
            customSparklineUrl      = if (s.mode == AddCoinMode.CUSTOM) s.sparklineUrl.trim().ifBlank { null } else null,
            customSparklineJsonPath = if (s.mode == AddCoinMode.CUSTOM) s.sparklineJsonPath.trim().ifBlank { null } else null,
            walletBalanceUrlTemplate = s.walletUrlTemplate.trim().ifBlank { null },
            walletBalanceJsonPath    = s.walletJsonPath.trim().ifBlank { null },
            walletBalanceDivisor     = s.walletDivisor.toDoubleOrNull() ?: 1.0
        )

        try {
            coinRepository.addCustomCoin(entity)
            // Auto-follow the newly added coin so it surfaces in the live tracker immediately.
            val current = widgetPrefs.followedCoinIds.first()
            if (id !in current) widgetPrefs.setFollowedCoinIds(current + id)
            // Kick off an immediate background fetch so the user doesn't have to wait
            // for the next periodic worker cycle to see live data.
            workScheduler.triggerImmediateRefresh()
            _state.update { it.copy(isSaving = false) }
            onSuccess()
        } catch (e: Exception) {
            _state.update { it.copy(isSaving = false, saveError = e.message ?: "Save failed") }
        }
    }
}
