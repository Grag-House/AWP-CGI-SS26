package hka.awp.cgi.temi.app.feature.settings.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.robotemi.sdk.navigation.model.SpeedLevel
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SpeedSettingCard // 👈 Der neue Import

@Composable
fun NavigationContent(
    currentGoToSpeed: SpeedLevel,
    currentFollowSpeed: SpeedLevel,
    onGoToSpeedChange: (SpeedLevel) -> Unit,
    onFollowSpeedChange: (SpeedLevel) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        SettingsHeader(
            title = stringResource(R.string.settings_navigation_title),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(40.dp))

        SpeedSettingCard(
            icon = Icons.Rounded.Speed,
            title = stringResource(R.string.settings_navigation_speed_title),
            subtitle = stringResource(R.string.settings_navigation_speed_subtitle),
            currentSpeed = currentGoToSpeed,
            onSpeedChange = onGoToSpeedChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        SpeedSettingCard(
            icon = Icons.Rounded.DirectionsRun,
            title = stringResource(R.string.settings_navigation_followspeed_title),
            subtitle = stringResource(R.string.settings_navigation_followspeed_subtitle),
            currentSpeed = currentFollowSpeed,
            onSpeedChange = onFollowSpeedChange
        )
    }
}
