package hka.awp.cgi.temi.app.feature.settings.adminPanel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R

private const val LAT_MIN = -90.0
private const val LAT_MAX = 90.0
private const val LON_MIN = -180.0
private const val LON_MAX = 180.0

@Composable
fun RestartAppConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = stringResource(R.string.admin_panel_restart_confirm_title))
        },
        text = {
            Text(
                text = stringResource(R.string.admin_panel_restart_confirm_text),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.admin_panel_confirm_restart))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
        }
    )
}

@Suppress("LongMethod")
@Composable
fun ChangePasswordDialog(
    onConfirm: (newPassword: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null
            )
        },
        title = {
            Text(text = stringResource(R.string.admin_panel_change_password_title))
        },
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
            Button(
                onClick = { onConfirm(newPassword) },
                enabled = newPassword.isNotBlank()
            ) {
                Text(stringResource(R.string.admin_panel_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
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

    AlertDialog(onDismissRequest = onDismiss, icon = {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null
        )
    }, title = {
        Text(text = stringResource(R.string.admin_panel_webserver_coordiantes_change))
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = latitudeInput,
                onValueChange = { latitudeInput = it },
                label = { Text(stringResource(R.string.admin_panel_latitude)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = isLatError,
                supportingText = if (isLatError) {
                    { Text(stringResource(R.string.admin_panel_latitude_error)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = longitudeInput,
                onValueChange = { longitudeInput = it },
                label = { Text(stringResource(R.string.admin_panel_longitude)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = isLonError,
                supportingText = if (isLonError) {
                    { Text(stringResource(R.string.admin_panel_longitude_error)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }, confirmButton = {
        Button(
            onClick = {
                if (latValue != null && lonValue != null) {
                    onConfirm(latValue, lonValue)
                }
            },
            enabled = !isLatError && !isLonError
        ) {
            Text(stringResource(R.string.admin_panel_confirm))
        }
    }, dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onReset) {
                Text(stringResource(R.string.admin_panel_reset_defaults))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
        }
    })
}

@Composable
fun EditUrlDialog(
    initialUrl: String,
    onConfirm: (url: String) -> Unit,
    onDismiss: () -> Unit
) {
    var urlInput by remember { mutableStateOf(initialUrl) }

    AlertDialog(onDismissRequest = onDismiss, icon = {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null
        )
    }, title = {
        Text(text = stringResource(R.string.admin_panel_webserver_url_change))
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text(stringResource(R.string.admin_panel_webserver_url)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }, confirmButton = {
        Button(
            onClick = {
                onConfirm(urlInput)
            }
        ) {
            Text(stringResource(R.string.admin_panel_confirm))
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.admin_panel_cancel))
        }
    })
}
