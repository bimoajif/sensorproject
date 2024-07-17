package com.bimoajif.sensorproject.database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sensor_reading")
data class SensorEntity(
    @PrimaryKey val id: Float,
    @ColumnInfo(name = "timestamp") val timestamp: Long?,
    @ColumnInfo(name = "pitch") val pitch: Long?,
    @ColumnInfo(name = "pitch_offset") val pitchOffset: Double?,
    @ColumnInfo(name = "yaw") val yaw: Long?,
    @ColumnInfo(name = "magneto_x") val magnetoX: Float?,
    @ColumnInfo(name = "magneto_y") val magnetoY: Float?,
    @ColumnInfo(name = "magneto_z") val magnetoZ: Float?,
)

@Entity(tableName = "ui_colors")
data class UiColorsEntity(
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "last_modified") val lastModified: Date
)