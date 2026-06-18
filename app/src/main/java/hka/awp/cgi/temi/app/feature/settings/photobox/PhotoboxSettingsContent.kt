package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow

@Suppress("LongParameterList")
@Composable
fun PhotoboxSettingsContent(
    onBackClick: () -> Unit,
    overlayEnabled: Boolean,
    onOverlayEnabledChange: (Boolean) -> Unit,
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

        UploadSettingsCard(
            driveFolderLink = driveFolderLink,
            driveUploadUrl = driveUploadUrl,
            onSave = onSaveUploadSettings
        )
    }
}

@Composable
private fun UploadSettingsCard(
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
