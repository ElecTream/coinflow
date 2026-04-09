package com.leeam.cryptowidget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [AlertEntity::class, CustomCoinEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(AlertDatabase.Converters::class)
abstract class AlertDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao
    abstract fun customCoinDao(): CustomCoinDao

    class Converters {
        @TypeConverter
        fun fromDirection(d: AlertDirection): String = d.name

        @TypeConverter
        fun toDirection(s: String): AlertDirection = AlertDirection.valueOf(s)

        @TypeConverter
        fun fromMode(m: AlertMode): String = m.name

        @TypeConverter
        fun toMode(s: String): AlertMode = AlertMode.valueOf(s)
    }
}
