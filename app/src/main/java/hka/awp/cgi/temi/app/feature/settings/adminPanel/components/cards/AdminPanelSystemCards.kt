package hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlindsClosed
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigLabel
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigSubtext
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigValue
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.PasswordDots

/**
 * Renders a system utility configuration card for restarting the application layer.
 *
 * Clicking this row triggers an immediate, full software lifecycle restart routine.
 *
 * @param onRestartClick Executed when the user interacts with the card to reboot the app.
 */
@Composable
fun RestartAppCard(onRestartClick: () -> Unit) {
    ConfigCard(onClick = onRestartClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.admin_panel_restart_app)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_restart_app))
                ConfigSubtext(stringResource(R.string.admin_panel_restart_app_subtitle))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Renders a system utility configuration card for terminating and closing the application.
 *
 * Clicking this row triggers a termination routine that exits the application layer entirely.
 *
 * @param onCloseClick Executed when the user interacts with the card to exit the application instance.
 */
@Composable
fun CloseAppCard(onCloseClick: () -> Unit) {
    ConfigCard(onClick = onCloseClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Default.BlindsClosed,
                contentDescription = stringResource(R.string.admin_panel_close_app)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_close_app))
                ConfigSubtext(stringResource(R.string.admin_panel_close_app_subtitle))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Renders an administration management card for inspecting and modifying the remote web server URL.
 *
 * Displays the current web endpoint target string and offers an inline text action to update the address path.
 *
 * @param url The currently configured remote web server endpoint network path.
 * @param onEdit Executed when the user interacts with the modification text button to update the link.
 */
@Composable
fun WebserverUrlCard(url: String, onEdit: () -> Unit) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Language,
                contentDescription = stringResource(R.string.admin_panel_webserver_url)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigLabel(stringResource(R.string.admin_panel_webserver_url))
                ConfigValue(url)
            }
            Text(
                text = stringResource(R.string.admin_panel_webserver_url_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }
    }
}

/**
 * Renders a restricted security card for changing the API authentication password used for web server interactions.
 *
 * Hides actual security details using masked dot layout elements and attaches a button action
 * to initiate an overlay modification.
 *
 * @param onUpdateWebserverPassword Executed when the text link is clicked to modify web server access keys.
 */
@Composable
fun WebserverPasswordCard(
    onUpdateWebserverPassword: () -> Unit
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.admin_panel_webserver_password)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_webserver_password))
                PasswordDots()
            }
            Text(
                text = stringResource(R.string.admin_panel_webserver_password_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onUpdateWebserverPassword)
            )
        }
    }
}

/**
 * Renders a restricted security card for changing the application's local administrator console master access key.
 *
 * Shields the current state using a masked password pattern and triggers an updates workflow onClick.
 *
 * @param onChangePassword Executed when the user requests a modification to the master panel security credentials.
 */
@Composable
fun AdminPasswordCard(
    onChangePassword: () -> Unit
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.admin_panel_admin_password)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_admin_password))
                PasswordDots()
            }
            Text(
                text = stringResource(R.string.admin_panel_admin_password_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onChangePassword)
            )
        }
    }
}
