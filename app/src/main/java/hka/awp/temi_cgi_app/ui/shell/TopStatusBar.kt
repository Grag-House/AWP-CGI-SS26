package hka.awp.temi_cgi_app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hka.awp.temi_cgi_app.utils.NetworkManager

/**
 * A component that represents the top status bar of the application.
 *
 * It displays system information including the current Wi-Fi signal level,
 * battery status, current time, and notification status.
 */
@Composable
fun TopStatusBar(wifiLevel: Int, currentTime: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            NetworkManager.getWifiIconForLevel(wifiLevel),
            contentDescription = "WLAN",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Default.BatteryFull,
            contentDescription = "Batterie",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = currentTime, color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            Icons.Default.Notifications,
            contentDescription = "Benachrichtigungen",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}