package hka.awp.temi_cgi_app.temi

import android.util.Log
import com.robotemi.sdk.BatteryData
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnBatteryStatusChangedListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TemiStatusServiceImpl : TemiStatusService, OnBatteryStatusChangedListener {

    private var robot: Robot? = null

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    override val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    override val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _isTemiAvailable = MutableStateFlow(false)
    override val isTemiAvailable: StateFlow<Boolean> = _isTemiAvailable.asStateFlow()

    override fun start() {
        try {
            robot = Robot.getInstance()
            _isTemiAvailable.value = true

            robot?.addOnBatteryStatusChangedListener(this)

            robot?.batteryData?.let { updateBattery(it) }
        } catch (e: Exception) {
            Log.w("TemiStatusService", "Temi SDK not available locally", e)
            _isTemiAvailable.value = false
            _batteryLevel.value = null
            _isCharging.value = false
        }
    }

    override fun stop() {
        try {
            robot?.removeOnBatteryStatusChangedListener(this)
        } catch (e: Exception) {
            Log.w("TemiStatusService", "Could not remove battery listener", e)
        }
    }

    override fun onBatteryStatusChanged(batteryData: BatteryData?) {
        if (batteryData == null) return
        updateBattery(batteryData)
    }

    private fun updateBattery(batteryData: BatteryData) {
        _batteryLevel.value = batteryData.level
        _isCharging.value = batteryData.isCharging
    }
}