package hka.awp.cgi.temi.app.ui.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.utils.NetworkManager
import hka.awp.cgi.temi.app.utils.TemiBatteryMonitor
import hka.awp.cgi.temi.app.utils.getLocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel responsible for managing the global application state, including navigation,
 * UI shell components, and system status monitoring.
 *
 * This ViewModel handles the state of the sidebar expansion, the currently selected
 * navigation route, and monitors system status like Wi-Fi and battery.
 *
 * @property networkManager The manager for retrieving network-related information.
 * @property clock The system clock for time updates.
 * @property datetimeFormatter Formatter for displaying the current time.
 * @property temiBatteryMonitor Monitor for robot battery status.
 */
class AppViewModel(
    networkManager: NetworkManager,
    clock: Clock,
    datetimeFormatter: DateTimeFormatter,
    temiBatteryMonitor: TemiBatteryMonitor
) : ViewModel() {
    var selectedRoute by mutableStateOf(Screen.Dashboard.route)
        private set

    /** Updates the currently selected navigation route. */
    fun onRouteSelect(screen: Screen) {
        selectedRoute = screen.route
    }

    var isSidebarExpanded by mutableStateOf(true)
        private set

    /** Toggles the expanded/collapsed state of the sidebar. */
    fun onSideBarToggle() {
        isSidebarExpanded = !isSidebarExpanded
        Timber.v("Sidepanel collapse triggered, currently: %s", isSidebarExpanded)
    }

    /** Reactive flow of the current Wi-Fi signal level (0-4). */
    val wifiLevel: StateFlow<Int> = networkManager.wifiSignalLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STATE_TIMEOUT), 0)

    private var _currentTime = MutableStateFlow(getLocalTime(clock, datetimeFormatter))

    /** Observable flow of the current formatted local time. */
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private fun startTimePolling(clock: Clock, dateTimeFormatter: DateTimeFormatter) {
        viewModelScope.launch {
            while (isActive) {
                _currentTime.value = getLocalTime(clock, dateTimeFormatter)
                @Suppress("MagicNumber")
                delay(15000L.milliseconds)
            }
        }
    }

    /** Observable flow of the current battery level percentage. */
    val batteryLevel: StateFlow<Int?> = temiBatteryMonitor.batteryLevel

    /** Observable flow indicating if the device is currently charging. */
    val isCharging: StateFlow<Boolean> = temiBatteryMonitor.isCharging

    init {
        startTimePolling(clock, datetimeFormatter)
    }

    companion object {
        private const val STATE_TIMEOUT = 5000L
    }
}
