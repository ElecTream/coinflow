package com.electream.cryptowidget.data.remote

import com.electream.cryptowidget.data.model.KrakenAssetPairsResponse
import com.electream.cryptowidget.data.model.KrakenOhlcResponse
import com.electream.cryptowidget.data.model.KrakenTickerResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface KrakenService {

    @GET("0/public/Ticker")
    suspend fun getTicker(
        @Query("pair") pair: String
    ): KrakenTickerResponse

    @GET("0/public/OHLC")
    suspend fun getOhlc(
        @Query("pair") pair: String,
        @Query("interval") interval: Int = 60  // 60 = hourly candles
    ): KrakenOhlcResponse

    /** Returns all tradable pairs (~700+). Called once; results cached in AddCoinViewModel. */
    @GET("0/public/AssetPairs")
    suspend fun getAssetPairs(): KrakenAssetPairsResponse
}
