package hka.awp.temi_cgi_app.feature.dashboard

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
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import hka.awp.temi_cgi_app.R
import hka.awp.temi_cgi_app.ui.components.DashboardCard
import hka.awp.temi_cgi_app.ui.components.ModeIcon
import hka.awp.temi_cgi_app.ui.shell.Screen

/**
 * The main content view for the Dashboard screen.
 *
 * Displays a welcome message and a grid of interactive [DashboardCard] entries
 *
 * @param modifier Modifier for layout adjustments within the parent container.
 * @param selectedRoute The current navigation route
 * @param onClick Callback triggered when an item that requires navigation is selected.
 */
@Composable
fun MainContent(
    modifier: Modifier = Modifier, selectedRoute: String, onClick: (Screen) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.SmartToy,
                contentDescription = "Roboter",
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
                },  style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardCard(
                    R.string.webserver,
                    R.string.webserverSub,
                    Icons.Rounded.Storage,
                    "Active 10.0.0.1",
                    onClick = {

                    })
            }
            item {
                DashboardCard(
                    title = R.string.wetter,
                    subtitle = R.string.wetterSub,
                    customIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.partly_cloudy_day),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Sun and cloud symbol for local weather"
                        )
                    },
                    bottomText = "21°C",
                    isTemp = true
                )
            }
            item {
                DashboardCard(
                    R.string.navigation,
                    R.string.navigationSub,
                    Icons.Rounded.Navigation,
                    "FASTEST ROUTE"
                )
            }
            item {
                DashboardCard(
                    R.string.modus,
                    R.string.modusSub,
                    null,
                    "SHOWROOM MODE",
                    overline = "Aktueller Modus",
                    customIcon = { ModeIcon(tint = MaterialTheme.colorScheme.primary) })
            }
            item {
                DashboardCard(
                    R.string.settings,
                    R.string.settingsSub,
                    Icons.Rounded.Settings
                )
            }
        }
    }
}