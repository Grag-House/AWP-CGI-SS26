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
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.CoordinateManagementCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.MqttReportsCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.PatrolSettingsCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.RestartAppCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.WebserverPasswordCard
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.WebserverUrlCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader

@Composable
@Suppress("LongParameterList")
fun AdminPanelContent(
    uiState: AdminPanelState,
    onBackClick: () -> Unit,
    onEditUrl: () -> Unit,
    onOpenMqtt: () -> Unit,
    onChangePassword: () -> Unit,
    onEditCoordinates: () -> Unit,
    onRestartRequest: () -> Unit,
    onNavigateToPatrolSettings: () -> Unit
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
                WebserverUrlCard(url = uiState.webserverUrl, onEdit = onEditUrl)
                MqttReportsCard(onNavigate = onOpenMqtt)
                WebserverPasswordCard(onChangePassword = onChangePassword)
                CoordinateManagementCard(coordinates = uiState.coordinates, onEdit = onEditCoordinates)
                PatrolSettingsCard(
                    currentModeText = uiState.patrolModeText,
                    onNavigate = onNavigateToPatrolSettings
                )
                RestartAppCard(onRestartClick = onRestartRequest)
            }
        }
    }
}
