package hka.awp.cgi.temi.app.feature.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import hka.awp.cgi.temi.app.R

/**
 * Represents the possible settings entries.
 */
sealed class SettingsItem(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector
                         ) {
    companion object {
        val settingsItems by lazy {
            listOf(
                Notifications,
                Display,
                Battery,
                Location,
                About
                  )
        }
    }

    data object Notifications : SettingsItem(
        titleRes = R.string.settings_notifications_title,
        subtitleRes = R.string.settings_notifications_subtitle,
        icon = Icons.Rounded.Notifications
                                            )

    data object Display : SettingsItem(
        titleRes = R.string.settings_display_title,
        subtitleRes = R.string.settings_display_subtitle,
        icon = Icons.Rounded.Brightness6
                                      )

    data object Battery : SettingsItem(
        titleRes = R.string.settings_battery_title,
        subtitleRes = R.string.settings_battery_subtitle,
        icon = Icons.Rounded.BatteryFull
                                      )

    data object Location : SettingsItem(
        titleRes = R.string.settings_location_title,
        subtitleRes = R.string.settings_location_subtitle,
        icon = Icons.Rounded.LocationOn
                                       )

    data object About : SettingsItem(
        titleRes = R.string.settings_about_title,
        subtitleRes = R.string.settings_about_subtitle,
        icon = Icons.Rounded.Info
                                    )
}
