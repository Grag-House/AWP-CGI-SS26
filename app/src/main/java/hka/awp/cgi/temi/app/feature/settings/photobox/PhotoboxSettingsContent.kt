package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow

@Composable
fun PhotoboxSettingsContent(
    onBackClick: () -> Unit,
    overlayEnabled: Boolean,
    onOverlayEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        SettingsHeader(
            title = stringResource(R.string.settings_photobox_subtitle),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.photobox_overlay_title),
                subtitle = if (overlayEnabled) {
                    stringResource(R.string.display_enabled)
                } else {
                    stringResource(R.string.display_disabled)
                },
                action = {
                    Switch(
                        checked = overlayEnabled,
                        onCheckedChange = onOverlayEnabledChange
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.photobox_overlay_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
