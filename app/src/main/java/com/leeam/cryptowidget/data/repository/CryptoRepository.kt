package com.leeam.cryptowidget.data.repository

import com.leeam.cryptowidget.data.model.CryptoWidgetData

interface CryptoRepository {
    suspend fun fetchWidgetData(coinId: String, walletAddress: String): Result<CryptoWidgetData>
    suspend fun fetchSparkline(coinId: String): Result<List<Double>>
    suspend fun fetchWalletBalance(address: String): Result<Double>
}
