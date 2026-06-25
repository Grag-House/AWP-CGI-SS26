package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R

private const val LAT_MIN = -90.0
private const val LAT_MAX = 90.0
private const val LON_MIN = -180.0
private const val LON_MAX = 180.0

@Composable
fun VoiceProfileList(profiles: List<String>, onDeleteClick: (String) -> Unit) {
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        profiles.forEach { profileName ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(profileName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                IconButton(
                    onClick = { onDeleteClick(profileName) },
                    modifier = Modifier.width(32.dp).height(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        "Profile löschen",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.width(16.dp).height(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onConfirm: (newPassword: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, null) },
        title = { Text(stringResource(R.string.admin_panel_change_password_title)) },
        text = {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text(stringResource(R.string.admin_panel_new_password)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(newPassword) }, enabled = newPassword.isNotBlank()) {
                Text(stringResource(R.string.admin_panel_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_panel_cancel)) }
        }
    )
}

@Composable
fun ProfileNameInputDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var nameInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Profil erstellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Geben Sie einen Namen für das Sprachprofil ein:")
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Profilname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nameInput.trim().isNotEmpty()) onConfirm(nameInput.trim()) },
                enabled = nameInput.trim().isNotEmpty()
            ) { Text("Lernen starten") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
fun DeleteProfileConfirmDialog(profileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil löschen") },
        text = { Text("Möchten Sie das Profil \"$profileName\" wirklich löschen?") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Löschen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
fun ResetVoiceProfilesDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stimmen zurücksetzen") },
        text = { Text("Möchten Sie wirklich alle gespeicherten Sprachprofile löschen?") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Löschen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_panel_cancel)) }
        }
    )
}

@Composable
fun EditCoordinatesDialog(
    initialLatitude: Double,
    initialLongitude: Double,
    onConfirm: (latitude: Double, longitude: Double) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var latitudeInput by remember { mutableStateOf(initialLatitude.toString()) }
    var longitudeInput by remember { mutableStateOf(initialLongitude.toString()) }
    val latValue = latitudeInput.toDoubleOrNull()
    val lonValue = longitudeInput.toDoubleOrNull()
    val isLatError = latValue == null || (latValue < LAT_MIN) || (latValue > LAT_MAX)
    val isLonError = lonValue == null || (lonValue < LON_MIN) || (lonValue > LON_MAX)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.LocationOn, null) },
        title = { Text(stringResource(R.string.admin_panel_webserver_coordiantes_change)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = latitudeInput,
                    onValueChange = { latitudeInput = it },
                    label = { Text(stringResource(R.string.admin_panel_latitude)) },
                    isError = isLatError,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = longitudeInput,
                    onValueChange = { longitudeInput = it },
                    label = { Text(stringResource(R.string.admin_panel_longitude)) },
                    isError = isLonError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (latValue != null && lonValue != null) onConfirm(latValue, lonValue) },
                enabled = !isLatError && !isLonError
            ) { Text(stringResource(R.string.admin_panel_confirm)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReset) { Text(stringResource(R.string.admin_panel_reset_defaults)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_panel_cancel)) }
            }
        }
    )
}

@Composable
fun EditUrlDialog(initialUrl: String, onConfirm: (url: String) -> Unit, onDismiss: () -> Unit) {
    var urlInput by remember { mutableStateOf(initialUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Language, null) },
        title = { Text(stringResource(R.string.admin_panel_webserver_url_change)) },
        text = {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text(stringResource(R.string.admin_panel_webserver_url)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(urlInput) }) {
                Text(stringResource(R.string.admin_panel_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_panel_cancel)) } }
    )
}
