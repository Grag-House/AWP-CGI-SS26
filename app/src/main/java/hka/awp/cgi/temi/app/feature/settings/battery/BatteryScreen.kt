package hka.awp.cgi.temi.app.feature.settings.battery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BatteryScreen(
    onBackClick: () -> Unit,
    viewModel: BatteryViewModel = koinViewModel()
                 ) {
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()

    BatteryContent(
        batteryLevel = batteryLevel,
        isCharging = isCharging,
        onBackClick = onBackClick
                  )
}
