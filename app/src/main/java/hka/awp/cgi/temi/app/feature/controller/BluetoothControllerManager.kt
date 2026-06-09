package hka.awp.cgi.temi.app.feature.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class BluetoothControllerManager(
    private val context: Context,
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _devices = MutableStateFlow<List<ControllerDevice>>(emptyList())
    val devices: StateFlow<List<ControllerDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        private fun updateConnectionState(
            address: String,
            isConnected: Boolean,
                                         ) {
            _devices.value = _devices.value.map {
                if (it.address == address) {
                    it.copy(isConnected = isConnected)
                } else {
                    it
                }
            }
        }
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    device?.let {
                        addOrUpdateDevice(it)
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    device?.let {
                        addOrUpdateDevice(it)
                        Timber.d("Bond state changed: ${it.safeName()} ${it.safeBondState()}")
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    Timber.d("Bluetooth discovery finished")
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    device?.let {
                        updateConnectionState(it.address, true)
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    device?.let {
                        updateConnectionState(it.address, false)
                    }
                }
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

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeBondState(): Int {
        return try {
            bondState
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission while reading bond state")
            BluetoothDevice.BOND_NONE
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun connectHidDevice(address: String) {
        try {
            val adapter = bluetoothAdapter ?: return
            val device = adapter.getRemoteDevice(address)

            val getProfileProxyMethod = BluetoothAdapter::class.java.getMethod(
                "getProfileProxy",
                Context::class.java,
                BluetoothProfile.ServiceListener::class.java,
                Int::class.javaPrimitiveType,
            )

            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    Timber.d("Bluetooth profile connected: profile=$profile proxy=${proxy.javaClass.name}")

                    try {
                        val connectMethod = proxy.javaClass.getMethod(
                            "connect",
                            BluetoothDevice::class.java,
                        )

                        val result = connectMethod.invoke(proxy, device)

                        Timber.d("HID connect result=$result for ${device.safeName()} ${device.address}")
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to invoke HID connect")
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Timber.d("Bluetooth profile disconnected: profile=$profile")
                }
            }

            val inputDeviceProfile = 4

            val result = getProfileProxyMethod.invoke(
                adapter,
                context,
                listener,
                inputDeviceProfile,
            )

            Timber.d("getProfileProxy HID result=$result for $address")
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission for HID connect")
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to connect HID device via reflection")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun disconnectHidDevice(address: String) {
        try {
            val adapter = bluetoothAdapter ?: return
            val device = adapter.getRemoteDevice(address)

            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        val disconnectMethod = proxy.javaClass.getMethod(
                            "disconnect",
                            BluetoothDevice::class.java,
                                                                        )

                        val result = disconnectMethod.invoke(proxy, device)
                        Timber.d("HID disconnect result=$result for ${device.safeName()} ${device.address}")
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to invoke HID disconnect")
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Timber.d("Bluetooth profile disconnected: profile=$profile")
                }
            }

            val inputDeviceProfile = 4

            val result = adapter.getProfileProxy(
                context,
                listener,
                inputDeviceProfile,
                                                )

            Timber.d("getProfileProxy HID disconnect result=$result for $address")
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission for HID disconnect")
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to disconnect HID device")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun removeBond(address: String) {
        try {
            val device = bluetoothAdapter?.getRemoteDevice(address)

            if (device == null) {
                Timber.e("Bluetooth device not found for address=$address")
                return
            }

            stopDiscovery()

            val removeBondMethod = device.javaClass.getMethod("removeBond")
            val result = removeBondMethod.invoke(device)

            Timber.d("removeBond result=$result for ${device.safeName()} ${device.address}")

            _devices.value = _devices.value.filterNot {
                it.address == address
            }
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission while removing bond")
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to remove bond")
        }
    }

    @SuppressLint("MissingPermission")
    fun logPairedDevices() {
        try {
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                Timber.d(
                    "Paired Bluetooth device: name=${device.safeName()}, " +
                        "address=${device.address}, bondState=${device.bondState}"
                )
            }
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while reading paired devices")
        }
    }

    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        try {
            val pairedDevices = bluetoothAdapter
                ?.bondedDevices
                ?.map { it.toControllerDevice() }
                .orEmpty()

            _devices.value = pairedDevices

            pairedDevices.forEach {
                Timber.d("Loaded paired device: $it")
            }
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while loading paired devices")
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        try {
            if (bluetoothAdapter?.isEnabled != true) {
                Timber.e("Bluetooth is not enabled")
                return
            }

            _devices.value = emptyList()
            loadPairedDevices()
            logPairedDevices()

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            val started = bluetoothAdapter.startDiscovery()
            _isScanning.value = started

            Timber.d("Bluetooth discovery started=$started")
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
            val device = bluetoothAdapter?.getRemoteDevice(address)

            if (device == null) {
                Timber.e("Bluetooth device not found for address=$address")
                return
            }

            stopDiscovery()

            val started = device.createBond()
            Timber.d("Pairing started=$started for ${device.safeName()} $address")
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing Bluetooth permission while pairing device")
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception, "Invalid Bluetooth address=$address")
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

    @SuppressLint("MissingPermission")
    private fun addOrUpdateDevice(device: BluetoothDevice) {
        val controllerDevice = device.toControllerDevice()

        Timber.d(
            "Found Bluetooth device: name=${controllerDevice.name}, " +
                "address=${controllerDevice.address}, bondState=${controllerDevice.bondState}"
        )

        _devices.value = _devices.value
            .filterNot { it.address == controllerDevice.address }
            .plus(controllerDevice)
            .sortedBy { it.name }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toControllerDevice(): ControllerDevice {
        return ControllerDevice(
            name = safeName(),
            address = address,
            bondState = safeBondState(),
        )
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String {
        return try {
            name ?: "Unbekanntes Gerät"
        } catch (exception: SecurityException) {
            "Unbekanntes Gerät"
        }
    }
}
