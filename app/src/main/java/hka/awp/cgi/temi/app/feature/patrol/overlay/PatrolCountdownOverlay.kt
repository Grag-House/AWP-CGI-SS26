package hka.awp.cgi.temi.app.feature.patrol.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R

/**
 * An overlay component displaying a countdown timer before an automated patrol begins.
 *
 * @param seconds The current remaining time in seconds to be displayed in the message.
 */
@Composable
fun PatrolCountdownOverlay(seconds: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card {
            Text(
                text = stringResource(R.string.patrol_countdown_message, seconds),
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                ),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
