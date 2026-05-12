package hka.awp.cgi.temi.app.utils

import com.robotemi.sdk.BatteryData
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnBatteryStatusChangedListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TemiBatteryMonitor(robot: Robot?) : OnBatteryStatusChangedListener {
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    override fun onBatteryStatusChanged(batteryData: BatteryData?) {
        batteryData?.let {
            _batteryLevel.value = it.level
            _isCharging.value = it.isCharging
        }
    }

    init {
        robot?.addOnBatteryStatusChangedListener(this)
        robot?.batteryData?.let {
            _batteryLevel.value = it.level
            _isCharging.value = it.isCharging
        }
    }
}
