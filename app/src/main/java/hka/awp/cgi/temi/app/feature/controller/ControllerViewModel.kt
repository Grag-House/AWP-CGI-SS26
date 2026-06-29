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

/**
 * ViewModel orchestrating the coordination between Bluetooth input events and platform hardware movement loops.
 */
@Suppress("TooManyFunctions")
class ControllerViewModel(
    private val movementController: TemiMovementController,
    private val bluetoothControllerManager: BluetoothControllerManager
) : ViewModel() {

    companion object {
        private const val CONTROLLER_UPDATE_INTERVAL_MS = 20L
        private const val BASE_SPEED_FACTOR = 1.0f
        private const val TURN_EFFORT_PENALTY_WEIGHT = 0.3f
        private const val TURN_MULTIPLIER = 2.0f
    }
    val devices: StateFlow<List<ControllerDevice>> = bluetoothControllerManager.devices
    val isScanning: StateFlow<Boolean> = bluetoothControllerManager.isScanning

    private val _controllerEnabled = MutableStateFlow(false)
    val controllerEnabled: StateFlow<Boolean> = _controllerEnabled.asStateFlow()

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

    /**
     * Connects an asynchronous HID profile proxy targeting the specified peripheral MAC address.
     */
    fun connectHidDevice(address: String) {
        bluetoothControllerManager.connectHidDevice(address)
    }

    /**
     * Disconnects the active HID profile proxy pipeline from the specified device.
     */
    fun disconnectHidDevice(address: String) {
        bluetoothControllerManager.disconnectHidDevice(address)
    }

    /**
     * Initiates cryptographic unpairing protocols targeting the specified hardware link record.
     */
    fun removeBond(address: String) {
        bluetoothControllerManager.removeBond(address)
    }

    /**
     * Configures the active control capture state and halts active hardware motors if disabled.
     */
    fun setControllerEnabled(enabled: Boolean) {
        _controllerEnabled.value = enabled
        if (!enabled) {
            movementController.stop()
        }
    }

    /**
     * Dispatches an asynchronous radio signal pass to discover nearby non-bonded peripherals.
     */
    fun startBluetoothScan() {
        bluetoothControllerManager.startDiscovery()
    }

    /**
     * Halts active host radio discovery sweeps to restore execution performance bands.
     */
    fun stopBluetoothScan() {
        bluetoothControllerManager.stopDiscovery()
    }

    /**
     * Dispatches an asynchronous cryptographic pairing request loop targeting the peripheral address.
     */
    fun pairDevice(address: String) {
        bluetoothControllerManager.pairDevice(address)
    }

    /**
     * Processes raw continuous joystick coordinate input values into scaled drive and angular configurations.
     */
    fun onControllerInput(x: Float, y: Float) {
        val steering = if (x > 0) x * x else -(x * x)
        val turnEffort = abs(steering)
        val linearSpeed = y * (BASE_SPEED_FACTOR - (turnEffort * TURN_EFFORT_PENALTY_WEIGHT))
        val turn = steering * TURN_MULTIPLIER

        lastX = turn
        lastY = linearSpeed
    }

    /**
     * Loads remembered system-level authenticated hardware profiles into state collection feeds.
     */
    fun loadPairedDevices() {
        bluetoothControllerManager.loadPairedDevices()
    }

    /**
     * Halts active hardware components and safely disposes subsystem registration layers.
     */
    override fun onCleared() {
        movementController.stop()
        bluetoothControllerManager.release()
        super.onCleared()
    }
}
