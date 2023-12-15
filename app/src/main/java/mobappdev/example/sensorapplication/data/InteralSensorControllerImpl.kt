package mobappdev.example.sensorapplication.data

/**
 * File: InternalSensorControllerImpl.kt
 * Purpose: Implementation of the Internal Sensor Controller.
 * Author: Jitse van Esch
 * Created: 2023-09-21
 * Last modified: 2023-09-21
 */

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mobappdev.example.sensorapplication.domain.InternalSensorController
import kotlin.math.pow

private const val LOG_TAG = "Internal Sensor Controller"

class InternalSensorControllerImpl(
    context: Context
): InternalSensorController, SensorEventListener {

    // Expose acceleration to the UI
    private val _currentLinAccUI = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentLinAccUI: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentLinAccUI.asStateFlow()

    private var _currentGyro: Triple<Float, Float, Float>? = null
    private var _currentLinAcc: Triple<Float, Float, Float>? = null

    // Expose gyro to the UI on a certain interval
    private val _currentGyroUI = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentGyroUI: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentGyroUI.asStateFlow()

    private val _streamingGyro = MutableStateFlow(false)
    override val streamingGyro: StateFlow<Boolean>
        get() = _streamingGyro.asStateFlow()

    private val _streamingLinAcc = MutableStateFlow(false)
    override val streamingLinAcc: StateFlow<Boolean>
        get() = _streamingLinAcc.asStateFlow()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private val accSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    private var velocityX = 0.0
    private var velocityY = 0.0
    private var velocityZ = 0.0
    private var totalRotationAngle = 0.0
    // Filtering parameters
    private val alpha = 0.8 // Complementary filter alpha value

    // Threshold for filtering small changes
    private val angleThreshold = 0.1 // Adjust as needed
    // Timestamp variable for calculating delta time
    private var lastTimestamp = 0L

    override fun startImuStream() {
        // Todo: implement
    }

    override fun stopImuStream() {
        // Todo: implement
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun startGyroStream() {
        if (gyroSensor == null) {
            Log.e(LOG_TAG, "Gyroscope sensor is not available on this device")
            return
        }
        if (_streamingGyro.value) {
            Log.e(LOG_TAG, "Gyroscope sensor is already streaming")
            return
        }
        Log.e(LOG_TAG, "GYRO SCOPE")
        // Register this class as a listener for gyroscope events
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI)

        // Start a coroutine to update the UI variable on a 2 Hz interval
        GlobalScope.launch(Dispatchers.Main) {
            _streamingGyro.value = true
            while (_streamingGyro.value) {
                // Update the UI variable
                _currentGyroUI.update { _currentGyro }
                delay(500)
            }
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun startAccStream() {
        if (accSensor == null) {
            Log.e(LOG_TAG, "Acc sensor is not available on this device")
            return
        }
        if (_streamingLinAcc.value) {
            Log.e(LOG_TAG, "Acc sensor is already streaming")
            return
        }
        Log.e(LOG_TAG, "LIN SCOPE")

        // Register this class as a listener for gyroscope events
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI)

        // Start a coroutine to update the UI variable on a 2 Hz interval
        GlobalScope.launch(Dispatchers.Main) {
            _streamingLinAcc.value = true
            while (_streamingLinAcc.value) {
                // Update the UI variable
                _currentLinAccUI.update { _currentLinAcc }
                delay(500)
            }
        }

    }

    override fun stopAccStream() {
        if (_streamingLinAcc.value) {
            // Unregister the listener to stop receiving gyroscope events (automatically stops the coroutine as well
            sensorManager.unregisterListener(this, accSensor)
            _streamingLinAcc.value = false
        }
    }
    override fun stopGyroStream() {
        if (_streamingGyro.value) {
            // Unregister the listener to stop receiving gyroscope events (automatically stops the coroutine as well
            sensorManager.unregisterListener(this, gyroSensor)
            _streamingGyro.value = false
        }
    }



    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Extract gyro data (angular speed around X, Y, and Z axes
            _currentGyro = Triple(event.values[0], event.values[1], event.values[2])
        }

        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            // Extract linear acceleration data (acceleration along X, Y, and Z axes)
            val accX = event.values[0]
            val accY = event.values[1]
            val accZ = event.values[2]

            // Integration for velocity

            val dt = (event.timestamp - lastTimestamp) * NS2S // Convert nanoseconds to seconds
            lastTimestamp = event.timestamp

            // Apply a low-pass filter to reduce noise
            velocityX = alpha * velocityX + (1 - alpha) * accX * dt
            velocityY = alpha * velocityY + (1 - alpha) * accY * dt
            velocityZ = alpha * velocityZ + (1 - alpha) * accZ * dt

            // Calculate the total rotation angle by combining contributions from all axes
            totalRotationAngle += Math.toDegrees(Math.sqrt(velocityX.pow(2) + velocityY.pow(2) + velocityZ.pow(2)))

            // Wrap the angle within the range [0, 360)
            totalRotationAngle %= 360.0

            // Ensure that the angle is non-negative
            if (totalRotationAngle < 0) {
                totalRotationAngle += 360.0
            }

            // Apply a threshold to filter out small changes
            if (totalRotationAngle < angleThreshold) {
                totalRotationAngle = 0.0
            }

            // Update your UI or do further processing with the total rotation angle
            _currentLinAccUI.update { Triple(totalRotationAngle.toFloat(), 0f, 0f) }
        }
    }
    companion object {
        private const val NS2S = 1.0 / 1_000_000_000.0 // Nanoseconds to seconds conversion factor
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not used in this example
    }
}