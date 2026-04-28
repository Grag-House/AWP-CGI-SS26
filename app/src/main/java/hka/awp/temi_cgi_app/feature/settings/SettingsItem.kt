package hka.awp.temi_cgi_app.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the possible setting entries.
 */
sealed class SettingsItem(
    val title: String, val subtitle: String, val icon: ImageVector
) {
    companion object {
        val settingsItems by lazy {
            listOf(
                Notifications, Display, Battery, Location, About
            )
        }
    }

    data object Notifications : SettingsItem(
        title = "Benachrichtigungen",
        subtitle = "Töne und Systemmeldungen",
        icon = Icons.Rounded.Notifications
    )

    data object Display : SettingsItem(
        title = "Anzeige",
        subtitle = "Helligkeit und Bildschirmschoner",
        icon = Icons.Rounded.Brightness6
    )

    data object Battery : SettingsItem(
        title = "Akku",
        subtitle = "Energieverbrauch und Akkustatus",
        icon = Icons.Rounded.BatteryFull
    )

    data object Location : SettingsItem(
        title = "Standort",
        subtitle = "Ortungsdienste und Navigation",
        icon = Icons.Rounded.LocationOn
    )

    data object About : SettingsItem(
        title = "Über",
        subtitle = "Informationen über das System",
        icon = Icons.Rounded.Info
    )
}