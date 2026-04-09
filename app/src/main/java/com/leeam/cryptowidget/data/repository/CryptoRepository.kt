package com.leeam.cryptowidget.data.repository

import com.leeam.cryptowidget.data.model.WidgetData

interface CryptoRepository {
    suspend fun fetchWidgetData(coinId: String, walletAddress: String): Result<WidgetData>
    suspend fun fetchSparkline(coinId: String): Result<List<Double>>
    suspend fun fetchWalletBalance(coinId: String, address: String): Result<Double>
}
