package mobappdev.example.sensorapplication.domain

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface IBluetoothController
{
    val bluetoothDevices: LiveData<List<String>>
    fun stopBluetoothDeviceDiscovery()
    fun startBluetoothDeviceDiscovery()
}