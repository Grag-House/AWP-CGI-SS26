package hka.awp.cgi.temi.app.feature.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import timber.log.Timber

private const val UNKNOWN_BLUETOOTH_DEVICE_NAME = "Unbekanntes Gerät"

fun Intent.getBluetoothDeviceExtra(): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}

@SuppressLint("MissingPermission")
fun BluetoothDevice.safeBondState(): Int {
    return try {
        bondState
    } catch (exception: SecurityException) {
        Timber.e(exception, "Missing permission while reading bond state")
        BluetoothDevice.BOND_NONE
    }
}

@SuppressLint("MissingPermission")
fun BluetoothDevice.safeName(): String {
    return try {
        name ?: UNKNOWN_BLUETOOTH_DEVICE_NAME
    } catch (exception: SecurityException) {
        Timber.e(exception, "Missing permission while reading Bluetooth device name")
        UNKNOWN_BLUETOOTH_DEVICE_NAME
    }
}

fun BluetoothDevice.toControllerDevice(): ControllerDevice {
    return ControllerDevice(
        name = safeName(),
        address = address,
        bondState = safeBondState(),
                           )
}
