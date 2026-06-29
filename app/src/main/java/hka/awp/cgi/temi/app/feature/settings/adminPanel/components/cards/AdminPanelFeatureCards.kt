package hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ShieldMoon
import androidx.compose.material.icons.rounded.SportsEsports
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
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigSubtext
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigValue

/**
 * Renders a configuration card for managing game-related hiding spot filters.
 *
 * Clicking this card triggers a modification workflow to filter out specific locations or locations
 * used in interactive robot games.
 *
 * @param onEdit Executed when the user interacts with the card to edit hiding spots.
 */
@Composable
fun HidingSpotFilterCard(onEdit: () -> Unit) {
    ConfigCard(onClick = onEdit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Rounded.SportsEsports,
                contentDescription = stringResource(R.string.admin_panel_hiding_spots)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_hiding_spots))
                ConfigSubtext(stringResource(R.string.admin_panel_hiding_spots_subtitle))
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
 * Renders a configuration card for adjusting geographic weather coordinates.
 *
 * Displays the currently active location or coordinate string and offers a text-based
 * action button to update the location settings used for external API requests.
 *
 * @param coordinates The currently configured coordinate metadata text string.
 * @param onEdit Executed when the user clicks the dedicated text button to change coordinates.
 */
@Composable
fun CoordinateManagementCard(
    coordinates: String,
    onEdit: () -> Unit,
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.LocationOn,
                contentDescription = stringResource(R.string.admin_panel_weather_coordinates)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_weather_coordinates))
                ConfigSubtext(coordinates)
            }
            Text(
                text = stringResource(R.string.admin_panel_webserver_coordiantes_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }
    }
}

/**
 * Renders a configuration card for navigating to general patrol behavior settings.
 *
 * Displays the status of the robot's active execution mode (e.g., automated nighttime security/patrol loops).
 *
 * @param currentModeText The text representation describing the currently active operational mode.
 * @param onNavigate Executed when the user clicks the card to modify general patrol parameters.
 */
@Composable
fun PatrolSettingsCard(
    currentModeText: String,
    onNavigate: () -> Unit
) {
    ConfigCard(onClick = onNavigate) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.ShieldMoon,
                contentDescription = stringResource(R.string.admin_panel_patrol_settings)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_patrol_settings))
                ConfigSubtext(stringResource(R.string.admin_panel_active_prefix, currentModeText))
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
 * Renders a configuration card for inspecting or modifying assigned navigation pathways.
 *
 * Details the actively selected route profile used during automated movement routines.
 *
 * @param currentRouteText The text label identifying the current active path profile.
 * @param onNavigate Executed when the user clicks the card to change or create route profiles.
 */
@Composable
fun PatrolRouteCard(
    currentRouteText: String,
    onNavigate: () -> Unit
) {
    ConfigCard(onClick = onNavigate) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.ShieldMoon,
                contentDescription = stringResource(R.string.admin_panel_patrol_route)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_patrol_route))
                ConfigSubtext(stringResource(R.string.admin_panel_active_prefix, currentRouteText))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
