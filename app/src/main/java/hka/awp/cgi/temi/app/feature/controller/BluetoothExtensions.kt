package hka.awp.cgi.temi.app.feature.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import timber.log.Timber

private const val UNKNOWN_BLUETOOTH_DEVICE_NAME = "Unbekanntes Gerät"

/**
 * Safely extracts the [BluetoothDevice] payload from an incoming broadcast intent.
 * * Resolves platform API compatibility deprecation variants gracefully by using the type-safe
 * [Intent.getParcelableExtra] flavor on Android Tiramisu (API 33) and above, while falling
 * back to the legacy extractor on older SDK baselines.
 *
 * @return The attached [BluetoothDevice] peripheral instance, or null if the extra data payload is absent.
 */
fun Intent.getBluetoothDeviceExtra(): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}

/**
 * Safely queries the current hardware bond state of the remote peripheral.
 *
 * Catches framework level [SecurityException] faults silently if the required Bluetooth
 * runtime hardware permissions have not been granted or were revoked mid-lifecycle.
 *
 * @return The integer representations matching [BluetoothDevice.BOND_BONDED], [BluetoothDevice.BOND_BONDING],
 * or falls back to [BluetoothDevice.BOND_NONE] upon permission failures.
 */
@SuppressLint("MissingPermission")
fun BluetoothDevice.safeBondState(): Int {
    return try {
        bondState
    } catch (exception: SecurityException) {
        Timber.e(exception, "Missing permission while reading bond state")
        BluetoothDevice.BOND_NONE
    }
}

/**
 * Safely retrieves the user-facing hardware broadcasting name signature of the remote peripheral.
 *
 * Wraps system-level framework queries inside a runtime catch block to safely intercept
 * [SecurityException] permission faults, returning a localized static fallback string asset if needed.
 *
 * @return The string name signature broadcast by the device, or [UNKNOWN_BLUETOOTH_DEVICE_NAME] on empty/failed fields.
 */
@SuppressLint("MissingPermission")
fun BluetoothDevice.safeName(): String {
    return try {
        name ?: UNKNOWN_BLUETOOTH_DEVICE_NAME
    } catch (exception: SecurityException) {
        Timber.e(exception, "Missing permission while reading Bluetooth device name")
        UNKNOWN_BLUETOOTH_DEVICE_NAME
    }
}

/**
 * Maps an unmanaged platform-level [BluetoothDevice] framework handle into an application-specific,
 * decoupled immutable [ControllerDevice] data architecture layer structure.
 *
 * This utility acts as an internal state-transformer entity, pipeline-shielding domain data layers from
 * breaking low-level Android hardware changes.
 *
 * @return An immutable [ControllerDevice] snapshot capturing safe name, hardware MAC, and bonding properties.
 */
fun BluetoothDevice.toControllerDevice(): ControllerDevice {
    return ControllerDevice(
        name = safeName(),
        address = address,
        bondState = safeBondState(),
    )
}
