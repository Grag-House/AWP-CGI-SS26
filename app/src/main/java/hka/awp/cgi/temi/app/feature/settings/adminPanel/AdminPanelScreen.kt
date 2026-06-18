@file:Suppress("AssignedValueIsNeverRead")

package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

private const val LAT_MIN = -90.0
private const val LAT_MAX = 90.0
private const val LON_MIN = -180.0
private const val LON_MAX = 180.0

// ─── Cards ────────────────────────────────────────────────────────────────────

/**
 * Displays the webserver URL with an edit action.
 *
 * @param url The webserver URL string to display.
 * @param onEdit Callback invoked when the edit action is tapped.
 */
@Composable
fun WebserverUrlCard(url: String, onEdit: () -> Unit) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Language,
                contentDescription = stringResource(R.string.admin_panel_webserver_url)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigLabel(stringResource(R.string.admin_panel_webserver_url))
                ConfigValue(url)
            }
            Text(
                text = stringResource(R.string.admin_panel_webserver_url_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }
    }
}

/**
 * Displays a navigable card for the MQTT reports section.
 *
 * @param onNavigate Callback invoked when the row is tapped.
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

/**
 * Displays the webserver password row with masked dots and a "Change" action.
 *
 * @param onChangePassword Callback invoked when "Change" is tapped.
 */
@Composable
fun WebserverPasswordCard(
    onChangePassword: () -> Unit
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.admin_panel_webserver_password)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_webserver_password))
                PasswordDots()
            }
            Text(
                text = stringResource(R.string.admin_panel_webserver_password_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onChangePassword)
            )
        }
    }
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
fun SpeakerVerificationCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Mic,
                contentDescription = stringResource(R.string.admin_panel_speaker_verification)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_speaker_verification))
                ConfigSubtext(stringResource(R.string.admin_panel_speaker_verification_subtitle))
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun VoiceProfilesManagementCard(
    voiceProfiles: Map<String, SpeakerVector>,
    isEnrollmentActive: Boolean,
    onLearnClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    ConfigCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConfigIconBox(
                    icon = Icons.Outlined.Mic,
                    contentDescription = "Stimmen-Management"
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ConfigValue("Sprachprofile")
                    ConfigSubtext(
                        if (isEnrollmentActive) {
                            "Enrollment aktiv..."
                        } else if (voiceProfiles.isEmpty()) {
                            "Keine Profile gespeichert"
                        } else {
                            "${voiceProfiles.size} Profile gespeichert"
                        }
                    )
                }
                Text(
                    text = if (isEnrollmentActive) "Stopp" else "Lernen",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onLearnClick)
                )
            }
            if (voiceProfiles.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    voiceProfiles.keys.forEach { profileName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profileName,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { onDeleteClick(profileName) },
                                modifier = Modifier.width(32.dp).height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Profile löschen",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.width(16.dp).height(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileNameInputDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Profil erstellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Geben Sie einen Namen für das Sprachprofil ein:")
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
                onClick = {
                    if (nameInput.trim().isNotEmpty()) {
                        onConfirm(nameInput.trim())
                    }
                },
                enabled = nameInput.trim().isNotEmpty()
            ) {
                Text("Lernen starten")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun DeleteProfileConfirmDialog(
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil löschen") },
        text = {
            Text(
                text = "Möchten Sie das Profil \"$profileName\" wirklich löschen? " +
                    "Diese Aktion kann nicht rückgängig gemacht werden."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Löschen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun ResetVoiceProfilesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stimmen zurücksetzen") },
        text = {
            Text(
                text = "Möchten Sie wirklich alle gespeicherten Sprachprofile löschen? " +
                    "Diese Aktion kann nicht rückgängig gemacht werden."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Löschen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
        }
    )
}

/**
 * Displays coordinate management with edit and navigate actions.
 *
 * @param coordinates The formatted coordinate string to display.
 * @param onEdit Callback invoked when the edit icon is tapped.
 */
@Composable
fun CoordinateManagementCard(
    coordinates: String,
    onEdit: () -> Unit,
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.LocationOn,
                contentDescription = stringResource(R.string.admin_panel_weather_coordinates)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_weather_coordinates))
                ConfigSubtext(coordinates)
            }
            Text(
                text = stringResource(R.string.admin_panel_webserver_coordiantes_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }
    }
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

// ─── Main screen ─────────────────────────────────────────────────────────────

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

@Composable
private fun AdminPanelDialogs(
    uiState: AdminPanelState,
    viewModel: AdminPanelViewModel,
    showCoordinateDialog: Boolean,
    onCoordinateDialogDismiss: () -> Unit,
    showPasswordDialog: Boolean,
    onPasswordDialogDismiss: () -> Unit,
    showUrlDialog: Boolean,
    onUrlDialogDismiss: () -> Unit,
    showMqttReportsDialog: Boolean,
    onMqttReportsDialogDismiss: () -> Unit,
    showResetVoiceProfilesDialog: Boolean,
    onResetVoiceProfilesDialogDismiss: () -> Unit,
    showProfileNameDialog: Boolean,
    onProfileNameDialogDismiss: () -> Unit,
    showDeleteConfirmDialog: Boolean,
    onDeleteConfirmDialogDismiss: () -> Unit,
    selectedProfileToDelete: String
) {
    if (showUrlDialog) {
        EditUrlDialog(initialUrl = uiState.webserverUrl, onConfirm = { url ->
            viewModel.onEditWebserverUrl(url)
            onUrlDialogDismiss()
        }, onDismiss = onUrlDialogDismiss)
    }
    if (showCoordinateDialog) {
        EditCoordinatesDialog(
            initialLatitude = uiState.latitude,
            initialLongitude = uiState.longitude,
            onConfirm = { lat, lon ->
                viewModel.onEditCoordinates(lat, lon)
                onCoordinateDialogDismiss()
            },
            onReset = {
                viewModel.onResetCoordinates()
                onCoordinateDialogDismiss()
            },
            onDismiss = onCoordinateDialogDismiss
        )
    }
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { viewModel.onChangePassword(it) },
            onDismiss = onPasswordDialogDismiss
        )
    }
    if (showMqttReportsDialog) {
        MqttReportsDialog(
            monitoredTopics = uiState.mqttReportTopics,
            events = uiState.mqttTrafficEvents,
            onClear = viewModel::onClearMqttReports,
            onDismiss = onMqttReportsDialogDismiss
        )
    }
    if (showResetVoiceProfilesDialog) {
        ResetVoiceProfilesDialog(
            onConfirm = viewModel::onResetVoiceProfiles,
            onDismiss = onResetVoiceProfilesDialogDismiss
        )
    }
    if (showProfileNameDialog) {
        ProfileNameInputDialog(
            onConfirm = { name ->
                viewModel.onToggleEnrollment(true, name)
                onProfileNameDialogDismiss()
            },
            onDismiss = onProfileNameDialogDismiss
        )
    }
    if (showDeleteConfirmDialog) {
        DeleteProfileConfirmDialog(
            profileName = selectedProfileToDelete,
            onConfirm = {
                viewModel.onDeleteVoiceProfile(selectedProfileToDelete)
            },
            onDismiss = onDeleteConfirmDialogDismiss
        )
    }
}

