package hka.awp.temi_cgi_app.ui.shell

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.temi_cgi_app.utils.NetworkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.robotemi.sdk.listeners.OnBatteryStatusChangedListener
import com.robotemi.sdk.BatteryData
import com.robotemi.sdk.Robot

/**
 * ViewModel responsible for managing the global application state, including navigation,
 * UI shell components, and system status monitoring.
 *
 * This ViewModel handles the state of the sidebar expansion, the currently selected
 * navigation route, and periodically polls the Wi-Fi signal strength.
 *
 * @property networkManager The manager used to retrieve network-related information, such as Wi-Fi levels.
 */
class AppViewModel(networkManager: NetworkManager) : ViewModel(), OnBatteryStatusChangedListener {
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

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging

    private var robot: Robot? = null

    private var _wifiLevel = MutableStateFlow(0)
    val wifiLevel: StateFlow<Int> = _wifiLevel

    private fun startWifiPolling(networkManager: NetworkManager) {
        viewModelScope.launch {
            while (isActive) {
                _wifiLevel.value = networkManager.getWifiSignalLevel()
                delay(5000L)
            }
        }
    }

    private fun startBatteryListener() {
        try {
            robot = Robot.getInstance()
            robot?.addOnBatteryStatusChangedListener(this)

            robot?.batteryData?.let { batteryData ->
                updateBatteryState(batteryData)
            }
        } catch (e: Exception) {
            Log.w(
                this.javaClass.simpleName,
                "Temi SDK not available, probably running locally",
                e
            )
            _batteryLevel.value = null
            _isCharging.value = false
        }
    }

    override fun onBatteryStatusChanged(batteryData: BatteryData?) {
        batteryData?.let { updateBatteryState(it) }
    }

    private fun updateBatteryState(batteryData: BatteryData) {
        _batteryLevel.value = batteryData.level
        _isCharging.value = batteryData.isCharging
    }

    override fun onCleared() {
        try {
            robot?.removeOnBatteryStatusChangedListener(this)
        } catch (e: Exception) {
            Log.w(this.javaClass.simpleName, "Could not remove battery listener", e)
        }

        super.onCleared()
    }

    init {
        startWifiPolling(networkManager)
        startBatteryListener()
    }
}