package hka.awp.cgi.temi.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.webserver.ServerState
import hka.awp.cgi.temi.app.ui.components.DashboardCard
import hka.awp.cgi.temi.app.ui.shell.Screen

// TODO add animation delay so the click animation is ran before the navigation

/**
 * The main content view for the Dashboard screen.
 *
 * Displays a welcome message and a grid of interactive [DashboardCard] entries
 *
 * @param modifier Modifier for layout adjustments within the parent container.
 * @param onClick Callback triggered when an item that requires navigation is selected.
 */
@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    onClick: (Screen) -> Unit = {},
    serverState: ServerState,
    currentTemperatureState: Int
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_temi_robot),
                contentDescription = stringResource(R.string.robot_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            val robotName = stringResource(id = R.string.robot_name)
            val welcomeParts = stringResource(R.string.welcome_message, "PLACEHOLDER").split("PLACEHOLDER")
            Text(
                text = buildAnnotatedString {
                    append(welcomeParts[0])
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(robotName)
                    }
                    if (welcomeParts.size > 1) {
                        append(welcomeParts[1])
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        ContentGrid(serverState, onClick, currentTemperatureState)
    }
}

@Composable
private fun ContentGrid(serverState: ServerState, onClick: (Screen) -> Unit, currentTemperatureState: Int) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRIDCELL_COUNT),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardCard(
                title = stringResource(R.string.webserver),
                subtitle = stringResource(R.string.webserverSub),
                icon = Icons.Rounded.Storage,
                bottomText = if (serverState.isReachable) {
                    "${stringResource(R.string.status_online)} (${
                        serverState.ipAddress ?: stringResource(
                            R.string.unknown_host_address
                        )
                    })"
                } else {
                    stringResource(R.string.status_offline)
                },
                onClick = {
                    onClick(Screen.Webserver)
                }
            )
        }
        @Suppress("MagicNumber")
        item {
            DashboardCard(
                title = stringResource(R.string.wetter),
                subtitle = stringResource(R.string.wetterSub),
                customIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.partly_cloudy_day),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(R.string.weather_icon_description)
                    )
                },
                bottomText = stringResource(R.string.temp_unit, currentTemperatureState),
                isTemp = true,
                onClick = { onClick(Screen.Weather) }
            )
        }
        item {
            DashboardCard(
                stringResource(R.string.navigation),
                stringResource(R.string.navigationSub),
                Icons.Rounded.Navigation,
                stringResource(R.string.fastestroute),
                onClick = { onClick(Screen.Navigation) }
            )
        }
        item {
            DashboardCard(
                title = stringResource(R.string.hide_and_seek),
                subtitle = stringResource(R.string.hide_and_seek_sub),
                icon = Icons.Rounded.SportsEsports,
                onClick = { onClick(Screen.HideAndSeek) }
            )
        }
        item {
            DashboardCard(
                stringResource(R.string.settings),
                stringResource(R.string.settingsSub),
                Icons.Rounded.Settings,
                onClick = {
                    onClick(Screen.Settings)
                }
            )
        }
        item {
            DashboardCard(
                title = stringResource(R.string.controller),
                subtitle = stringResource(R.string.controller_subtitle),
                customIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.joystick_24dp_000000_fill0_wght400_grad0_opsz24),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(R.string.controller_description)
                    )
                },
                bottomText = stringResource(R.string.controller_bottom_text),
                onClick = {
                    onClick(Screen.Controller)
                }
            )
        }
    }
}

@Suppress("SpellCheckingInspection")
private const val GRIDCELL_COUNT = 3
