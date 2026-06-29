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
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.MqttReportsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader

@Composable
@Suppress("LongParameterList", "LongMethod")
fun AdminPanelContent(
    uiState: AdminPanelState,
    onBackClick: () -> Unit,
    onEditUrl: () -> Unit,
    onOpenMqtt: () -> Unit,
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
