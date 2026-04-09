package com.leeam.cryptowidget.di

import com.leeam.cryptowidget.data.repository.CoinRepository
import com.leeam.cryptowidget.data.repository.CoinRepositoryImpl
import com.leeam.cryptowidget.data.repository.CryptoRepository
import com.leeam.cryptowidget.data.repository.CryptoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCryptoRepository(impl: CryptoRepositoryImpl): CryptoRepository

    @Binds
    @Singleton
    abstract fun bindCoinRepository(impl: CoinRepositoryImpl): CoinRepository
}
