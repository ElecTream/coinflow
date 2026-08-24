package com.electream.cryptowidget.data.repository

import com.electream.cryptowidget.data.local.CustomCoinDao
import com.electream.cryptowidget.data.local.CustomCoinEntity
import com.electream.cryptowidget.data.local.toCoinDefinition
import com.electream.cryptowidget.data.model.CoinDefinition
import com.electream.cryptowidget.data.model.CoinRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepositoryImpl @Inject constructor(
    private val customCoinDao: CustomCoinDao
) : CoinRepository {

    override fun allCoins(): Flow<List<CoinDefinition>> =
        customCoinDao.getAllCustomCoins().map { customCoins ->
            CoinRegistry.all + customCoins.map { it.toCoinDefinition() }
        }

    override suspend fun coinById(id: String): CoinDefinition? {
        // Built-in coins take priority; fall back to Room lookup for BYOC coins.
        val builtin = CoinRegistry.all.firstOrNull { it.id == id }
        if (builtin != null) return builtin
        return customCoinDao.getCustomCoinById(id)?.toCoinDefinition()
    }

    override suspend fun addCustomCoin(entity: CustomCoinEntity) {
        customCoinDao.insertCustomCoin(entity)
    }

    override suspend fun deleteCustomCoin(id: String) {
        customCoinDao.deleteCustomCoinById(id)
    }
}
