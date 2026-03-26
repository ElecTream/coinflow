package com.leeam.cryptowidget.data.remote

import com.leeam.cryptowidget.data.model.CoinListItem
import com.leeam.cryptowidget.data.model.MarketChartResponse
import com.leeam.cryptowidget.data.model.SimplePriceResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoService {

    @GET("api/v3/simple/price")
    suspend fun getSimplePrice(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String,
        @Query("include_24hr_change") include24hrChange: Boolean = true
    ): SimplePriceResponse

    @GET("api/v3/coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id") coinId: String,
        @Query("vs_currency") vsCurrency: String,
        @Query("days") days: Int,
        @Query("interval") interval: String = "hourly"
    ): MarketChartResponse

    @GET("api/v3/coins/list")
    suspend fun getCoinList(): List<CoinListItem>
}
