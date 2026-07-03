package hka.awp.cgi.temi.app.feature.settings.battery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow

/**
 * Renders the main content screen for the battery settings section.
 *
 * This component displays the current battery level percentage using a visual progress bar
 * and contextual text coloration (e.g., shifts to red when low). It also outlines the
 * system's active power status, distinguishing whether the hardware is running on battery mode
 * or actively charging.
 *
 * @param batteryLevel The current charge percentage of the battery (expected range: 0 to 100).
 * @param isCharging A boolean flag indicating whether the device is connected to a power supply.
 * @param onBackClick Fired when the user interacts with the navigation back button in the header.
 */
@Composable
fun BatteryContent(
    batteryLevel: Int,
    isCharging: Boolean,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        SettingsHeader(
            title = stringResource(
                R.string.battery
            ),
            onBackClick = onBackClick
        )

        Spacer(
            modifier = Modifier.height(40.dp)
        )

        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.BatteryFull,
                title = stringResource(
                    R.string.battery_status
                ),
                subtitle =
                if (isCharging) {
                    stringResource(
                        R.string.battery_charging
                    )
                } else {
                    stringResource(
                        R.string.battery_not_charging
                    )
                },
                action = {
                    Text(
                        text = "$batteryLevel%",
                        style = MaterialTheme.typography.headlineSmall,
                        color =
                        if (batteryLevel < 20) {
                            Color.Red
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            LinearProgressIndicator(
                progress = { batteryLevel / 100f },
                modifier = Modifier.fillMaxWidth(),
                color =
                if (batteryLevel < 20) {
                    Color.Red
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor =
                MaterialTheme.colorScheme.outlineVariant
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.Bolt,
                title = stringResource(
                    R.string.power_status
                ),
                subtitle =
                if (isCharging) {
                    stringResource(
                        R.string.currently_charging
                    )
                } else {
                    stringResource(
                        R.string.battery_mode_active
                    )
                }
            )
        }
    }
}
