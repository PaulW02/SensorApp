package mobappdev.example.sensorapplication.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiDefaultImpl
import mobappdev.example.sensorapplication.domain.IBluetoothController

class BluetoothController(
    private val context: Context,
):IBluetoothController{
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
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val _bluetoothDevices = MutableLiveData<List<String>>() // Device IDs as strings
    override val bluetoothDevices: LiveData<List<String>>
        get() = _bluetoothDevices

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d("BLUETOOOOTH", "Device found: ${device.name}")
                      if (isPolarDevice(device)) {
                            val updatedList = _bluetoothDevices.value.orEmpty().toMutableList()
                            val polarNumber = extractPolarNumber(device.name ?: "")
                            if (polarNumber != null && !updatedList.contains(device.name)) {
                                updatedList.add(device.name)
                                _bluetoothDevices.postValue(updatedList)
                            }
                      }
                    }
                }
            }
        }
    }
    init {
            context.registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            api.setPolarFilter(false)
    }
    @SuppressLint("MissingPermission")
    override fun stopBluetoothDeviceDiscovery() {
        context.unregisterReceiver(bluetoothReceiver)
        bluetoothAdapter?.cancelDiscovery()
    }
    @SuppressLint("MissingPermission")
   override  fun startBluetoothDeviceDiscovery() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        println("StartBluetoothDISVVORCY")
        bluetoothAdapter?.startDiscovery()
    }
    @SuppressLint("MissingPermission")
    private fun isPolarDevice(device: BluetoothDevice): Boolean {
        // Replace this with your logic to identify Polar devices
        // For example, you might check the device name or other attributes
        return device.name?.startsWith("Polar") == true
    }

    private fun extractPolarNumber(deviceName: String): String? {
        // Extract the number from the device name
        return deviceName.replace("Polar Sense", "").trim()
    }

}