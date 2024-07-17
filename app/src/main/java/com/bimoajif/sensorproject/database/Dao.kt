package com.bimoajif.sensorproject.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SensorDao {
    @Query("SELECT * FROM sensor_reading")
    fun getAll(): List<SensorEntity>

    @Query("SELECT * FROM sensor_reading WHERE id = :id")
    fun getById(id: String): SensorEntity

    @Query("DELETE FROM sensor_reading")
    fun deleteAll()

    @Insert
    fun insertAll(vararg sensors: SensorEntity)

    @Delete
    fun delete(sensor: SensorEntity)
}

@Dao
interface UiColorsDao {
    @Query("SELECT * FROM ui_colors")
    fun getAll(): List<UiColorsEntity>

    @Update
    fun update(uiColor: UiColorsEntity)
}