package hka.awp.temi_cgi_app.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NetworkWifi1Bar
import androidx.compose.material.icons.rounded.NetworkWifi2Bar
import androidx.compose.material.icons.rounded.NetworkWifi3Bar
import androidx.compose.material.icons.rounded.SignalWifi0Bar
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Utility class for managing and retrieving network-related information, specifically focusing on
 * Wi-Fi connectivity and signal strength.
 *
 * @property context The application context used to access system services like [ConnectivityManager] and [WifiManager].
 */
data class NetworkManager(
    val context: Context, private val sdkVersion: Int = Build.VERSION.SDK_INT
) {
    companion object {
        fun getWifiIconForLevel(level: Int): ImageVector {
            return when (level) {
                0 -> Icons.Rounded.SignalWifi0Bar
                1 -> Icons.Rounded.NetworkWifi1Bar
                2 -> Icons.Rounded.NetworkWifi2Bar
                3 -> Icons.Rounded.NetworkWifi3Bar
                4 -> Icons.Rounded.SignalWifi4Bar
                else -> Icons.Rounded.SignalWifiOff
            }
        }
    }

    //TODO check for temi android SDK later on, so this can be streamlined (e. g. remove dead paths)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE])
    fun getWifiSignalLevel(context: Context): Int {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val network = connectivityManager.activeNetwork ?: return 0
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val transportInfo = capabilities.transportInfo
                if (transportInfo is WifiInfo) {
                    val rssi = transportInfo.rssi
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // anrdoid sdk 30+
                        wifiManager.calculateSignalLevel(rssi)
                    } else {
                        // android sdk 29
                        @Suppress("DEPRECATION") WifiManager.calculateSignalLevel(rssi, 5)
                    }
                }
            }
        } else {
            // 2. Legacy (sdk 24 - 28)
            @Suppress("DEPRECATION") val wifiInfo = wifiManager.connectionInfo
            @Suppress("DEPRECATION") return WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
        }
        return 0
    }
}
