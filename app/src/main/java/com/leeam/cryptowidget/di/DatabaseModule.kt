package com.leeam.cryptowidget.di

import android.content.Context
import androidx.room.Room
import com.leeam.cryptowidget.data.local.AlertDao
import com.leeam.cryptowidget.data.local.AlertDatabase
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAlertDao(db: AlertDatabase): AlertDao = db.alertDao()
}
