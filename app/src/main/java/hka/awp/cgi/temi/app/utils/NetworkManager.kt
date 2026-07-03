package hka.awp.cgi.temi.app.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

/**
 * Utility class for managing and monitoring network connectivity and Wi-Fi signal strength.
 *
 * This class provides reactive access to the current Wi-Fi signal level using Kotlin Flows.
 * It observes system broadcasts to update the signal level automatically when it changes.
 *
 * @property context The application context used to access system network services.
 */
class NetworkManager(private val context: Context) {
    private val connectivityManager = context.applicationContext.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * A [Flow] that emits the current Wi-Fi signal level (0-4) or 0 if not connected.
     * The flow updates whenever the system reports a change in Wi-Fi signal strength.
     */
    val wifiSignalLevel: Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiManager.RSSI_CHANGED_ACTION) {
                    trySend(calculateSignalLevel())
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))

        // Emit initial value
        trySend(calculateSignalLevel())

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.onStart {
        emit(calculateSignalLevel())
    }.distinctUntilChanged()

    /**
     * Calculates the current Wi-Fi signal level.
     *
     * @return An integer between 0 and 4 representing signal strength, or 0 if no Wi-Fi.
     */
    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE]
    )
    private fun calculateSignalLevel(): Int {
        val network = connectivityManager.activeNetwork ?: return 0
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            val rssi = wifiInfo.rssi
            @Suppress("DEPRECATION")
            return WifiManager.calculateSignalLevel(rssi, SIGNAL_LEVELS)
        }
        return 0
    }

    /**
     * Returns an [ImageVector] icon corresponding to a given Wi-Fi signal level.
     *
     * @param level The signal level (0-4).
     * @return The matching [ImageVector] for the icon.
     */
    @Suppress("MagicNumber")
    fun getWifiIconForLevel(level: Int): ImageVector = when (level) {
        0 -> Icons.Rounded.SignalWifi0Bar
        1 -> Icons.Rounded.NetworkWifi1Bar
        2 -> Icons.Rounded.NetworkWifi2Bar
        3 -> Icons.Rounded.NetworkWifi3Bar
        4 -> Icons.Rounded.SignalWifi4Bar
        else -> Icons.Rounded.SignalWifiOff
    }

    companion object {
        /** Total number of signal level bars to calculate. */
        private const val SIGNAL_LEVELS = 5
    }
}
