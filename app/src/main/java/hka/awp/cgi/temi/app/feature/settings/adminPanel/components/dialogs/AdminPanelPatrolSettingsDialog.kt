package hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R
import java.util.Locale

/**
 * Supported execution strategy modes for automated robot patrol patterns.
 */
enum class AdminPanelPatrolSettingsDialog {
    /** Patrol routines are dispatched dynamically within a variable time interval. */
    RANDOM,

    /** Patrol routines are dispatched precisely at fixed hourly intervals. */
    FIXED
}

/**
 * Displays a modal dialog for scheduling and configuring the robot's automated patrol profiles.
 *
 * This complex configuration dialog manages mutational UI state tracking for runtime variables such as
 * master switch triggers, randomized time-window bounds via sliders, and targeted hour-of-day filter chips.
 *
 * @param initialIsEnabled Flag indicating whether automated patrolling is globally enabled on launch.
 * @param initialMode The startup strategy profile (Randomized vs Fixed schedules) to initialize.
 * @param initialMinMinutes The fallback minimum time offset boundary value for localized random iterations.
 * @param initialMaxMinutes The fallback maximum time offset boundary value for localized random iterations.
 * @param initialHours A distinct set of assigned operating hours predefined for fixed schedules.
 * @param onTriggerPatrol Callback triggered when bypassing schedules to initiate an instantaneous patrol cycle.
 * @param onDismiss Callback triggered when closing the overlay layout window interface.
 * @param onSave Event hook fired when finalizing changes, pushing altered
 * telemetry data back to persistence boundaries.
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod")
@Composable
fun AdminPanelPatrolSettingsDialog(
    initialIsEnabled: Boolean = false,
    initialMode: AdminPanelPatrolSettingsDialog = AdminPanelPatrolSettingsDialog.RANDOM,
    initialMinMinutes: Int = 40,
    initialMaxMinutes: Int = 60,
    initialHours: Set<Int> = emptySet(),
    onTriggerPatrol: () -> Unit,
    onDismiss: () -> Unit,
    onSave:
    (isEnabled: Boolean, mode: AdminPanelPatrolSettingsDialog, minMin: Int, maxMin: Int, hours: Set<Int>) -> Unit
) {
    var isEnabled by remember { mutableStateOf(initialIsEnabled) }
    var selectedMode by remember { mutableStateOf(initialMode) }
    var minMinutes by remember { mutableFloatStateOf(initialMinMinutes.toFloat()) }
    var maxMinutes by remember { mutableFloatStateOf(initialMaxMinutes.toFloat()) }
    var selectedHours by remember { mutableStateOf(initialHours) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.patrol_settings_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.patrol_settings_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEnabled = !isEnabled },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.patrol_settings_enable_label),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                if (isEnabled) {
                    Button(
                        onClick = {
                            onTriggerPatrol()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.patrol_settings_trigger_now_button))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.patrol_settings_mode_label),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = AdminPanelPatrolSettingsDialog.RANDOM },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == AdminPanelPatrolSettingsDialog.RANDOM,
                                onClick = { selectedMode = AdminPanelPatrolSettingsDialog.RANDOM }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.patrol_settings_mode_random))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = AdminPanelPatrolSettingsDialog.FIXED },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == AdminPanelPatrolSettingsDialog.FIXED,
                                onClick = { selectedMode = AdminPanelPatrolSettingsDialog.FIXED }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.patrol_settings_mode_fixed))
                        }
                    }

                    if (selectedMode == AdminPanelPatrolSettingsDialog.RANDOM) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                stringResource(R.string.patrol_settings_interval_bounds_label),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            Text(stringResource(R.string.patrol_settings_min_distance, minMinutes.toInt()))
                            Slider(
                                value = minMinutes,
                                onValueChange = { minMinutes = it.coerceAtMost(maxMinutes) },
                                valueRange = 10f..120f
                            )

                            Text(stringResource(R.string.patrol_settings_max_distance, maxMinutes.toInt()))
                            Slider(
                                value = maxMinutes,
                                onValueChange = { maxMinutes = it.coerceAtLeast(minMinutes) },
                                valueRange = 10f..120f
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.patrol_settings_select_hours_label),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                @Suppress("MagicNumber")
                                for (hour in 0..23) {
                                    val isSelected = hour in selectedHours
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedHours = if (isSelected) {
                                                selectedHours - hour
                                            } else {
                                                selectedHours + hour
                                            }
                                        },
                                        label = { Text(text = String.format(Locale.GERMANY, "%02d:00", hour)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(isEnabled, selectedMode, minMinutes.toInt(), maxMinutes.toInt(), selectedHours)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
        }
    )
}

/**
 * Displays a warning alert dialog indicating that no valid navigation pathway route is currently selected.
 *
 * This popup is typically triggered as a safety validation checkpoint preventing automated operation starts
 * when missing crucial orientation track definitions.
 *
 * @param onDismiss Callback triggered when the notification modal is acknowledged or dismissed.
 */
@Composable
fun NoRouteSelectedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = stringResource(R.string.admin_panel_no_route_selected_title))
        },
        text = {
            Text(
                text = stringResource(R.string.admin_panel_no_route_selected_text),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_confirm))
            }
        }
    )
}
