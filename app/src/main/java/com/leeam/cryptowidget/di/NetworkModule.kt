package com.leeam.cryptowidget.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.leeam.cryptowidget.data.remote.BitcoinService
import com.leeam.cryptowidget.data.remote.EthereumService
import com.leeam.cryptowidget.data.remote.KrakenService
import com.leeam.cryptowidget.data.remote.SolanaService
import com.leeam.cryptowidget.data.remote.XrplService
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
        encodeDefaults = true   // required: ensures default fields like XrplRequest.method are serialized
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Coinflow/1.0 Android")
                        .header("Accept", "application/json")
                        .build()
                )
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    // ── Retrofit instances ────────────────────────────────────────────────────

    @Provides @Singleton @Named("kraken")
    fun provideKrakenRetrofit(client: OkHttpClient): Retrofit = retrofit("https://api.kraken.com/", client)

    @Provides @Singleton @Named("xrpl")
    fun provideXrplRetrofit(client: OkHttpClient): Retrofit = retrofit("https://xrplcluster.com/", client)

    @Provides @Singleton @Named("bitcoin")
    fun provideBitcoinRetrofit(client: OkHttpClient): Retrofit = retrofit("https://blockstream.info/", client)

    @Provides @Singleton @Named("ethereum")
    fun provideEthereumRetrofit(client: OkHttpClient): Retrofit = retrofit("https://eth.llamarpc.com/", client)

    @Provides @Singleton @Named("solana")
    fun provideSolanaRetrofit(client: OkHttpClient): Retrofit = retrofit("https://api.mainnet-beta.solana.com/", client)

    // ── Service instances ─────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideKrakenService(@Named("kraken") r: Retrofit): KrakenService = r.create(KrakenService::class.java)

    @Provides @Singleton
    fun provideXrplService(@Named("xrpl") r: Retrofit): XrplService = r.create(XrplService::class.java)

    @Provides @Singleton
    fun provideBitcoinService(@Named("bitcoin") r: Retrofit): BitcoinService = r.create(BitcoinService::class.java)

    @Provides @Singleton
    fun provideEthereumService(@Named("ethereum") r: Retrofit): EthereumService = r.create(EthereumService::class.java)

    @Provides @Singleton
    fun provideSolanaService(@Named("solana") r: Retrofit): SolanaService = r.create(SolanaService::class.java)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
