package hka.awp.cgi.temi.app.utils

import com.robotemi.sdk.BatteryData
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnBatteryStatusChangedListener
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TemiBatteryMonitor(robot: Robot?, private val mqttManager: MqttManager) : OnBatteryStatusChangedListener {
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    override fun onBatteryStatusChanged(batteryData: BatteryData?) {
        batteryData?.let {
            _batteryLevel.value = it.level
            _isCharging.value = it.isCharging
        }

        publishBatteryLevel(batteryData)
    }

    init {
        robot?.addOnBatteryStatusChangedListener(this)
        robot?.batteryData?.let {
            _batteryLevel.value = it.level
            _isCharging.value = it.isCharging
        }
    }

    fun publishBatteryLevel(batteryData: BatteryData?) {
        CoroutineScope(Dispatchers.IO).launch {
            mqttManager.publishStatus(MqttManager.BATTERY_TOPIC, batteryData?.level?.toString() ?: "unknown")
        }
    }
}
