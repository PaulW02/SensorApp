import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM

@Composable
fun SensorScreen(
    vm:DataVM
){
    val state = vm.state.collectAsStateWithLifecycle().value
    val deviceId = vm.deviceId.collectAsStateWithLifecycle().value
}


