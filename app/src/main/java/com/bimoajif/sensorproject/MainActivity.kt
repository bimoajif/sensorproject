package com.bimoajif.sensorproject

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bimoajif.sensorproject.database.AppDatabase
import com.bimoajif.sensorproject.database.SensorDao
import com.bimoajif.sensorproject.database.SensorEntity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var rotationVector: Sensor? = null

    private lateinit var db: AppDatabase
    private lateinit var dao: SensorDao

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationVectorReading = FloatArray(4)

    private val accelerometerReadingFiltered = FloatArray(3)
    private val magnetometerReadingFiltered = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val rotationMatrixRemapped = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val orientationAnglesRemapped = FloatArray(3)

    private val alpha = 0.8f

    private lateinit var pitchTextView: TextView
    private lateinit var rollTextView: TextView
    private lateinit var yawTextView: TextView

    private lateinit var magXTextView: TextView
    private lateinit var magYTextView: TextView
    private lateinit var magZTextView: TextView

    private lateinit var lineChartView: LineChart

    private var lineYValues = ArrayList<Entry>()
    private var lineYDataSet: LineDataSet? = null

    private var lineZValues = ArrayList<Entry>()
    private var lineZDataSet: LineDataSet? = null

    private var lineData: LineData? = null

    private var i: Float = 0.0.toFloat()
    private val maxEntries = 250

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "sensor_reading").allowMainThreadQueries().build()
        dao = db.sensorDao()

        dao.deleteAll()

        pitchTextView = findViewById(R.id.pitch)
        rollTextView = findViewById(R.id.roll)
        yawTextView = findViewById(R.id.yaw)
        magXTextView = findViewById(R.id.magnetic_x)
        magYTextView = findViewById(R.id.magnetic_y)
        magZTextView = findViewById(R.id.magnetic_z)
        lineChartView = findViewById(R.id.chart)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        lineYDataSet = LineDataSet(lineYValues, "Magnetic Field Y")
        lineZDataSet = LineDataSet(lineZValues, "Magnetic Field Z")

        lineYDataSet?.setDrawCircles(false)
        lineZDataSet?.setDrawCircles(false)

        lineYDataSet?.color = getColor(R.color.colorPrimary)
        lineZDataSet?.color = getColor(R.color.colorAccent)

        lineData = LineData(lineYDataSet, lineZDataSet)
        lineChartView.data = lineData
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
//                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                lowPassFilter(event.values, accelerometerReading, accelerometerReadingFiltered)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
//                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                lowPassFilter(event.values, magnetometerReading, magnetometerReadingFiltered)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                System.arraycopy(event.values, 0, rotationVectorReading, 0, rotationVectorReading.size)
//                filter(event.values, rotationVectorReading, rotationVectorReadingFiltered)
            }
        }

        if (rotationVectorReading.isNotEmpty()) {
            Log.d("rotation_reading_x", "x: ${rotationVectorReading[0]}")
            Log.d("rotation_reading_y", "y: ${rotationVectorReading[1]}")
            Log.d("rotation_reading_z", "z: ${rotationVectorReading[2]}")
        }


        if (accelerometerReading.isNotEmpty() && magnetometerReading.isNotEmpty()) {
            Log.d("magneto_reading_x", "x: ${magnetometerReading[0]}")
            Log.d("magneto_reading_y", "y: ${magnetometerReading[1]}")
            Log.d("magneto_reading_z", "z: ${magnetometerReading[2]}")

            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, rotationMatrixRemapped)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            SensorManager.getOrientation(rotationMatrixRemapped, orientationAnglesRemapped)

            // Convert radians to degrees
            var pitch = (abs(Math.toDegrees(orientationAnglesRemapped[1].toDouble()))).toFloat().roundToLong()
            val roll = Math.toDegrees(orientationAnglesRemapped[2].toDouble()).toFloat().roundToInt()
            var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat().roundToLong()

//            azimuth -= 180
            if (azimuth < 0) {
                azimuth += 360
            }

            if (accelerometerReading[2] < 0) {
                pitch *= -1
            }

            if(accelerometerReading[1] < 0 && accelerometerReading[2] > 0) {
                pitch = 180 - pitch
            }

            setLineChartData(Entry(i, abs(magnetometerReadingFiltered[1])), Entry(i, abs(magnetometerReadingFiltered[2])))
            i += 1

            pitchTextView.text = "Pitch: $pitch"
            rollTextView.text = "Roll: $roll"
            yawTextView.text = "Yaw: $azimuth"
            magXTextView.text = "x: ${magnetometerReading[0]}"
            magYTextView.text = "y: ${magnetometerReading[1]}"
            magZTextView.text = "z: ${magnetometerReading[2]}"

            val sensor = SensorEntity(id = i, magnetoX = magnetometerReadingFiltered[0], magnetoY = magnetometerReadingFiltered[1], magnetoZ = magnetometerReadingFiltered[2])

            if (i < 500 && i > 100) {
                dao.insertAll(sensor)
            }
        }

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something if sensor accuracy changes
    }

    override fun onDestroy() {
        lineYValues.clear()
        lineZValues.clear()
        super.onDestroy()
    }

    private fun setLineChartData(entryY: Entry, entryZ: Entry) {
        if (lineYValues.size > maxEntries) {
            lineYValues.removeAt(0)
            lineZValues.removeAt(0)
        }


        lineYValues.add(entryY)
        lineZValues.add(entryZ)

        lineYDataSet?.notifyDataSetChanged()
        lineZDataSet?.notifyDataSetChanged()
        lineData?.notifyDataChanged()
        lineChartView.notifyDataSetChanged()
        lineChartView.invalidate()
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