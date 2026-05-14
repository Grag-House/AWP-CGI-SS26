package hka.awp.cgi.temi.app.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
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
 * @property context The application context used to access system services like [ConnectivityManager]
 * and [WifiManager].
 */
class NetworkManager(val context: Context) {
    private val connectivityManager = context.applicationContext.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    companion object {
        @Suppress("MagicNumber")
        fun getWifiIconForLevel(level: Int): ImageVector = when (level) {
            0 -> Icons.Rounded.SignalWifi0Bar
            1 -> Icons.Rounded.NetworkWifi1Bar
            2 -> Icons.Rounded.NetworkWifi2Bar
            3 -> Icons.Rounded.NetworkWifi3Bar
            4 -> Icons.Rounded.SignalWifi4Bar
            else -> Icons.Rounded.SignalWifiOff
        }
    }

    // this will only run on android sdk 23, therefore usage of now deprecated methods is fine / necessary
    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE]
    )
    fun getWifiSignalLevel(): Int {
        val network = connectivityManager.activeNetwork ?: return 0
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            val rssi = wifiInfo.rssi
            @Suppress("DEPRECATION", "MagicNumber")
            return WifiManager.calculateSignalLevel(rssi, 5)
        }
        return 0
    }
}
