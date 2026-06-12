@file:Suppress("AssignedValueIsNeverRead")

package hka.awp.cgi.temi.app.feature.settings.adminPanel

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.AdminPasswordPrompt
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ChangePasswordDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.DialogPatrolMode
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.EditCoordinatesDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.EditUrlDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.MqttReportsDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.PatrolSettingsDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.RestartAppConfirmationDialog
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

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

    var showCoordinateDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showMqttReportsDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showPatrolSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AdminPanelEvent.OpenMqttReports -> showMqttReportsDialog = true
                AdminPanelEvent.PasswordChanged -> showPasswordDialog = false
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

    if (isAuthorized) {
        AdminPasswordPrompt(
            isError = passwordError,
            onConfirm = { enteredPassword -> viewModel.checkPassword(enteredPassword) },
            onBackClick = onBackClick,
            onValueChange = { viewModel.clearPasswordError() }
        )
        return
    }

    if (showUrlDialog) {
        EditUrlDialog(initialUrl = uiState.webserverUrl, onConfirm = { url ->
            viewModel.onEditWebserverUrl(url)
            showUrlDialog = false
        }, onDismiss = { showUrlDialog = false })
    }
    if (showCoordinateDialog) {
        EditCoordinatesDialog(
            initialLatitude = uiState.latitude,
            initialLongitude = uiState.longitude,
            onConfirm = { lat, lon ->
                viewModel.onEditCoordinates(lat, lon)
                showCoordinateDialog = false
            },
            onReset = {
                viewModel.onResetCoordinates()
                showCoordinateDialog = false
            },
            onDismiss = { showCoordinateDialog = false }
        )
    }
    if (showPasswordDialog) {
        ChangePasswordDialog(onConfirm = { viewModel.onChangePassword(it) }, onDismiss = { showPasswordDialog = false })
    }
    if (showMqttReportsDialog) {
        MqttReportsDialog(
            monitoredTopics = uiState.mqttReportTopics,
            events = uiState.mqttTrafficEvents,
            onClear = viewModel::onClearMqttReports,
            onDismiss = { showMqttReportsDialog = false }
        )
    }
    if (showRestartDialog) {
        RestartAppConfirmationDialog(
            onConfirm = {
                viewModel.onRestartAppRequested()
                showRestartDialog = false
            },
            onDismiss = { showRestartDialog = false }
        )
    }
    if (showPatrolSettingsDialog) {
        PatrolSettingsDialog(
            initialIsEnabled = false,
            initialMode = DialogPatrolMode.RANDOM,
            initialMinMinutes = 40,
            initialMaxMinutes = 60,
            initialHours = emptySet(),
            onDismiss = { showPatrolSettingsDialog = false },
            onSave = { isEnabled, mode, minMin, maxMin, hours ->
                viewModel.onSavePatrolSettings(mode, minMin, maxMin, hours)
                showPatrolSettingsDialog = false
            }
                            )
    }

    AdminPanelContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onEditUrl = { showUrlDialog = true },
        onOpenMqtt = viewModel::onOpenMqttReports,
        onChangePassword = { showPasswordDialog = true },
        onEditCoordinates = { showCoordinateDialog = true },
        onRestartRequest = { showRestartDialog = true },
        onNavigateToPatrolSettings = { showPatrolSettingsDialog = true }
    )
}
