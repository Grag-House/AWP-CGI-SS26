package hka.awp.cgi.temi.app.feature.controller

import androidx.lifecycle.ViewModel
import hka.awp.cgi.temi.app.utils.TemiMovementController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ControllerViewModel(
    private val movementController: TemiMovementController,
    private val bluetoothControllerManager: BluetoothControllerManager,
) : ViewModel() {

    val devices: StateFlow<List<ControllerDevice>> = bluetoothControllerManager.devices
    val isScanning: StateFlow<Boolean> = bluetoothControllerManager.isScanning

    private val _controllerEnabled = MutableStateFlow(false)
    val controllerEnabled: StateFlow<Boolean> = _controllerEnabled.asStateFlow()

    fun connectHidDevice(address: String) {
        bluetoothControllerManager.connectHidDevice(address)
    }

    fun removeBond(address: String) {
        bluetoothControllerManager.removeBond(address)
    }

    fun setControllerEnabled(enabled: Boolean) {
        _controllerEnabled.value = enabled

        if (!enabled) {
            movementController.stop()
        }
    }

    fun startBluetoothScan() {
        bluetoothControllerManager.startDiscovery()
    }

    fun stopBluetoothScan() {
        bluetoothControllerManager.stopDiscovery()
    }

    fun pairDevice(address: String) {
        bluetoothControllerManager.pairDevice(address)
    }

    fun logPairedDevices() {
        bluetoothControllerManager.logPairedDevices()
    }

    fun onControllerInput(x: Float, y: Float) {
        if (!_controllerEnabled.value) {
            return
        }

        if (x == 0f && y == 0f) {
            movementController.stop()
            return
        }

        movementController.move(
            linear = -y,
            angular = x,
        )
    }

    fun onControllerReleased() {
        movementController.stop()
    }

    override fun onCleared() {
        movementController.stop()
        bluetoothControllerManager.release()
        super.onCleared()
    }
}
