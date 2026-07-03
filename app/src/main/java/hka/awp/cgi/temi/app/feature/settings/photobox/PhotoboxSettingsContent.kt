package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxBanner
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow

/**
 * Renders the stateless layout content for the Photobox settings screen.
 *
 * This view consolidates a variety of configuration options tailored for the robot's photobox functionality,
 * including image frame overlay toggles, customizable graphic banner watermarks, and input fields
 * for backend Google Drive upload automation parameters.
 *
 * @param onBackClick Executed when the user interacts with the navigation back button in the header.
 * @param overlayEnabled Boolean flag indicating whether the image frame overlay layer is active.
 * @param onOverlayEnabledChange Callback triggered when the overlay switch state shifts.
 * @param bannerEnabled Boolean flag indicating whether the promotional banner watermark is active.
 * @param onBannerEnabledChange Callback triggered when the banner switch state shifts.
 * @param banner The currently active [PhotoboxBanner] choice selected for image embedding.
 * @param onBannerSelect Callback triggered when a different banner choice type is selected.
 * @param driveFolderLink The current folder URL reference string pointing to the targeted remote storage directory.
 * @param driveUploadUrl The backend service endpoint link handling automated upload requests.
 * @param onSaveUploadSettings Callback triggered when saving updated input credentials for the storage paths.
 */
@Suppress("LongParameterList")
@Composable
fun PhotoboxSettingsContent(
    onBackClick: () -> Unit,
    overlayEnabled: Boolean,
    onOverlayEnabledChange: (Boolean) -> Unit,
    bannerEnabled: Boolean,
    onBannerEnabledChange: (Boolean) -> Unit,
    banner: PhotoboxBanner,
    onBannerSelect: (PhotoboxBanner) -> Unit,
    driveFolderLink: String,
    driveUploadUrl: String,
    onSaveUploadSettings: (folderLink: String, uploadUrl: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

        Spacer(modifier = Modifier.height(20.dp))

        BannerSettingsCard(
            bannerEnabled = bannerEnabled,
            onBannerEnabledChange = onBannerEnabledChange,
            banner = banner,
            onBannerSelect = onBannerSelect
        )

        Spacer(modifier = Modifier.height(20.dp))

        UploadSettingsCard(
            driveFolderLink = driveFolderLink,
            driveUploadUrl = driveUploadUrl,
            onSave = onSaveUploadSettings
        )
    }
}
