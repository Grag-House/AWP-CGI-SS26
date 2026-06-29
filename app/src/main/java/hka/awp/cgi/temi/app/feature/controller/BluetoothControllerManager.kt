package hka.awp.cgi.temi.app.feature.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Central infrastructure manager for controlling Bluetooth input peripherals (gamepads/controllers).
 *
 * This class encapsulates low-level interactions with the [BluetoothAdapter] and processes
 * asynchronous hardware events via an internal [BroadcastReceiver]. The active state
 * (discovered devices, scanning status) is exposed via reactive [StateFlow] pipelines for UI consumption.
 * The actual HID profile connection sequence is delegated directly to the [BluetoothHidConnector].
 *
 * @property context The application context used for registering system broadcast receivers.
 */
class BluetoothControllerManager(
    private val context: Context
) {
    private val bluetoothAdapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter
    private val hidConnector = BluetoothHidConnector(context, bluetoothAdapter)

    private val _devices = MutableStateFlow<List<ControllerDevice>>(emptyList())

    /**
     * An observable reactive data stream providing the currently known, paired, and discovered
     * Bluetooth controllers in the environment, sorted alphabetically by name.
     */
    val devices: StateFlow<List<ControllerDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)

    /**
     * An observable reactive data stream indicating whether the Bluetooth radio is currently
     * actively scanning for nearby peripherals.
     */
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /**
     * Internal receiver handling system-wide Bluetooth broadcasts.
     * Reacts to discovered devices, bond state mutations, and active ACL connection events.
     */
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.getBluetoothDeviceExtra()

            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> device?.let { addOrUpdateDevice(it) }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> device?.let {
                    addOrUpdateDevice(it)
                    Timber.d("Bond state changed: ${it.safeName()} ${it.safeBondState()}")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    Timber.d("Bluetooth discovery finished")
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> device?.let { updateConnectionState(it.address, true) }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> device?.let { updateConnectionState(it.address, false) }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
    }

    /**
     * Initiates a connection link to the Human Interface Device
     * (HID) profile of the peripheral matching the specified MAC address.
     *
     * @param address The hardware MAC address of the targeted peripheral.
     */
    fun connectHidDevice(address: String) = hidConnector.connect(address)

    /**
     * Tears down an active HID profile proxy channel targeting the designated hardware MAC address.
     *
     * @param address The hardware MAC address of the targeted peripheral.
     */
    fun disconnectHidDevice(address: String) = hidConnector.disconnect(address)

    /**
     * Removes the bond pairing record associated with the designated hardware address from the host OS.
     *
     * Halts any active radio discovery sweeps beforehand to avoid system transaction conflicts,
     * and clears the target model instance from the internal state flow collection upon completion.
     *
     * @param address The unique hardware MAC address key matching the paired peripheral targeted for removal.
     */
    @Suppress("TooGenericExceptionCaught")
    fun removeBond(address: String) {
        try {
            val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
            stopDiscovery()

            val removeBondMethod = device.javaClass.getMethod("removeBond")
            removeBondMethod.invoke(device)

            _devices.value = _devices.value.filterNot { it.address == address }
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission while removing bond")
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to remove bond")
        }
    }

    /**
     * Initiates an active asynchronous OTA radio scanning loop to trace non-bonded remote peripherals.
     *
     * Clears the current device history state, reloads bonded devices, and cancels any ongoing
     * system discovery sweeps beforehand to cleanly reset radio state cycles.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        try {
            if (bluetoothAdapter?.isEnabled != true) return

            _devices.value = emptyList()
            loadPairedDevices()

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            _isScanning.value = bluetoothAdapter.startDiscovery()
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while starting discovery")
        }
    }

    /**
     * Requests an explicit termination intercept signal to halt ongoing host radio discovery sweeps.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
            _isScanning.value = false
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while stopping discovery")
        }
    }

    /**
     * Dispatches an asynchronous cryptographic pairing request sequence to a specified target MAC endpoint.
     * Cancels active discovery searches beforehand to maximize link stability during key exchanges.
     *
     * @param address The unique target hardware MAC endpoint address string to initialize authentications with.
     */
    @SuppressLint("MissingPermission")
    fun pairDevice(address: String) {
        try {
            val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
            stopDiscovery()
            device.createBond()
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while pairing device")
        }
    }

    /**
     * Queries cached local radio data structures to load bonded system elements directly
     * into the mutable [_devices] state flow pipeline.
     */
    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        try {
            _devices.value = bluetoothAdapter?.bondedDevices?.map { it.toControllerDevice() }.orEmpty()
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while loading paired devices")
        }
    }

    /**
     * Disposes and unbinds resource references, halting discovery passes and unregistering
     * the internal broadcast receiver instance to prevent platform-level memory leaks.
     */
    fun release() {
        stopDiscovery()
        try {
            context.unregisterReceiver(receiver)
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception, "Bluetooth receiver was not registered")
        }
    }

    /**
     * Updates the internal tracking connection state flow topology
     * (ACL layer) matching a specific hardware target signature.
     */
    private fun updateConnectionState(address: String, isConnected: Boolean) {
        _devices.value = _devices.value.map {
            if (it.address == address) it.copy(isConnected = isConnected) else it
        }
    }

    /**
     * Intercepts a newly discovered device model, filters existing duplicate entries out from
     * current data history flow states, and performs an alphabetical sort pass by name.
     */
    private fun addOrUpdateDevice(device: BluetoothDevice) {
        val controllerDevice = device.toControllerDevice()
        _devices.value = _devices.value
            .filterNot { it.address == controllerDevice.address }
            .plus(controllerDevice)
            .sortedBy { it.name }
    }
}
