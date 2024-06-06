package com.bimoajif.sensorproject

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var rotationVector: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationVectorReading = FloatArray(4)

    private val accelerometerReadingFiltered = FloatArray(3)
    private val magnetometerReadingFiltered = FloatArray(3)
    private val rotationVectorReadingFiltered = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val alpha = 0.8f

    private lateinit var pitchTextView: TextView
    private lateinit var rollTextView: TextView
    private lateinit var yawTextView: TextView

    private lateinit var magXTextView: TextView
    private lateinit var magYTextView: TextView
    private lateinit var magZTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pitchTextView = findViewById(R.id.pitch)
        rollTextView = findViewById(R.id.roll)
        yawTextView = findViewById(R.id.yaw)
        magXTextView = findViewById(R.id.magnetic_x)
        magYTextView = findViewById(R.id.magnetic_y)
        magZTextView = findViewById(R.id.magnetic_z)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        magnetometer?.also { magnetometer ->
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        rotationVector?.also { rotationVector ->
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
//                filter(event.values, accelerometerReading, accelerometerReadingFiltered)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
//                filter(event.values, magnetometerReading, magnetometerReadingFiltered)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                System.arraycopy(event.values, 0, rotationVectorReading, 0, rotationVectorReading.size)
//                filter(event.values, rotationVectorReading, rotationVectorReadingFiltered)
            }
        }

        if(rotationVectorReading.isNotEmpty()) {
            Log.d("rotation_reading_x", "x: ${rotationVectorReading[0]}")
            Log.d("rotation_reading_y", "y: ${rotationVectorReading[1]}")
            Log.d("rotation_reading_z", "z: ${rotationVectorReading[2]}")
        }


        if (accelerometerReading.isNotEmpty() && magnetometerReading.isNotEmpty()) {
            Log.d("magneto_reading_x", "x: ${magnetometerReading[0]}")
            Log.d("magneto_reading_y", "y: ${magnetometerReading[1]}")
            Log.d("magneto_reading_z", "z: ${magnetometerReading[2]}")

            Log.d("acc_reading_z", "z: ${accelerometerReading[2]}")

//            magnetometerReading[1] = magnetometerReading[2].also { magnetometerReading[2] = magnetometerReading[1] }

            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

//            Log.i("inclination_matrix", SensorManager.getInclination(inclinationMatrix).toString())

            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, rotationMatrix)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Convert radians to degrees
            var pitch = (abs(Math.toDegrees(orientationAngles[1].toDouble()))).toFloat().roundToLong()
            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat().roundToInt()
            var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat().roundToLong()

            if (azimuth < 0) {
                azimuth += 360
            }

            if (accelerometerReading[2] < 0) {
                pitch *= -1
            }

            pitchTextView.text = "Pitch: $pitch"
            rollTextView.text = "Roll: $roll"
            yawTextView.text = "Yaw: $azimuth"
            magXTextView.text = "x: ${magnetometerReading[0]}"
            magYTextView.text = "y: ${magnetometerReading[1]}"
            magZTextView.text = "z: ${magnetometerReading[2]}"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something if sensor accuracy changes
    }

    private fun lowPassFilter(input: FloatArray, output: FloatArray, filteredOutput: FloatArray) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        for(i in input.indices) {
            filteredOutput[i] = alpha * output[i] + (1 - alpha) * input[i]
            output[i] = filteredOutput[i]
//            filteredOutput[i] = output[i] + alpha * (input[i] - output[i])
//            output[i] = filteredOutput[i]
        }
    }
}