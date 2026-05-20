package hka.awp.cgi.temi.app.feature.settings.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow

@Suppress("LongParameterList")
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
        SettingsHeader(
            title = stringResource(R.string.settings_display_subtitle),
            onBackClick = onBackClick
                      )

        Spacer(modifier = Modifier.height(40.dp))

        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.Brightness6,
                title = stringResource(R.string.display_brightness)
                       )

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = 0f..1f
                  )

            Text(
                text = stringResource(
                    R.string.display_brightness_percent,
                    (brightness * 100).toInt()
                                     ),
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.bodyMedium
                )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard(onClick = onTimeoutClick) {
            SettingsRow(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.display_screensaver),
                subtitle = stringResource(
                    R.string.display_screensaver_after,
                    currentTimeoutLabel
                                         ),
                action = {
                    Text(
                        text = stringResource(R.string.display_change),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                        )
                }
                       )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard {
            SettingsRow(
                icon = if (isDarkMode)
                    Icons.Rounded.DarkMode
                else
                    Icons.Rounded.LightMode,

                title = stringResource(R.string.display_dark_mode),

                subtitle =
                    if (isDarkMode)
                        stringResource(R.string.display_enabled)
                    else
                        stringResource(R.string.display_disabled),

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
