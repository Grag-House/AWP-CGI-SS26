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
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

/**
 * The core orchestrator view for the Admin Panel feature.
 *
 * This screen intercepts unauthenticated access attempts via [AdminPasswordPrompt]. Upon successful authorization,
 * it initializes lifecycle-aware state-stream collection tracking for core properties, filter states, and system
 * side-effect events (such as hardware process reboots, application shutdowns, and validation warnings).
 * It coordinates local [DialogState] mutations and binds them to the primary layout.
 *
 * @param onBackClick Navigation event hook executed when exiting the administrative context boundary.
 * @param viewModel The state-container and business-logic driver injected via Koin.
 */
@Composable
@Suppress("LongMethod")
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

                AdminPanelEvent.NoRouteSelected -> {
                    dialogs = dialogs.copy(showPatrolSettings = false, showNoRouteDialog = true)
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
        onDismiss = { dialogs = DialogState() },
        onPatrolError = {
            dialogs = dialogs.copy(
                showPatrolSettings = false,
                showNoRouteDialog = true
            )
        }
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
            onToggleWebserverAuthentication = {
                viewModel.onAction(AdminPanelAction.ToggleWebserverVerification(it))
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
