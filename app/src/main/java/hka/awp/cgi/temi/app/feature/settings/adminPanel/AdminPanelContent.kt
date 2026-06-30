package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.AdminPasswordCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.CloseAppCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.CoordinateManagementCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.HidingSpotFilterCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.PatrolRouteCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.PatrolSettingsCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.RestartAppCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.SpeakerVerificationCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.SpeakerVerificationThresholdCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.VoiceProfilesManagementCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.WebserverPasswordCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.WebserverUrlCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards.WebserverVerificationCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.MqttReportsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader

/**
 * Renders the primary container layout scroll-surface assembling the robot's executive administrator cockpit panel.
 *
 * This master template aggregates all specific configuration subgroups (including local and remote security protocols,
 * MQTT broker reports, telemetry coordinate presets, voice matching databases, and autonomous patrol configurations)
 * inside a unified scrollable view. It translates values fed from a declarative [AdminPanelState] snapshot
 * and binds stateless interactions directly back to higher-level orchestrating view models or controllers.
 *
 * @param uiState The active immutable view state state-holder holding required operational telemetry information.
 * @param onBackClick Intercepts upper navigation buttons to move out of the console screen.
 * @param onEditUrl Triggers the input dialog workflow for modifying the remote backend web server path.
 * @param onOpenMqtt Displays the log tracking overlay for active MQTT broker telemetry traffic.
 * @param onUpdateWebserverPassword Displays the security prompt layout for changing remote synchronization access keys.
 * @param onChangeAdminPassword Displays the security prompt layout for modifying the local master access passphrase.
 * @param onEditCoordinates Triggers the decimal validation input modal for latitude/longitude geofence anchoring.
 * @param onRestartRequest Displays a safety confirmation dialog to trigger an application software lifecycle reboot.
 * @param onNavigateToPatrolSettings Dispatches view mutations or navigates to adjust scheduler strategies
 * (Random/Fixed).
 * @param onNavigateToPatrolRoute Dispatches views to adjust target sequence lists mapping custom checkpoint layouts.
 * @param onCloseRequest Displays a safety verification prompt to terminate and close out the active runtime process.
 * @param onOpenHidingSpotFilter Opens the filter settings overlay tracking applicable hiding-spot
 * location restrictions.
 * @param onToggleSpeakerVerification Toggles the global biometric validation block barrier for processing
 * voice commands.
 * @param onEditSpeakerThreshold Displays the precision adjustment input text dialog for matching validation bounds.
 * @param onToggleEnrollment Initiates or prematurely aborts a recording pipeline capture phase to map fresh
 * speaker profiles.
 * @param onDeleteVoiceProfile Erases a single biometric profile entry row matching the forwarded identifier string.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun AdminPanelContent(
    uiState: AdminPanelState,
    onBackClick: () -> Unit,
    onEditUrl: () -> Unit,
    onOpenMqtt: () -> Unit,
    onToggleWebserverAuthentication: (Boolean) -> Unit,
    onUpdateWebserverPassword: () -> Unit,
    onChangeAdminPassword: () -> Unit,
    onEditCoordinates: () -> Unit,
    onRestartRequest: () -> Unit,
    onNavigateToPatrolSettings: () -> Unit,
    onNavigateToPatrolRoute: () -> Unit,
    onCloseRequest: () -> Unit,
    onOpenHidingSpotFilter: () -> Unit,
    onToggleSpeakerVerification: (Boolean) -> Unit,
    onEditSpeakerThreshold: () -> Unit,
    onToggleEnrollment: () -> Unit,
    onDeleteVoiceProfile: (String) -> Unit
) {
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

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MqttReportsCard(
                    onNavigate = onOpenMqtt
                )

                WebserverUrlCard(
                    url = uiState.webserverUrl,
                    onEdit = onEditUrl
                )

                WebserverVerificationCard(
                    enabled = uiState.isSpeakerVerificationEnabled,
                    onToggle = onToggleWebserverAuthentication
                                         )

                WebserverPasswordCard(
                    onUpdateWebserverPassword = onUpdateWebserverPassword
                )

                AdminPasswordCard(
                    onChangePassword = onChangeAdminPassword
                )

                CoordinateManagementCard(
                    coordinates = uiState.coordinates,
                    onEdit = onEditCoordinates
                )

                HidingSpotFilterCard(
                    onEdit = onOpenHidingSpotFilter
                )

                PatrolSettingsCard(
                    currentModeText = if (!uiState.isPatrolEnabled) {
                        stringResource(R.string.admin_panel_patrol_disabled)
                    } else {
                        "${uiState.patrolMode.name}: ${uiState.minMinutes}-${uiState.maxMinutes} min"
                    },
                    onNavigate = onNavigateToPatrolSettings
                )

                PatrolRouteCard(
                    currentRouteText = if (uiState.patrolRoute.isEmpty()) {
                        stringResource(R.string.admin_panel_no_route_selected_title)
                    } else {
                        uiState.patrolRoute.joinToString(" → ")
                    },
                    onNavigate = onNavigateToPatrolRoute
                )

                SpeakerVerificationCard(
                    enabled = uiState.isSpeakerVerificationEnabled,
                    onToggle = onToggleSpeakerVerification
                )

                SpeakerVerificationThresholdCard(
                    threshold = uiState.speakerVerificationThreshold,
                    onEdit = onEditSpeakerThreshold
                )

                VoiceProfilesManagementCard(
                    uiState.voiceProfiles,
                    uiState.isEnrollmentActive,
                    onToggleEnrollment,
                    onDeleteVoiceProfile
                )

                CloseAppCard(
                    onCloseClick = onCloseRequest
                )

                RestartAppCard(
                    onRestartClick = onRestartRequest
                )
            }
        }
    }
}
