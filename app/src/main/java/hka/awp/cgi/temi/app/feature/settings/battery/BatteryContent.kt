package hka.awp.cgi.temi.app.feature.settings.battery

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow


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
        // 1. Dein Header
        SettingsHeader(
            title = "Akku",
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(40.dp))

        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.BatteryFull,
                title = "Akkustatus",
                subtitle = if (isCharging) "Wird geladen..." else "Wird aktuell nicht geladen.",
                action = {
                    Text(
                        text = "$batteryLevel%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (batteryLevel < 20) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { batteryLevel / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = if (batteryLevel < 20) Color.Red else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.Bolt,
                title = "Energieverbrauch",
                subtitle = "Aktuelle Nutzung wird vom System optimiert"
            )
        }
    }
}
