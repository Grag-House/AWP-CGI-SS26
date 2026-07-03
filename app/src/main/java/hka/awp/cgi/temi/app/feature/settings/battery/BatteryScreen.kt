package hka.awp.cgi.temi.app.feature.settings.battery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point screen for the Battery settings feature.
 *
 * This composable acts as the state container (orchestrator), fetching the [BatteryViewModel]
 * via Koin dependency injection, collecting its reactive state flows as Compose state,
 * and passing the raw data down to the stateless [BatteryContent].
 *
 * @param onBackClick Fired when the user navigates back from this screen.
 * @param viewModel The state provider for battery metrics, injected via Koin by default.
 */
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
