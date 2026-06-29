package hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigSubtext
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigValue
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.ConfigCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.ConfigIconBox
import java.util.Locale

private const val THRESHOLD_MIN = 0.0
private const val THRESHOLD_MAX = 1.0

/**
 * Displays an input dialog for adjusting the biometric speaker verification confidence threshold.
 *
 * Automatically intercepts comma inputs and sanitizes them into dot format. It validates the parsed double
 * value against [THRESHOLD_MIN] and [THRESHOLD_MAX], disabling submission unless the constraint is satisfied.
 *
 * @param initialThreshold The current threshold parameter to pre-populate within the text field.
 * @param onConfirm Callback fired once a valid decimal boundary score has been confirmed.
 * @param onDismiss Callback triggered when closing or canceling the dialog instance.
 */
@Composable
fun EditSpeakerVerificationThresholdDialog(
    initialThreshold: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var thresholdInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialThreshold)) }
    val thresholdValue = thresholdInput.toDoubleOrNull()
    val isThresholdError = thresholdValue == null || thresholdValue < THRESHOLD_MIN || thresholdValue > THRESHOLD_MAX

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Outlined.Mic, contentDescription = null) },
        title = { Text("Voice Match Threshold") },
        text = {
            OutlinedTextField(
                value = thresholdInput,
                onValueChange = { thresholdInput = it.replace(',', '.') },
                label = { Text("Wert zwischen 0.0 und 1.0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = isThresholdError,
                supportingText = if (isThresholdError) { { Text("Erlaubt: 0.0 bis 1.0") } } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (thresholdValue != null) onConfirm(thresholdValue) },
                enabled = !isThresholdError,
            ) { Text(stringResource(R.string.admin_panel_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_panel_cancel)) }
        },
    )
}

/**
 * Displays an overlay input dialog for specifying a identifier name before training a new voice print profile.
 *
 * Keeps the submission pipeline blocked until a non-empty, trimmed text value is supplied.
 *
 * @param onConfirm Callback supplying the sanitized, trimmed registration name string to initiate enrollment.
 * @param onDismiss Callback triggered when canceling the process profile declaration.
 */
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

/**
 * Displays a critical validation alert dialog confirming the individual removal of a voice print profile.
 *
 * Uses error-themed action markers to emphasize the destructive nature of removing biometric templates.
 *
 * @param profileName The designated unique textual label key pointing to the target voice print element.
 * @param onConfirm Callback fired when explicitly consenting to clear the target entry.
 * @param onDismiss Callback triggered when canceling the target removal step.
 */
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Löschen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

/**
 * Displays a critical validation alert dialog confirming the master reset and wipe of all registered biometric data.
 *
 * Uses error-themed styling to highlight the destructive nature of flushing all profile signatures.
 *
 * @param onConfirm Callback fired when explicitly consenting to clear all existing voice print structures.
 * @param onDismiss Callback triggered when rejecting the data wipe operation.
 */
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Löschen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_panel_cancel)) }
        }
    )
}

/**
 * Renders an embedded sub-list displaying active speaker profiles inline alongside individual deletion triggers.
 *
 * @param profiles A list tracking the active names of registered voice profile records.
 * @param onDeleteClick Intercepts deletion clicks on a specific entry, forwarding the target profile identifier name.
 */
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

/**
 * Renders an navigation entry row card targeting full systemic MQTT telemetry report logs.
 *
 * @param onNavigate Executed when the user interacts with the card container layout to view messaging history.
 */
@Composable
fun MqttReportsCard(onNavigate: () -> Unit) {
    ConfigCard(onClick = onNavigate) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Storage,
                contentDescription = stringResource(R.string.admin_panel_mqtt_reports)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_mqtt_reports))
                ConfigSubtext(stringResource(R.string.admin_panel_mqtt_reports_subtitle))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
