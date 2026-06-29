package hka.awp.cgi.temi.app.feature.settings.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.utils.TemiBatteryMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel responsible for exposing reactive system battery state to the UI layer.
 *
 * It acts as an intermediate bridge that listens to hardware events provided by the
 * [TemiBatteryMonitor] and prepares them into lifecycle-aware [StateFlow] fields.
 * Any nullability or unsafe values coming from low-level hardware updates are sanitized here.
 *
 * @property batteryMonitor The hardware utility wrapper tracking active charging states and raw battery telemetry.
 */
@Suppress("MagicNumber")
class BatteryViewModel(
    private val batteryMonitor: TemiBatteryMonitor
) : ViewModel() {

    /**
     * An observable stream representing the current battery charge percentage (0-100).
     * Automatically maps unknown or null hardware readings safely down to `0`.
     * Keeps upstream collection active for up to 5 seconds after UI unsubscription to handle configuration changes.
     */
    val batteryLevel: StateFlow<Int> =
        batteryMonitor.batteryLevel
            .map { it ?: 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    /**
     * An observable stream indicating whether the device is currently plugged into a power source or charging dock.
     */
    val isCharging: StateFlow<Boolean> =
        batteryMonitor.isCharging
}
