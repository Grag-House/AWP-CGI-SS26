package hka.awp.temi_cgi_app.ui.shell

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.temi_cgi_app.utils.NetworkManager
import hka.awp.temi_cgi_app.utils.getLocalTime
import hka.awp.temi_cgi_app.utils.TemiBatteryMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.format.DateTimeFormatter

/**
 * ViewModel responsible for managing the global application state, including navigation,
 * UI shell components, and system status monitoring.
 *
 * This ViewModel handles the state of the sidebar expansion, the currently selected
 * navigation route, and periodically polls the Wi-Fi signal strength.
 *
 * @property networkManager The manager used to retrieve network-related information, such as Wi-Fi levels.
 */
class AppViewModel(
    networkManager: NetworkManager, clock: Clock, datetimeFormatter: DateTimeFormatter
) : ViewModel() {
class AppViewModel(
    private val networkManager: NetworkManager, temiBatteryMonitor: TemiBatteryMonitor
) : ViewModel() {
    var selectedRoute by mutableStateOf(Screen.Dashboard.route)
        private set

    fun onRouteSelect(screen: Screen) {
        selectedRoute = screen.route
        Log.d(this.javaClass.simpleName, "TODO routing")
    }

    var isSidebarExpanded by mutableStateOf(true)
        private set

    fun onSideBarToggle() {
        isSidebarExpanded = !isSidebarExpanded
        Log.d(
            this.javaClass.simpleName, "Sidepanel collapse triggered, currently: $isSidebarExpanded"
        )
    }

    private var _wifiLevel = MutableStateFlow(0)
    val wifiLevel: StateFlow<Int> = _wifiLevel.asStateFlow()

    private fun startWifiPolling(networkManager: NetworkManager) {
        viewModelScope.launch {
            while (isActive) {
                _wifiLevel.value = networkManager.getWifiSignalLevel()
                delay(5000L)
            }
        }
    }

    private var _currentTime = MutableStateFlow(getLocalTime(clock, datetimeFormatter))
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private fun startTimePolling(clock: Clock, dateTimeFormatter: DateTimeFormatter) {
        viewModelScope.launch {
            while (isActive) {
                _currentTime.value = getLocalTime(clock, dateTimeFormatter)
                delay(15000L)
            }
        }
    }

    val batteryLevel: StateFlow<Int?> = temiBatteryMonitor.batteryLevel
    val isCharging: StateFlow<Boolean> = temiBatteryMonitor.isCharging

    init {
        startWifiPolling(networkManager)
        startTimePolling(clock, datetimeFormatter)
    }
}