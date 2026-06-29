package hka.awp.cgi.temi.app.feature.settings.adminPanel

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotFilterCallbacks
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotFilterContent
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.AdminPasswordPrompt
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ChangeAdminPasswordDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ChangeWebserverPasswordDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.CloseAppConfirmationDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.DeleteProfileConfirmDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.EditCoordinatesDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.EditSpeakerVerificationThresholdDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.EditUrlDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.MqttReportsDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ProfileNameInputDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ResetVoiceProfilesDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.RestartAppConfirmationDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolRouteDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolSettingsDialog
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

private data class DialogState(
    val showUrl: Boolean = false,
    val showCoordinate: Boolean = false,
    val showThreshold: Boolean = false,
    val showAdminPassword: Boolean = false,
    val showWebserverPassword: Boolean = false,
    val showMqttReports: Boolean = false,
    val showResetVoiceProfiles: Boolean = false,
    val showProfileName: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showRestart: Boolean = false,
    val showClose: Boolean = false,
    val selectedProfileToDelete: String = "",
    val showPatrolSettings: Boolean = false,
    val showPatrolRoute: Boolean = false,
                              )

@Composable
private fun AdminPanelDialogs(
    uiState: AdminPanelState,
    onAction: (AdminPanelAction) -> Unit,
    dialogState: DialogState,
    onDismiss: () -> Unit
                             ) {
    if (dialogState.showUrl) {
        EditUrlDialog(
            uiState.webserverUrl,
            {
                onAction(AdminPanelAction.EditWebserverUrl(it))
                onDismiss()
            },
            onDismiss
                     )
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

    if (dialogState.showAdminPassword) {
        ChangeAdminPasswordDialog(
            {
                onAction(AdminPanelAction.ChangeAdminPassword(it))
                onDismiss()
            },
            onDismiss
                                 )
    }

    if (dialogState.showWebserverPassword) {
        ChangeWebserverPasswordDialog(
            {
                onAction(AdminPanelAction.ChangeWebserverPassword(it))
                onDismiss()
            },
            onDismiss
                                     )
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
        ResetVoiceProfilesDialog(
            {
                onAction(AdminPanelAction.ResetVoiceProfiles)
                onDismiss()
            },
            onDismiss
                                )
    }

    if (dialogState.showProfileName) {
        ProfileNameInputDialog(
            {
                onAction(AdminPanelAction.ToggleEnrollment(true, it))
                onDismiss()
            },
            onDismiss
                              )
    }

    if (dialogState.showDeleteConfirm) {
        val name = dialogState.selectedProfileToDelete
        DeleteProfileConfirmDialog(
            name,
            {
                onAction(AdminPanelAction.DeleteVoiceProfile(name))
                onDismiss()
            },
            onDismiss
                                  )
    }

    if (dialogState.showRestart) {
        RestartAppConfirmationDialog(
            {
                onAction(AdminPanelAction.RequestRestart)
                onDismiss()
            },
            onDismiss
                                    )
    }

    if (dialogState.showClose) {
        CloseAppConfirmationDialog(
            {
                onAction(AdminPanelAction.RequestCloseApp)
                onDismiss()
            },
            onDismiss
                                  )
    }
    if (dialogState.showPatrolSettings) {
        PatrolSettingsDialog(
            initialIsEnabled = uiState.isPatrolEnabled,
            initialMode = uiState.patrolMode,
            initialMinMinutes = uiState.minMinutes,
            initialMaxMinutes = uiState.maxMinutes,
            initialHours = uiState.selectedHours,
            onTriggerPatrol = {
                onAction(AdminPanelAction.TriggerImmediatePatrol)
                onDismiss()
            },
            onDismiss = onDismiss,
            onSave = { enabled, mode, min, max, hours ->
                onAction(
                    AdminPanelAction.SavePatrolSettings(
                        isEnabled = enabled,
                        mode = mode,
                        minMinutes = min,
                        maxMinutes = max,
                        hours = hours
                                                       )
                        )
                onDismiss()
            }
                            )
    }

    if (dialogState.showPatrolRoute) {
        PatrolRouteDialog(
                savedLocations = uiState.savedLocations,
                initialRoute = uiState.patrolRoute,
                onDismiss = onDismiss,
                onSave = {
                    onAction(AdminPanelAction.SavePatrolRoute(it))
                    onDismiss()
                }
        )
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
                AdminPanelEvent.OpenMqttReports -> {
                    dialogs = dialogs.copy(showMqttReports = true)
                }

                AdminPanelEvent.PasswordChanged,
                AdminPanelEvent.WebserverPasswordChanged -> {
                    dialogs = dialogs.copy(
                        showAdminPassword = false,
                        showWebserverPassword = false
                                          )
                }

                AdminPanelEvent.RestartAppTriggered -> {
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    val mainIntent = Intent.makeRestartActivityTask(intent?.component)
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                }

                AdminPanelEvent.CloseAppTriggered -> {
                    (context as? Activity)?.finishAffinity()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onAction(AdminPanelAction.ResetAuthorization)
        }
    }

    if (!isAuthorized) {
        AdminPasswordPrompt(
            isError = passwordError,
            onConfirm = { enteredPassword ->
                viewModel.onAction(AdminPanelAction.CheckAdminPassword(enteredPassword))
            },
            onBackClick = onBackClick,
            onValueChange = {
                viewModel.onAction(AdminPanelAction.ClearPasswordError)
            }
                           )
        return
    }

    AdminPanelDialogs(
        uiState = uiState,
        onAction = viewModel::onAction,
        dialogState = dialogs,
        onDismiss = { dialogs = DialogState() }
                     )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
              ) {
            AdminPanelContent(
                uiState = uiState,
                onBackClick = onBackClick,
                onEditUrl = {
                    dialogs = DialogState(showUrl = true)
                },
                onOpenMqtt = {
                    viewModel.onAction(AdminPanelAction.OpenMqttReports)
                },
                onUpdateWebserverPassword = {
                    dialogs = DialogState(showWebserverPassword = true)
                },
                onChangeAdminPassword = {
                    dialogs = DialogState(showAdminPassword = true)
                },
                onEditCoordinates = {
                    dialogs = DialogState(showCoordinate = true)
                },
                onRestartRequest = {
                    dialogs = DialogState(showRestart = true)
                },
                onNavigateToPatrolSettings = {
                    viewModel.loadPatrolLocations()
                    dialogs = DialogState(showPatrolSettings = true)
                },
                onNavigateToPatrolRoute = {
                    viewModel.loadPatrolLocations()
                    dialogs = DialogState(showPatrolRoute = true)
                },
                onCloseRequest = {
                    dialogs = DialogState(showClose = true)
                },
                onOpenHidingSpotFilter = {
                    viewModel.filterManager.open()
                },
                onToggleSpeakerVerification = {
                    viewModel.onAction(AdminPanelAction.ToggleSpeakerVerification(it))
                },
                onEditSpeakerThreshold = {
                    dialogs = DialogState(showThreshold = true)
                },
                onToggleEnrollment = {
                    if (uiState.isEnrollmentActive) {
                        viewModel.onAction(AdminPanelAction.ToggleEnrollment(false))
                    } else {
                        dialogs = DialogState(showProfileName = true)
                    }
                },
                onDeleteVoiceProfile = {
                    dialogs = DialogState(
                        showDeleteConfirm = true,
                        selectedProfileToDelete = it
                                         )
                }
            )
        }
    }

