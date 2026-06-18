package hka.awp.cgi.temi.app.feature.settings.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.utils.TemiBatteryMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Suppress("MagicNumber")
class BatteryViewModel(
    private val batteryMonitor: TemiBatteryMonitor
) : ViewModel() {

    val batteryLevel: StateFlow<Int> =
        batteryMonitor.batteryLevel
            .map { it ?: 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    val isCharging: StateFlow<Boolean> =
        batteryMonitor.isCharging
}
