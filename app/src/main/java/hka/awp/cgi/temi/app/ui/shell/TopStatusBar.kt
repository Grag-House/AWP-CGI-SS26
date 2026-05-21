package hka.awp.cgi.temi.app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.BatteryIndicator
import hka.awp.cgi.temi.app.utils.NetworkManager

/**
 * A component that represents the top status bar of the application.
 *
 * It displays system information including the current Wi-Fi signal level,
 * battery status, current time, and notification status.
 */
@Composable
fun TopStatusBar(
    wifiLevel: Int,
    currentTime: String,
    batteryLevel: Int?,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            NetworkManager.getWifiIconForLevel(wifiLevel),
            contentDescription = stringResource(R.string.wifi_description),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        BatteryIndicator(
            level = batteryLevel,
            isCharging = isCharging
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = currentTime,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            Icons.Default.Notifications,
            contentDescription = stringResource(R.string.notifications_description),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
