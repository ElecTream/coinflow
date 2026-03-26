package com.leeam.cryptowidget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [AlertEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AlertDatabase.Converters::class)
abstract class AlertDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao

    class Converters {
        @TypeConverter
        fun fromDirection(d: AlertDirection): String = d.name

        @TypeConverter
        fun toDirection(s: String): AlertDirection = AlertDirection.valueOf(s)
    }
}
