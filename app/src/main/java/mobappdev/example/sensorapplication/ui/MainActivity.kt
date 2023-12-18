package mobappdev.example.sensorapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mobappdev.example.sensorapplication.ui.screens.BluetoothDataScreen
import mobappdev.example.sensorapplication.ui.screens.InternalDataScreen
import mobappdev.example.sensorapplication.ui.theme.SensorapplicationTheme
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deviceId = "C07BD627"

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 31)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 30)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 29)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SensorapplicationTheme {
                val dataVM = hiltViewModel<DataVM>()
                dataVM.chooseSensor(deviceId)

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    Scaffold(
                        content = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 100.dp) // Adjust the padding as needed
                            ) {
                                NavHost(navController, startDestination = Screen.Bluetooth.route) {
                                    composable(Screen.Bluetooth.route) { BluetoothDataScreen(vm = dataVM) }
                                    composable(Screen.Internal.route) { InternalDataScreen(vm = dataVM) }
                                }
                            }
                        },
                        bottomBar = {
                            Spacer(modifier = Modifier.height(56.dp))
                            NavigationBar {
                                val items = listOf("External", "Internal")
                                var selectedItem by remember { mutableStateOf(0) }

                                items.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        icon = {
                                            when (index) {
                                                0 -> Icon(
                                                    Icons.Default.Create,
                                                    contentDescription = item
                                                )

                                                1 -> Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = item
                                                )

                                                else -> Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = item
                                                )
                                            }
                                        },
                                        label = { Text(item) },
                                        selected = selectedItem == index,
                                        onClick = {
                                            if (selectedItem != index) {
                                                selectedItem = index
                                                when (index) {
                                                    0 -> navController.navigate(Screen.Bluetooth.route)
                                                    1 -> navController.navigate(Screen.Internal.route)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    private sealed class Screen(val route: String) {
        object Blank : Screen("blank")
        object Bluetooth : Screen("bluetooth")
        object Internal : Screen("internal")
    }
}
