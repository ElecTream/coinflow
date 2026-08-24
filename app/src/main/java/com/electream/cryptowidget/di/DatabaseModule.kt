package com.electream.cryptowidget.di

import android.content.Context
import androidx.room.Room
import com.electream.cryptowidget.data.local.AlertDao
import com.electream.cryptowidget.data.local.AlertDatabase
import com.electream.cryptowidget.data.local.CustomCoinDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAlertDatabase(@ApplicationContext context: Context): AlertDatabase =
        Room.databaseBuilder(context, AlertDatabase::class.java, "crypto_alerts.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideAlertDao(db: AlertDatabase): AlertDao = db.alertDao()

    @Provides
    fun provideCustomCoinDao(db: AlertDatabase): CustomCoinDao = db.customCoinDao()
}
