package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxBanner
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsRow

/**
 * A standalone settings card layout for toggling and selecting photobox watermark banners.
 *
 * It features a primary master switch to enable or disable watermark overlays entirely.
 * When activated, it reveals a horizontal row of selectable layout variations defined within [PhotoboxBanner].
 *
 * @param bannerEnabled Boolean flag specifying whether promotional banner watermarks are active.
 * @param onBannerEnabledChange Callback triggered when the master switch state changes.
 * @param banner The currently highlighted banner style configuration.
 * @param onBannerSelect Callback triggered when the user picks a different banner option layout.
 */
@Composable
fun BannerSettingsCard(
    bannerEnabled: Boolean,
    onBannerEnabledChange: (Boolean) -> Unit,
    banner: PhotoboxBanner,
    onBannerSelect: (PhotoboxBanner) -> Unit
) {
    SettingsCard {
        SettingsRow(
            icon = Icons.AutoMirrored.Rounded.BrandingWatermark,
            title = stringResource(R.string.photobox_banner_title),
            subtitle = if (bannerEnabled) {
                stringResource(R.string.display_enabled)
            } else {
                stringResource(R.string.display_disabled)
            },
            action = {
                Switch(
                    checked = bannerEnabled,
                    onCheckedChange = onBannerEnabledChange
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.photobox_banner_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        if (bannerEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoboxBanner.entries.forEach { option ->
                    val isSelected = option == banner
                    Surface(
                        onClick = { onBannerSelect(option) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                text = stringResource(option.labelRes),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private val PhotoboxBanner.labelRes: Int
    get() = when (this) {
        PhotoboxBanner.FIFTY_YEARS_CGI -> R.string.photobox_banner_50_years_cgi
        PhotoboxBanner.CGI_LAB -> R.string.photobox_banner_cgi_lab
    }

/**
 * A stateful-input settings card layout tailored for managing photobox cloud storage properties.
 *
 * This card holds text fields for specifying remote Google Drive target credentials. It safely manages
 * internal input buffers using remember storage, syncing with global states through local side-effects,
 * and provides a structured action button to pass finalized configurations back to backend observers.
 *
 * @param driveFolderLink The current folder URL reference string pointing to the targeted remote storage directory.
 * @param driveUploadUrl The backend service endpoint link handling automated upload requests.
 * @param onSave Callback block fired when changes are submitted, supplying the modified folder link and upload URL.
 */
@Composable
fun UploadSettingsCard(
    driveFolderLink: String,
    driveUploadUrl: String,
    onSave: (folderLink: String, uploadUrl: String) -> Unit
) {
    var folderLinkInput by remember { mutableStateOf(driveFolderLink) }
    var uploadUrlInput by remember { mutableStateOf(driveUploadUrl) }

    LaunchedEffect(driveFolderLink) { folderLinkInput = driveFolderLink }
    LaunchedEffect(driveUploadUrl) { uploadUrlInput = driveUploadUrl }

    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsRow(
                icon = Icons.Rounded.CloudUpload,
                title = stringResource(R.string.photobox_upload_section_title)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.photobox_upload_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = folderLinkInput,
            onValueChange = { folderLinkInput = it },
            label = { Text(stringResource(R.string.photobox_drive_folder_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uploadUrlInput,
            onValueChange = { uploadUrlInput = it },
            label = { Text(stringResource(R.string.photobox_drive_upload_url_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSave(folderLinkInput, uploadUrlInput) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.photobox_upload_save_button))
        }
    }
}
