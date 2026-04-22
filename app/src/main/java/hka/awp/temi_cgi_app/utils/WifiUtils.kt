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
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.NetworkWifi1Bar
import androidx.compose.material.icons.rounded.NetworkWifi2Bar
import androidx.compose.material.icons.rounded.NetworkWifi3Bar
import androidx.compose.material.icons.rounded.SignalWifi0Bar
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.ui.graphics.vector.ImageVector

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun getWifiSignalLevel(context: Context): Int {
    val connectivityManger = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManger.activeNetwork  ?: return 0
    val capabilities = connectivityManger.getNetworkCapabilities(network) ?: return 0

    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val transportInfo = capabilities.transportInfo

            if (transportInfo is WifiInfo) {
                val rssi = transportInfo.rssi
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android >= 11 will return a value from 0 to the max level of the device
                    wifiManager.calculateSignalLevel(rssi)
                } else {
                    // Android <= 10
                    WifiManager.calculateSignalLevel(rssi, 5)
                }
            }
        }
    }
    return 0
}

fun getWifiIconForLevel(level: Int): ImageVector{
    return when(level){
        0 ->  Icons.Rounded.SignalWifi0Bar
        1 -> Icons.Rounded.NetworkWifi1Bar
        2 -> Icons.Rounded.NetworkWifi2Bar
        3 -> Icons.Rounded.NetworkWifi3Bar
        4 -> Icons.Rounded.NetworkWifi
        else -> Icons.Rounded.SignalWifiOff
    }
}