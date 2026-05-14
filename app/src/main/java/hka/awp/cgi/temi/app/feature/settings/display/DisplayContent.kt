package hka.awp.cgi.temi.app.feature.settings.display

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow


@Composable
fun DisplayContent(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    currentTimeoutLabel: String,
    onTimeoutClick: () -> Unit,
    onBackClick: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        // 1. Header
        SettingsHeader(
            title = "Anzeige & Helligkeit",
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Brightness Card
        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.Brightness6,
                title = "Bildschirmhelligkeit"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = 0f..1f
            )

            Text(
                text = "${(brightness * 100).toInt()}%",
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Bildschirmschoner-Card
        SettingsCard(onClick = onTimeoutClick) {
            SettingsRow(
                icon = Icons.Rounded.Timer,
                title = "Bildschirmschoner",
                subtitle = "Startet nach $currentTimeoutLabel Inaktivität",
                action = {
                    Text(
                        text = "Ändern",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Dark Mode Card
        SettingsCard {
            SettingsRow(
                icon = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                title = "Dark Mode",
                subtitle = if (isDarkMode) "Eingeschaltet" else "Ausgeschaltet",
                action = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange
                    )
                }
            )
        }
    }
}
