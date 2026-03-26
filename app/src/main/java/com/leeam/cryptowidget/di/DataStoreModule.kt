package com.leeam.cryptowidget.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// WidgetPreferences is @Singleton with @Inject constructor — Hilt provides it automatically.
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule
