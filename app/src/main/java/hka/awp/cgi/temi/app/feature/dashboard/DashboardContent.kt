package hka.awp.cgi.temi.app.feature.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
