package mobappdev.example.sensorapplication.domain

import androidx.lifecycle.LiveData

interface IBluetoothController
{
    val bluetoothDevices: LiveData<List<String>>
    fun stopBluetoothDeviceDiscovery()
    fun startBluetoothDeviceDiscovery()
}