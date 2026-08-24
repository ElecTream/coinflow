package com.electream.cryptowidget.di

import com.electream.cryptowidget.data.repository.CoinRepository
import com.electream.cryptowidget.data.repository.CoinRepositoryImpl
import com.electream.cryptowidget.data.repository.CryptoRepository
import com.electream.cryptowidget.data.repository.CryptoRepositoryImpl
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
