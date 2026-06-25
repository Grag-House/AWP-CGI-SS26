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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

// ─── Dialog State ────────────────────────────────────────────────────────────

private data class DialogState(
    val showUrl: Boolean = false,
    val showCoordinate: Boolean = false,
    val showThreshold: Boolean = false,
    val showPassword: Boolean = false,
    val showMqttReports: Boolean = false,
    val showResetVoiceProfiles: Boolean = false,
    val showProfileName: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val selectedProfileToDelete: String = ""
)

// ─── Cards ────────────────────────────────────────────────────────────────────

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

@Composable
fun WebserverPasswordCard(onChangePassword: () -> Unit) {
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

@Composable
fun SpeakerVerificationCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
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
            Switch(checked = enabled, onCheckedChange = onToggle)
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
                ConfigIconBox(Icons.Outlined.Mic, "Stimmen-Management")
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ConfigValue("Sprachprofile")
                    ConfigSubtext(
                        when {
                            isEnrollmentActive -> "Enrollment aktiv..."
                            voiceProfiles.isEmpty() -> "Keine Profile gespeichert"
                            else -> "${voiceProfiles.size} Profile gespeichert"
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
                VoiceProfileList(voiceProfiles.keys.toList(), onDeleteClick)
            }
        }
    }
}

@Composable
fun CoordinateManagementCard(coordinates: String, onEdit: () -> Unit) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(Icons.Outlined.LocationOn, stringResource(R.string.admin_panel_weather_coordinates))
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
private fun AdminPanelDialogs(
    uiState: AdminPanelState,
    onAction: (AdminPanelAction) -> Unit,
    dialogState: DialogState,
    onDismiss: () -> Unit
) {
    if (dialogState.showUrl) {
        EditUrlDialog(uiState.webserverUrl, {
            onAction(AdminPanelAction.EditWebserverUrl(it))
            onDismiss()
        }, onDismiss)
    }
    if (dialogState.showCoordinate) {
        EditCoordinatesDialog(
            uiState.latitude,
            uiState.longitude,
            { lat, lon ->
                onAction(AdminPanelAction.EditCoordinates(lat, lon))
                onDismiss()
            },
            {
                onAction(AdminPanelAction.ResetCoordinates)
                onDismiss()
            },
            onDismiss
        )
    }
    if (dialogState.showThreshold) {
        EditSpeakerVerificationThresholdDialog(
            uiState.speakerVerificationThreshold,
            {
                onAction(AdminPanelAction.EditSpeakerVerificationThreshold(it))
                onDismiss()
            },
            onDismiss
        )
    }
    if (dialogState.showPassword) {
        ChangePasswordDialog({ onAction(AdminPanelAction.ChangePassword(it)) }, onDismiss)
    }
    if (dialogState.showMqttReports) {
        MqttReportsDialog(
            uiState.mqttReportTopics,
            uiState.mqttTrafficEvents,
            { onAction(AdminPanelAction.ClearMqttReports) },
            onDismiss
        )
    }
    if (dialogState.showResetVoiceProfiles) {
        ResetVoiceProfilesDialog({ onAction(AdminPanelAction.ResetVoiceProfiles) }, onDismiss)
    }
    if (dialogState.showProfileName) {
        ProfileNameInputDialog({
            onAction(AdminPanelAction.ToggleEnrollment(true, it))
            onDismiss()
        }, onDismiss)
    }
    if (dialogState.showDeleteConfirm) {
        val name = dialogState.selectedProfileToDelete
        DeleteProfileConfirmDialog(name, { onAction(AdminPanelAction.DeleteVoiceProfile(name)) }, onDismiss)
    }
}

@Composable
fun AdminPanelScreen(
    onBackClick: () -> Unit,
    viewModel: AdminPanelViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dialogs by remember { mutableStateOf(DialogState()) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            dialogs = when (event) {
                AdminPanelEvent.OpenMqttReports -> dialogs.copy(showMqttReports = true)
                AdminPanelEvent.PasswordChanged -> dialogs.copy(showPassword = false)
            }
        }
    }

    AdminPanelDialogs(uiState, viewModel::onAction, dialogs) { dialogs = DialogState() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp)) {
        SettingsHeader(stringResource(R.string.admin_panel_header), onBackClick)
        Spacer(Modifier.height(40.dp))
        AdminPanelContent(uiState, viewModel::onAction) { dialogs = it }
    }
}

@Composable
private fun AdminPanelContent(
    uiState: AdminPanelState,
    onAction: (AdminPanelAction) -> Unit,
    updateDialogs: (DialogState) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WebserverUrlCard(uiState.webserverUrl) { updateDialogs(DialogState(showUrl = true)) }
        MqttReportsCard { onAction(AdminPanelAction.OpenMqttReports) }
        WebserverPasswordCard { updateDialogs(DialogState(showPassword = true)) }
        CoordinateManagementCard(uiState.coordinates) { updateDialogs(DialogState(showCoordinate = true)) }
        SpeakerVerificationCard(uiState.isSpeakerVerificationEnabled) {
            onAction(AdminPanelAction.ToggleSpeakerVerification(it))
        }
        SpeakerVerificationThresholdCard(uiState.speakerVerificationThreshold) {
            updateDialogs(DialogState(showThreshold = true))
        }
        VoiceProfilesManagementCard(
            uiState.voiceProfiles,
            uiState.isEnrollmentActive,
            {
                if (uiState.isEnrollmentActive) {
                    onAction(
                        AdminPanelAction.ToggleEnrollment(false)
                    )
                } else {
                    updateDialogs(DialogState(showProfileName = true))
                }
            },
            { updateDialogs(DialogState(showDeleteConfirm = true, selectedProfileToDelete = it)) }
        )
    }
}
