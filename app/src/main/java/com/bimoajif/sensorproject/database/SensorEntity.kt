package com.bimoajif.sensorproject.database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_reading")
data class SensorEntity(
    @PrimaryKey val id: Float,
    @ColumnInfo(name = "magneto_x") val magnetoX: Float?,
    @ColumnInfo(name = "magneto_y") val magnetoY: Float?,
    @ColumnInfo(name = "magneto_z") val magnetoZ: Float?,
)
