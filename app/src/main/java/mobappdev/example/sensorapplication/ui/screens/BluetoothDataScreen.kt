package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mobappdev.example.sensorapplication.ui.viewmodels.CombinedSensorData
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM

@Composable
fun BluetoothDataScreen(
    vm: DataVM
) {
    val state = vm.state.collectAsStateWithLifecycle().value
    val deviceId = vm.deviceId.collectAsStateWithLifecycle().value
    val bluetoothDevices = vm.bluetoothDevices.value ?: emptyList()
    var isPopupVisible by remember { mutableStateOf(false) }
    var selectedDeviceId by remember { mutableStateOf("") }

    val value: String = when (val combinedSensorData = vm.combinedDataFlow.collectAsState().value) {
        is CombinedSensorData.GyroData -> {
            val triple = combinedSensorData.gyro
            if (triple == null) {
                "-"
            } else {
                String.format("%.1f, %.1f, %.1f", triple.first, triple.second, triple.third)
            }

        }

        is CombinedSensorData.HrData -> combinedSensorData.hr.toString()
        is CombinedSensorData.AccData -> {
            val value = combinedSensorData.acc
            if (value == null) {
                "---"
            } else {
                String.format("%.0f", value)
            }

        }

        else -> "-"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Text(text = if (state.connected) "connected" else "disconnected")
        Box(
            contentAlignment = Center,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (state.measuring) value else "-",
                fontSize = if (value.length < 3) 128.sp else 54.sp,
                color = Color.Black,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = vm::connectToSensor,
                enabled = !state.connected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Connect\n${deviceId}")
            }
            Button(
                onClick = vm::disconnectFromSensor,
                enabled = state.connected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Disconnect")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = vm::startHr,
                enabled = (!state.measuring),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Start\nHr Stream")
            }
            Button(
                onClick = vm::startForeignLinAcc,
                enabled = (!state.measuring),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Start LinAcc Stream")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = vm::stopDataStream,
                enabled = (state.measuring),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Stop\nstream")
            }
            Column {

                Button(
                    onClick = { isPopupVisible = true },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Start Device Discovery")
                }

                if (isPopupVisible) {
                    vm.startBluetoothDeviceDiscovery()
                    BluetoothDevicesPopup(
                        bluetoothDevices = bluetoothDevices,
                        onDeviceSelected = {
                            vm.setDeviceId(it.toString())
                            isPopupVisible = false // Close the popup on device selection
                        },
                        onDismiss = { isPopupVisible = false }
                    )
                }
            }
        }
    }
}
@Composable
fun BluetoothDevicesPopup(
    bluetoothDevices: List<String>,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .height(IntrinsicSize.Max)
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                bluetoothDevices.forEach { device ->
                    Text(
                        text = device,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device) }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}