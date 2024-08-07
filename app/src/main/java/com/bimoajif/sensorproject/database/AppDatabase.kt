package com.bimoajif.sensorproject.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SensorEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
}