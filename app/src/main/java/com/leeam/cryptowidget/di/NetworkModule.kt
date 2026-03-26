package com.leeam.cryptowidget.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.leeam.cryptowidget.data.remote.CoinGeckoService
import com.leeam.cryptowidget.data.remote.XrpScanService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "CryptoWidget/1.0 Android")
                        .header("Accept", "application/json")
                        .build()
                )
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("coingecko")
    fun provideCoinGeckoRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named("xrpscan")
    fun provideXrpScanRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.xrpscan.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideCoinGeckoService(@Named("coingecko") retrofit: Retrofit): CoinGeckoService =
        retrofit.create(CoinGeckoService::class.java)

    @Provides
    @Singleton
    fun provideXrpScanService(@Named("xrpscan") retrofit: Retrofit): XrpScanService =
        retrofit.create(XrpScanService::class.java)
}
