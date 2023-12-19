package mobappdev.example.sensorapplication.data

/**
 * File: AndroidPolarController.kt
 * Purpose: Implementation of the PolarController Interface.
 *          Communicates with the polar API
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mobappdev.example.sensorapplication.domain.PolarController
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.log

class AndroidPolarController (
    private val context: Context,
): PolarController {

    private val api: PolarBleApi by lazy {
        // Notice all features are enabled
        PolarBleApiDefaultImpl.defaultImplementation(
            context = context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        )
    }

    private var hrDisposable: Disposable? = null
    private var accDisposable: Disposable? = null // Declare accDisposable for accelerometer stream
    private val TAG = "AndroidPolarController"
    private val RAD_TO_DEG = 180 / PI
    private val _currentHR = MutableStateFlow<Int?>(null)
    private val _currentAcc = MutableStateFlow<Float?>(null)
    override val currentHR: StateFlow<Int?>
        get() = _currentHR.asStateFlow()

     override val currentAcc: StateFlow<Float?>
        get() = _currentAcc.asStateFlow()

    private val _accList = MutableStateFlow<List<Int>>(emptyList())
    override val accList: StateFlow<List<Int>>
        get() = _accList.asStateFlow()

    private val _hrList = MutableStateFlow<List<Int>>(emptyList())
    override val hrList: StateFlow<List<Int>>
        get() = _hrList.asStateFlow()

    private val _connected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean>
        get() = _connected.asStateFlow()

    private val _measuring = MutableStateFlow(false)
    override val measuring: StateFlow<Boolean>
        get() = _measuring.asStateFlow()

    val bluetoothManager: BluetoothManager? = getSystemService(context, BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.getAdapter()

    private val _bluetoothDevices = MutableLiveData<List<String>>() // Device IDs as strings
    override val bluetoothDevices: LiveData<List<String>>
        get() = _bluetoothDevices


    private val bluetoothReceiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                }
            }
        }
    }

    init {
        api.setPolarFilter(false)

        val enableSdkLogs = false
        if(enableSdkLogs) {
            api.setApiLogger { s: String -> Log.d("Polar API Logger", s) }
        }

        api.setApiCallback(object: PolarBleApiCallback() {
            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                _connected.update { true }
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                _connected.update { false }
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")
            }
        })
    }
   override fun startBluetoothDeviceDiscovery() {
       val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
       context.registerReceiver(bluetoothReceiver, filter)
/*        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }*/

       if (ActivityCompat.checkSelfPermission(
               this.context,
               Manifest.permission.BLUETOOTH_SCAN
           ) == PackageManager.PERMISSION_GRANTED
       ) {
       } else {
       }
       Log.e("TEST", "EHELLO");
       bluetoothAdapter?.startDiscovery()
    }

   override fun stopBluetoothDeviceDiscovery() {
        context.unregisterReceiver(bluetoothReceiver)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }
    override fun connectToDevice(deviceId: String) {
        try {
            api.connectToDevice(deviceId)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            Log.e(TAG, "Failed to connect to $deviceId.\n Reason $polarInvalidArgument")
        }
    }

    override fun disconnectFromDevice(deviceId: String) {
        try {
            api.disconnectFromDevice(deviceId)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            Log.e(TAG, "Failed to disconnect from $deviceId.\n Reason $polarInvalidArgument")
        }
    }
    override fun startAccStreaming(deviceId: String) {
        val isDisposed = accDisposable?.isDisposed ?: true
        if (isDisposed) {
            val settings = api.requestStreamSettings(deviceId, feature = PolarBleApi.PolarDeviceDataType.ACC)
            _measuring.update { true }


            val polarSensorSetting = settings.blockingGet()
            accDisposable = api.startAccStreaming(deviceId, polarSensorSetting)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { accData: PolarAccelerometerData ->
                        //Log.e("HEJ", " TEST " + accData.samples)
                        for (sample in accData.samples) {
                            val accX = sample.x.toDouble()
                            val accY = sample.y.toDouble()
                            val accZ = sample.z.toDouble()
                            // Use the filtered linear acceleration values to calculate the tilt angle (x)
                            val x = RAD_TO_DEG * atan2(accY, accZ)
                            Log.e("LOGGG", "" + x + " ")
                            _currentAcc.update {x.toFloat()}
                            _accList.update { accList ->
                                accList + sample.x
                            }
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Acc stream failed.\nReason $error")
                    },
                    { Log.d(TAG, "Acc stream complete") }
                )
        } else {
            Log.d(TAG, "Already streaming")
        }
    }

    override fun stopAccStreaming() {
        _measuring.update { false }
        accDisposable?.dispose()
        _currentAcc.update { null }
    }

    override fun startHrStreaming(deviceId: String) {
        val isDisposed = hrDisposable?.isDisposed ?: true
        if(isDisposed) {
            _measuring.update { true }
            hrDisposable = api.startHrStreaming(deviceId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        for (sample in hrData.samples) {
                            _currentHR.update {
                                sample.hr
                            }
                            _hrList.update { hrList ->
                                hrList + sample.hr
                            }
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Hr stream failed.\nReason $error")
                    },
                    { Log.d(TAG, "Hr stream complete")}
                )
        } else {
            Log.d(TAG, "Already streaming")
        }

    }

    override fun stopHrStreaming() {
        _measuring.update { false }
        hrDisposable?.dispose()
        _currentHR.update { null }
    }
}