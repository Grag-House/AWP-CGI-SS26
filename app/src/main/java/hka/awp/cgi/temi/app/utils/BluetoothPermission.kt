package hka.awp.cgi.temi.app.utils

import android.Manifest
import android.os.Build

/**
 * Provides the required manifest permission string cluster based on execution SDK level.
 */
fun getBluetoothPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}
