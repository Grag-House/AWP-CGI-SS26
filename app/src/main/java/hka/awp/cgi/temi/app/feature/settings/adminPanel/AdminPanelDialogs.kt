package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.compose.runtime.Composable
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.AdminPanelPatrolSettingsDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.AdminPanelRouteDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.ChangeAdminPasswordDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.ChangeWebserverPasswordDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.CloseAppConfirmationDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.DeleteProfileConfirmDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.EditCoordinatesDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.EditSpeakerVerificationThresholdDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.EditUrlDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.MqttReportsDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.NoRouteSelectedDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.ProfileNameInputDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.ResetVoiceProfilesDialog
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.RestartAppConfirmationDialog

/**
 * Internal conditional switch router that resolves and draws overlay alert dialogs based on the flags
 * mapped within [DialogState].
 *
 * @param uiState The active master telemetry snapshot used to initialize baseline configuration structures inside child
 * fields.
 * @param onAction Dispatches system intent interactions back to the feature's architectural state engine.
 * @param dialogState The layout model snapshot tracking which specific popovers should be displayed.
 * @param onPatrolError Callback fired if a patrol execution sequence fails validation checks due to an empty
 * configuration.
 * @param onDismiss Universal cleanup trigger used to clear the active dialog layout state visibility fields.
 */
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun AdminPanelDialogs(
    uiState: AdminPanelState,
    onAction: (AdminPanelAction) -> Unit,
    dialogState: DialogState,
    onPatrolError: () -> Unit,
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
            { password, user ->
                onAction(AdminPanelAction.ChangeWebserverPassword(password, user))
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

    if (dialogState.showNoRouteDialog) {
        NoRouteSelectedDialog(onDismiss)
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
        AdminPanelPatrolSettingsDialog(
            initialIsEnabled = uiState.isPatrolEnabled,
            initialMode = uiState.patrolMode,
            initialMinMinutes = uiState.minMinutes,
            initialMaxMinutes = uiState.maxMinutes,
            initialHours = uiState.selectedHours,
            onTriggerPatrol = {
                onAction(AdminPanelAction.TriggerImmediatePatrol)
                if (uiState.patrolRoute.isEmpty()) {
                    onPatrolError()
                }
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
        AdminPanelRouteDialog(
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

/**
 * An internal data class holding the visibility flag states for various alert and configuration sub-dialogs
 * rendered inside the administration dashboard context.
 *
 * @property showUrl Flag to control the visibility of the web server URL editing dialog.
 * @property showCoordinate Flag to control the visibility of the coordinate preset configuration dialog.
 * @property showThreshold Flag to control the visibility of the speaker verification threshold editing dialog.
 * @property showAdminPassword Flag to control the visibility of the local master admin password change dialog.
 * @property showWebserverPassword Flag to control the visibility of the web server password change dialog.
 * @property showMqttReports Flag to control the visibility of the MQTT report traffic tracking dialog.
 * @property showResetVoiceProfiles Flag to control the visibility of the speaker profiles factory reset confirmation
 * dialog.
 * @property showProfileName Flag to control the visibility of the voice identifier profile input dialog.
 * @property showDeleteConfirm Flag to control the visibility of a profile deletion confirmation warning popup.
 * @property showRestart Flag to control the visibility of the system application lifecycle restart alert.
 * @property showClose Flag to control the visibility of the application process exit validation dialog.
 * @property selectedProfileToDelete Holds the text reference key of the biometric voice profile targeted for deletion.
 * @property showPatrolSettings Flag to control the visibility of the autonomous scheduler patrol behavior menu.
 * @property showPatrolRoute Flag to control the visibility of the checkpoint waypoint route mapping overlay.
 * @property showNoRouteDialog Flag to control the visibility of the error dialog displayed when launching an
 * unconfigured route.
 */
@Suppress("LongMethod")
data class DialogState(
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
    val showNoRouteDialog: Boolean = false,
)
