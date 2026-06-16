package hka.awp.cgi.temi.app.feature.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.utils.TemiMovementController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

@Suppress("TooManyFunctions")
class ControllerViewModel(
    private val movementController: TemiMovementController,
    private val bluetoothControllerManager: BluetoothControllerManager,
    private val cameraStreamManager: ControllerCameraStreamManager
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

        if (enabled) {
            cameraStreamManager.startLiveView()
        } else {
            movementController.stop()
            cameraStreamManager.stopLiveView()
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

    private var lastX = 0f
    private var lastY = 0f
    private var wasMoving = false

    init {
        bluetoothControllerManager.loadPairedDevices()

        viewModelScope.launch {
            while (true) {
                if (_controllerEnabled.value) {
                    val isMoving = lastX != 0f || lastY != 0f

                    if (isMoving) {
                        movementController.move(
                            linear = -lastY,
                            angular = lastX,
                        )
                        wasMoving = true
                    } else if (wasMoving) {
                        movementController.stop()
                        wasMoving = false
                    }
                }

                delay(CONTROLLER_UPDATE_INTERVAL_MS)
            }
        }
    }

    fun onControllerInput(x: Float, y: Float) {
        val steering = if (x > 0) x * x else -(x * x)

        val turnMultiplier = 2.0f

        val turnEffort = abs(steering)
        val linearSpeed = y * (1.0f - (turnEffort * 0.3f))

        val turn = steering * turnMultiplier

        lastX = turn
        lastY = linearSpeed
    }

    fun loadPairedDevices() {
        bluetoothControllerManager.loadPairedDevices()
    }

    fun disconnectHidDevice(address: String) {
        bluetoothControllerManager.disconnectHidDevice(address)
    }

    override fun onCleared() {
        movementController.stop()
        bluetoothControllerManager.release()
        cameraStreamManager.stopLiveView()
        super.onCleared()
    }
}

private const val CONTROLLER_UPDATE_INTERVAL_MS = 20L
