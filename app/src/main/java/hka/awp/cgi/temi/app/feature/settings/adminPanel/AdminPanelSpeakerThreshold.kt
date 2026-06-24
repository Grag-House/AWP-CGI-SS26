package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R
import java.util.Locale

private const val THRESHOLD_MIN = 0.0
private const val THRESHOLD_MAX = 1.0

@Composable
fun SpeakerVerificationThresholdCard(
    threshold: Double,
    onEdit: () -> Unit,
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Mic,
                contentDescription = "Speaker Verification Threshold",
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue("Voice Match Threshold")
                ConfigSubtext(String.format(Locale.US, "%.2f", threshold))
            }
            Text(
                text = stringResource(R.string.admin_panel_speaker_verification_threshold_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onEdit),
            )
        }
    }
}

@Composable
fun EditSpeakerVerificationThresholdDialog(
    initialThreshold: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var thresholdInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialThreshold)) }

    val thresholdValue = thresholdInput.toDoubleOrNull()
    val isThresholdError =
        thresholdValue == null || thresholdValue < THRESHOLD_MIN || thresholdValue > THRESHOLD_MAX

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = null,
            )
        },
        title = { Text("Voice Match Threshold") },
        text = {
            OutlinedTextField(
                value = thresholdInput,
                onValueChange = { thresholdInput = it.replace(',', '.') },
                label = { Text("Wert zwischen 0.0 und 1.0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = isThresholdError,
                supportingText = if (isThresholdError) {
                    { Text("Erlaubt: 0.0 bis 1.0") }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (thresholdValue != null) {
                        onConfirm(thresholdValue)
                    }
                },
                enabled = !isThresholdError,
            ) {
                Text(stringResource(R.string.admin_panel_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
        },
    )
}

