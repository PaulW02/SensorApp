package mobappdev.example.sensorapplication.ui.viewmodels

/**
 * File: DataVM.kt
 * Purpose: Defines the view model of the data screen.
 *          Uses Dagger-Hilt to inject a controller model
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.app.Application
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import mobappdev.example.sensorapplication.data.AndroidPolarController
import mobappdev.example.sensorapplication.data.BluetoothController
import mobappdev.example.sensorapplication.domain.IBluetoothController
import mobappdev.example.sensorapplication.domain.InternalSensorController
import mobappdev.example.sensorapplication.domain.PolarController
import javax.inject.Inject
import mobappdev.example.sensorapplication.domain.FileController

// Inside your DataVM class


@HiltViewModel
class DataVM @Inject constructor(
    private val application: Application,
    private val polarController: PolarController,
    private val bluetoothController: IBluetoothController,
    private val internalSensorController: InternalSensorController,
    private val fileController: FileController
): ViewModel() {
    // Add a MutableLiveData for the chart data
    private val gyroDataFlow = internalSensorController.currentGyroUI
    private val accDataFlow = internalSensorController.currentLinAccUI
    private val accDataFlowForeign = polarController.currentAcc
    private val hrDataFlow = polarController.currentHR

    private val _bluetoothDevices = MutableLiveData<List<String>>()
    val bluetoothDevices: LiveData<List<String>> = _bluetoothDevices

    private val _sensorConnected = MutableLiveData<Boolean>()
    val sensorConnected: LiveData<Boolean>
        get() = _sensorConnected
    private val _sensorHRData = MutableLiveData<Int?>()
    val sensorHRData: LiveData<Int?>
        get() = _sensorHRData
    // Combine the two data flows
    val combinedDataFlow= combine(
        gyroDataFlow,
        accDataFlow,
        hrDataFlow,
        accDataFlowForeign
    ) { gyro, acc, hr, accForeign ->
        if (hr != null ) {
            CombinedSensorData.HrData(hr)
        } else if (gyro != null) {
            CombinedSensorData.GyroData(gyro)
        } else if (acc != null) {
            CombinedSensorData.AccData(acc)
        } else if (accForeign != null) {
            CombinedSensorData.AccData(accForeign.toFloat())
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _state = MutableStateFlow(DataUiState())
    val state = combine(
        polarController.hrList,
        polarController.connected,
        _state
    ) { hrList, connected, state ->
        state.copy(
            hrList = hrList,
            connected = connected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private var streamType: StreamType? = null
    init {
        bluetoothController.bluetoothDevices.observeForever { devices ->
            _bluetoothDevices.postValue(devices)
        }
    }

    private val _deviceId = MutableStateFlow("")

    val deviceId: StateFlow<String>
        get() = _deviceId.asStateFlow()



    private val _elapsedTime = MutableLiveData<Long>()
    val elapsedTime: LiveData<Long>
        get() = _elapsedTime

    private val _timerFinished = MutableLiveData<Boolean>()
    val timerFinished: LiveData<Boolean>
        get() = _timerFinished

    private var timer: CountDownTimer? = null

    private val _angleList = mutableListOf<Float>()
    val angleList: List<Float>
        get() = _angleList

    init {
        _elapsedTime.value = 0
        _timerFinished.value = false
    }
    fun startTimer(streamType: StreamType?) {
        _angleList.clear()
        timer = object : CountDownTimer(10000, 20) {
            override fun onTick(millisUntilFinished: Long) {
                _elapsedTime.value = 10000 - millisUntilFinished
                var currentAngle = internalSensorController.currentLinAccUI
                when (streamType) {
                    StreamType.LOCAL_ACC  -> currentAngle = internalSensorController.currentLinAccUI
                    StreamType.FOREIGN_ACC  -> currentAngle = polarController.currentAcc
                    else -> {} // Do nothing
                }
                currentAngle.value?.let {
                    _angleList.add(it)
                }
                if (_elapsedTime.value == 10000L) {
                    onFinish()
                }
            }


            override fun onFinish() {
                _timerFinished.value = true
                stopDataStream()
            }
        }.start()
    }

    fun stopTimer() {
        timer?.cancel()
        fileController.saveDataToCsv(angleList)
    }
    fun updateBluetoothDevices(devices: List<String>) {
        _bluetoothDevices.value = devices
    }
    fun startBluetoothDeviceDiscovery() {
        bluetoothController.bluetoothDevices.observeForever { devices ->
            _bluetoothDevices.postValue(devices)
        }
        bluetoothController.startBluetoothDeviceDiscovery()
    }

    fun setDeviceId(deviceId: String) {
        val parts = deviceId.split(" ")
        val extractedPart = parts.getOrNull(2)
        if (extractedPart != null) {
            _deviceId.value = extractedPart
        }
    }
    fun stopBluetoothDeviceDiscovery()
    {
        bluetoothController.stopBluetoothDeviceDiscovery()
    }
    fun chooseSensor(deviceId: String) {
        _deviceId.update { deviceId }
    }

    fun connectToSensor() {
        polarController.connectToDevice(_deviceId.value)
    }

    fun disconnectFromSensor() {
        stopDataStream()
        polarController.disconnectFromDevice(_deviceId.value)
    }

    fun startHr() {
        polarController.startHrStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_HR
        _state.update { it.copy(measuring = true) }
    }

    fun startGyro() {
        internalSensorController.startGyroStream()
        streamType = StreamType.LOCAL_GYRO

        _state.update { it.copy(measuring = true) }
    }

    fun startLinAcc() {
        internalSensorController.startAccStream()
        streamType = StreamType.LOCAL_ACC
        startTimer(streamType)
        _state.update { it.copy(measuring = true) }
    }

    fun startForeignLinAcc() {
        polarController.startAccStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_ACC
        startTimer(streamType)
        _state.update { it.copy(measuring = true) }
    }


    fun stopDataStream(){
        Log.e("ACC", " test " + streamType.toString());
        when (streamType) {
            StreamType.LOCAL_GYRO -> internalSensorController.stopGyroStream()
            StreamType.LOCAL_ACC  -> internalSensorController.stopAccStream()
            StreamType.FOREIGN_ACC  -> polarController.stopAccStreaming()
            StreamType.FOREIGN_HR -> polarController.stopHrStreaming()
            else -> {} // Do nothing
        }
        stopTimer()
        _state.update { it.copy(measuring = false) }
    }
}

data class DataUiState(
    val hrList: List<Int> = emptyList(),
    val connected: Boolean = false,
    val measuring: Boolean = false
)

enum class StreamType {
    LOCAL_GYRO, LOCAL_ACC, FOREIGN_HR, FOREIGN_ACC
}

sealed class CombinedSensorData {
    data class GyroData(val gyro: Triple<Float, Float, Float>?) : CombinedSensorData()
    data class AccData(val acc: Float?) : CombinedSensorData()
    data class HrData(val hr: Int?) : CombinedSensorData()
}