package mobappdev.example.sensorapplication.domain

/**
 * File: InternalSensorController.kt
 * Purpose: Defines the blueprint for the Internal Sensor Controller.
 * Author: Jitse van Esch
 * Created: 2023-09-21
 * Last modified: 2023-09-21
 */

import android.hardware.SensorEvent
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.StateFlow

interface InternalSensorController {
    val currentLinAccUI: StateFlow<Float?>
    val currentGyroUI: StateFlow<Triple<Float, Float, Float>?>
    val streamingGyro: StateFlow<Boolean>
    val streamingLinAcc: StateFlow<Boolean>
    fun startImuStream()
    fun stopImuStream()

    fun startGyroStream()
    fun stopGyroStream()

    fun startAccStream()
    fun stopAccStream()

    fun onSensorChanged(event: SensorEvent)
}