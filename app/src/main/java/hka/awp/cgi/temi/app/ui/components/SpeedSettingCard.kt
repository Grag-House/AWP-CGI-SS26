package hka.awp.cgi.temi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.robotemi.sdk.navigation.model.SpeedLevel

/**
 * A reusable settings card component to display and select different speed levels for the Temi robot.
 *
 * @param icon The icon representing the type of speed configuration.
 * @param title The main heading for the setting card.
 * @param subtitle A detailed description explaining what the speed level configures.
 * @param currentSpeed The currently active [SpeedLevel] to mark the selected chip.
 * @param onSpeedChange Callback triggered when a different [SpeedLevel] chip is selected.
 * @param modifier Optional [Modifier] to apply layout adjustments to the card.
 */
@Composable
fun SpeedSettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    currentSpeed: SpeedLevel,
    onSpeedChange: (SpeedLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val speedLevels = listOf(
        SpeedLevel.VERY_SLOW,
        SpeedLevel.SLOW,
        SpeedLevel.MEDIUM,
        SpeedLevel.HIGH,
        SpeedLevel.VERY_HIGH
    )

    SettingsCard(modifier = modifier) {
        SettingsRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            action = {}
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            speedLevels.forEach { level ->
                FilterChip(
                    selected = currentSpeed == level,
                    onClick = { onSpeedChange(level) },
                    label = { Text(text = level.name) }
                )
            }
        }
    }
}
