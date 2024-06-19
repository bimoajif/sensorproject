package com.bimoajif.sensorproject.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SensorDao {
    @Query("SELECT * FROM sensor_reading")
    fun getAll(): List<SensorEntity>

    @Query("DELETE FROM sensor_reading")
    fun deleteAll()

    @Insert
    fun insertAll(vararg sensors: SensorEntity)

    @Delete
    fun delete(sensor: SensorEntity)
}