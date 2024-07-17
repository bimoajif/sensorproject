package com.bimoajif.sensorproject

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.bimoajif.sensorproject.database.AppDatabase
import com.bimoajif.sensorproject.database.SensorDao
import com.bimoajif.sensorproject.database.SensorEntity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private lateinit var db: AppDatabase
    private lateinit var dao: SensorDao

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val accelerometerReadingFiltered = FloatArray(3)
    private val magnetometerReadingFiltered = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val rotationMatrixRemapped = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val orientationAnglesRemapped = FloatArray(3)

    private val alpha = 0.8f

    private lateinit var pitchTextView: TextView
    private lateinit var pitchRelativeTextView: TextView
    private lateinit var rollTextView: TextView
    private lateinit var yawTextView: TextView

    private lateinit var magXTextView: TextView
    private lateinit var magYTextView: TextView
    private lateinit var magZTextView: TextView

    private lateinit var timerTextView: TextView

    private lateinit var lineChartView: LineChart

    private lateinit var buttonView: Button
    private lateinit var stopButtonView: Button
    private lateinit var startButtonView: Button

    private var countDownTimer: CountDownTimer? = null
    private var timeInMilliseconds = 60000L
    private var pauseOffSet = 0L

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
        pitchRelativeTextView = findViewById(R.id.pitch_relative)
        rollTextView = findViewById(R.id.roll)
        yawTextView = findViewById(R.id.yaw)
        magXTextView = findViewById(R.id.magnetic_x)
        magYTextView = findViewById(R.id.magnetic_y)
        magZTextView = findViewById(R.id.magnetic_z)
        timerTextView = findViewById(R.id.timer)
        lineChartView = findViewById(R.id.chart)
        buttonView = findViewById(R.id.button)
        stopButtonView = findViewById(R.id.button_stop)
        startButtonView = findViewById(R.id.button_start)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        lineYDataSet = LineDataSet(lineYValues, "Magnetic Field Y")
        lineZDataSet = LineDataSet(lineZValues, "Magnetic Field Z")

        lineYDataSet?.setDrawCircles(false)
        lineZDataSet?.setDrawCircles(false)

        lineYDataSet?.color = getColor(R.color.colorPrimary)
        lineZDataSet?.color = getColor(R.color.colorAccent)

        lineData = LineData(lineYDataSet, lineZDataSet)
        lineChartView.data = lineData
    }

    private fun registerSensor() {
        accelerometer?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        magnetometer?.also { magnetometer ->
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onResume() {
        super.onResume()
        registerSensor()
        buttonView.setOnClickListener {
            lineYValues.clear()
            lineZValues.clear()
            i = 0.0.toFloat()
            dao.deleteAll()

            val text = "Reset Reading"
            val duration = Toast.LENGTH_SHORT

            Toast.makeText(this, text, duration).show()

            resetTimer()
        }

        stopButtonView.setOnClickListener {
            sensorManager.unregisterListener(this)
            val text = "Stop Reading"
            val duration = Toast.LENGTH_SHORT

            Toast.makeText(this, text, duration).show()

            pauseTimer()
        }

        startButtonView.setOnClickListener {
            registerSensor()
            val text = "Start Reading"
            val duration = Toast.LENGTH_SHORT

            Toast.makeText(this, text, duration).show()

            startTimer(pauseOffSet)
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

            var pitch2 = (abs(Math.toDegrees(orientationAngles[1].toDouble()))).toFloat().roundToLong()
            var azimuth2 = Math.toDegrees(orientationAngles[0].toDouble()).toFloat().roundToLong()

            val pitchRelative = abs((Math.toDegrees(orientationAnglesRemapped[1].toDouble()))).toFloat() + 22.6
            val pitch2Relative = abs((Math.toDegrees(orientationAngles[1].toDouble()))).toFloat() + 22.6

            azimuth -= 180
            if (azimuth < -180) {
                azimuth += 360
            }

            if (azimuth2 < 0) {
                azimuth2 += 360
            }

            if (accelerometerReading[2] < 0) {
                pitch *= -1
                pitch2 *= -1
            }

            if(accelerometerReading[1] < 0 && accelerometerReading[2] > 0) {
                pitch = 180 - pitch
            }

            setLineChartData(Entry(i, abs(magnetometerReadingFiltered[1])), Entry(i, abs(magnetometerReadingFiltered[2])))
            i += 1

            "Pitch: $pitch2".also { pitchTextView.text = it }
            "Pitch Offset: $pitch2Relative ".also { pitchRelativeTextView.text = it }
            "Roll: $roll".also { rollTextView.text = it }
            "Yaw: $azimuth2".also { yawTextView.text = it }
            "x: ${magnetometerReading[0]}".also { magXTextView.text = it }
            "y: ${magnetometerReading[1]}".also { magYTextView.text = it }
            "z: ${magnetometerReading[2]}".also { magZTextView.text = it }

            val sensor = SensorEntity(id = i, timestamp= ((timeInMilliseconds - pauseOffSet)/ 1000), pitch = pitch2, pitchOffset = pitch2Relative, yaw = azimuth2, magnetoX = magnetometerReadingFiltered[0], magnetoY = magnetometerReadingFiltered[1], magnetoZ = magnetometerReadingFiltered[2])

            if (i == 100.toFloat()) {
                val text = "Start Reading..."
                val duration = Toast.LENGTH_SHORT

                Toast.makeText(this, text, duration).show()

                startTimer(pauseOffSet)
            }

            if (((timeInMilliseconds - pauseOffSet)/ 1000).toInt() != 0 && i > 100) {
//                if(i % 25 == 0.0.toFloat()) {
//                    val text = "Change Distance"
//                    val duration = Toast.LENGTH_SHORT
//
//                    Toast.makeText(this, text, duration).show()
//                }
                dao.insertAll(sensor)
            }

            if (((timeInMilliseconds - pauseOffSet)/ 1000).toInt() == 0) {
                val text = "Finish Reading!"
                val duration = Toast.LENGTH_SHORT

                sensorManager.unregisterListener(this)
                Toast.makeText(this, text, duration).show()

                pauseTimer()
            }
        }

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something if sensor accuracy changes
        val text = "${sensor.name} Accuracy change to $accuracy"
        val duration = Toast.LENGTH_SHORT

        Toast.makeText(this, text, duration).show()
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

    private fun startTimer(pauseOffsetL : Long) {
        countDownTimer = object : CountDownTimer(timeInMilliseconds - pauseOffsetL, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                pauseOffSet = timeInMilliseconds - millisUntilFinished
                timerTextView.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                val text = "Timer Finished!"
                val duration = Toast.LENGTH_SHORT

                Toast.makeText(this@MainActivity, text, duration).show()
            }
        }.start()
    }

    private fun pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer!!.cancel()
        }
    }

    private fun resetTimer() {
        if (countDownTimer != null) {
            countDownTimer!!.cancel()
            timerTextView.text = (timeInMilliseconds/ 1000).toString()
            countDownTimer = null
            pauseOffSet = 0
        }
    }

    private fun exportDataToCsv() {
        val dataset = dao.getAll()

        try {
            val writer = FileWriter(File(getExternalFilesDir(null), "sensor_reading.csv"))
            writer.append("id, magneto_x, magneto_y, magneto_z")

            for (data in dataset) {
                writer.append("${data.id}, ${data.magnetoX}, ${data.magnetoY}, ${data.magnetoZ}")
            }

            writer.flush()
            writer.close()

        } catch (e: IOException) {
            val text = e.toString()
            val duration = Toast.LENGTH_SHORT

            e.printStackTrace()
            Toast.makeText(this, text, duration).show()
        }
    }
}
