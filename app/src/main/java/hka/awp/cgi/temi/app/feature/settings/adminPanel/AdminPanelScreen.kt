@file:Suppress("AssignedValueIsNeverRead")

package hka.awp.cgi.temi.app.feature.settings.adminPanel

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotFilterCallbacks
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotFilterContent
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.AdminPasswordPrompt
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ChangePasswordDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.CoordinateManagementCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.DeleteProfileConfirmDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.EditCoordinatesDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.EditSpeakerVerificationThresholdDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.EditUrlDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.HidingSpotFilterCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.MqttReportsCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.MqttReportsDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ProfileNameInputDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.RestartAppCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.RestartAppConfirmationDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ResetVoiceProfilesDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.SpeakerVerificationCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.SpeakerVerificationThresholdCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.VoiceProfilesManagementCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.WebserverPasswordCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.WebserverUrlCard
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
    val showRestart: Boolean = false,
    val selectedProfileToDelete: String = ""
)

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
    if (dialogState.showRestart) {
        RestartAppConfirmationDialog({
            onAction(AdminPanelAction.RequestRestart)
            onDismiss()
        }, onDismiss)
    }
}

@Composable
fun AdminPanelScreen(
    onBackClick: () -> Unit,
    viewModel: AdminPanelViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle()
    val passwordError by viewModel.passwordError.collectAsStateWithLifecycle()
    val filterState by viewModel.filterManager.filterState.collectAsStateWithLifecycle()
    val showHidingSpotFilter by viewModel.filterManager.isOpen.collectAsStateWithLifecycle()

    var dialogs by remember { mutableStateOf(DialogState()) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AdminPanelEvent.OpenMqttReports -> dialogs = dialogs.copy(showMqttReports = true)
                AdminPanelEvent.PasswordChanged -> dialogs = dialogs.copy(showPassword = false)
                AdminPanelEvent.RestartAppTriggered -> {
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    val mainIntent = Intent.makeRestartActivityTask(intent?.component)
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetAuthorization()
        }
    }

    if (!isAuthorized) {
        AdminPasswordPrompt(
            isError = passwordError,
            onConfirm = { enteredPassword -> viewModel.checkPassword(enteredPassword) },
            onBackClick = onBackClick,
            onValueChange = { viewModel.clearPasswordError() }
        )
        return
    }

    AdminPanelDialogs(uiState, viewModel::onAction, dialogs) { dialogs = DialogState() }

    if (showHidingSpotFilter) {
        Dialog(onDismissRequest = viewModel.filterManager::dismiss) {
            HidingSpotFilterContent(
                state = filterState,
                callbacks = HidingSpotFilterCallbacks(
                    onToggle = viewModel.filterManager::toggle,
                    onSelectAll = viewModel.filterManager::selectAll,
                    onDeselectAll = viewModel.filterManager::deselectAll,
                    onSave = viewModel.filterManager::save,
                    onDismiss = viewModel.filterManager::dismiss
                )
            )
        }
    }

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
            SettingsHeader(stringResource(R.string.admin_panel_header), onBackClick)
            Spacer(Modifier.height(40.dp))
            AdminPanelContent(uiState, viewModel) { dialogs = it }
        }
    }
}

@Composable
private fun AdminPanelContent(
    uiState: AdminPanelState,
    viewModel: AdminPanelViewModel,
    updateDialogs: (DialogState) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WebserverUrlCard(uiState.webserverUrl) { updateDialogs(DialogState(showUrl = true)) }
        MqttReportsCard { viewModel.onAction(AdminPanelAction.OpenMqttReports) }
        WebserverPasswordCard { updateDialogs(DialogState(showPassword = true)) }
        CoordinateManagementCard(uiState.coordinates) { updateDialogs(DialogState(showCoordinate = true)) }
        HidingSpotFilterCard { viewModel.filterManager.open() }
        RestartAppCard { updateDialogs(DialogState(showRestart = true)) }
        SpeakerVerificationCard(uiState.isSpeakerVerificationEnabled) {
            viewModel.onAction(AdminPanelAction.ToggleSpeakerVerification(it))
        }
        SpeakerVerificationThresholdCard(uiState.speakerVerificationThreshold) {
            updateDialogs(DialogState(showThreshold = true))
        }
        VoiceProfilesManagementCard(
            uiState.voiceProfiles,
            uiState.isEnrollmentActive,
            {
                if (uiState.isEnrollmentActive) {
                    viewModel.onAction(AdminPanelAction.ToggleEnrollment(false))
                } else {
                    updateDialogs(DialogState(showProfileName = true))
                }
            },
            { updateDialogs(DialogState(showDeleteConfirm = true, selectedProfileToDelete = it)) }
        )
    }
}