@Composable
fun AdminPanelScreen(
    onBackClick: () -> Unit,
    viewModel: AdminPanelViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCoordinateDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showMqttReportsDialog by remember { mutableStateOf(false) }
    var showResetVoiceProfilesDialog by remember { mutableStateOf(false) }
    var showProfileNameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedProfileToDelete by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AdminPanelEvent.OpenMqttReports -> {
                    showMqttReportsDialog = true
                }

                AdminPanelEvent.PasswordChanged -> {
                    showPasswordDialog = false
                }
            }
        }
    }

    AdminPanelDialogs(
        uiState = uiState,
        viewModel = viewModel,
        showCoordinateDialog = showCoordinateDialog,
        onCoordinateDialogDismiss = { showCoordinateDialog = false },
        showPasswordDialog = showPasswordDialog,
        onPasswordDialogDismiss = { showPasswordDialog = false },
        showUrlDialog = showUrlDialog,
        onUrlDialogDismiss = { showUrlDialog = false },
        showMqttReportsDialog = showMqttReportsDialog,
        onMqttReportsDialogDismiss = { showMqttReportsDialog = false },
        showResetVoiceProfilesDialog = showResetVoiceProfilesDialog,
        onResetVoiceProfilesDialogDismiss = { showResetVoiceProfilesDialog = false },
        showProfileNameDialog = showProfileNameDialog,
        onProfileNameDialogDismiss = { showProfileNameDialog = false },
        showDeleteConfirmDialog = showDeleteConfirmDialog,
        onDeleteConfirmDialogDismiss = { showDeleteConfirmDialog = false },
        selectedProfileToDelete = selectedProfileToDelete
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            SettingsHeader(
                title = stringResource(R.string.admin_panel_header),
                onBackClick = onBackClick
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WebserverUrlCard(
                    url = uiState.webserverUrl,
                    onEdit = { showUrlDialog = true }
                )

                MqttReportsCard(
                    onNavigate = viewModel::onOpenMqttReports
                )

                WebserverPasswordCard(
                    onChangePassword = { showPasswordDialog = true }
                )

                CoordinateManagementCard(
                    coordinates = uiState.coordinates,
                    onEdit = { showCoordinateDialog = true }
                )

                SpeakerVerificationCard(
                    enabled = uiState.isSpeakerVerificationEnabled,
                    onToggle = { viewModel.onToggleSpeakerVerification(it) }
                )

                VoiceProfilesManagementCard(
                    voiceProfiles = uiState.voiceProfiles,
                    isEnrollmentActive = uiState.isEnrollmentActive,
                    onLearnClick = { showProfileNameDialog = true },
                    onDeleteClick = { profileName ->
                        selectedProfileToDelete = profileName
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
    }
}
