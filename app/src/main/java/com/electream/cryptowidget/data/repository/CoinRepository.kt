package com.electream.cryptowidget.data.repository

import com.electream.cryptowidget.data.local.CustomCoinEntity
import com.electream.cryptowidget.data.model.CoinDefinition
import kotlinx.coroutines.flow.Flow

/**
 * Provides the canonical list of all available coins — built-in [CoinDefinition]s
 * from [com.electream.cryptowidget.data.model.CoinRegistry] merged with any user-added
 * coins stored in Room.
 */
interface CoinRepository {
    /** Emits the full list (built-in + custom) whenever the custom-coin table changes. */
    fun allCoins(): Flow<List<CoinDefinition>>

    /** Returns a single coin by ID, checking built-ins first then Room. Null if not found. */
    suspend fun coinById(id: String): CoinDefinition?

    suspend fun addCustomCoin(entity: CustomCoinEntity)
    suspend fun deleteCustomCoin(id: String)
}
