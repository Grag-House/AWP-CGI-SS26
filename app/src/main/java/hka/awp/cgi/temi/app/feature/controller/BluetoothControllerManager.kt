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

class BluetoothControllerManager(
    private val context: Context
                                ) {
    private val bluetoothAdapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter
    private val hidConnector = BluetoothHidConnector(context, bluetoothAdapter)

    private val _devices = MutableStateFlow<List<ControllerDevice>>(emptyList())
    val devices: StateFlow<List<ControllerDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

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

    fun connectHidDevice(address: String) = hidConnector.connect(address)

    fun disconnectHidDevice(address: String) = hidConnector.disconnect(address)

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

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
            _isScanning.value = false
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while stopping discovery")
        }
    }

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

    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        try {
            _devices.value = bluetoothAdapter?.bondedDevices?.map { it.toControllerDevice() }.orEmpty()
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while loading paired devices")
        }
    }

    fun release() {
        stopDiscovery()
        try {
            context.unregisterReceiver(receiver)
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception, "Bluetooth receiver was not registered")
        }
    }

    private fun updateConnectionState(address: String, isConnected: Boolean) {
        _devices.value = _devices.value.map {
            if (it.address == address) it.copy(isConnected = isConnected) else it
        }
    }

    private fun addOrUpdateDevice(device: BluetoothDevice) {
        val controllerDevice = device.toControllerDevice()
        _devices.value = _devices.value
            .filterNot { it.address == controllerDevice.address }
            .plus(controllerDevice)
            .sortedBy { it.name }
    }
}
