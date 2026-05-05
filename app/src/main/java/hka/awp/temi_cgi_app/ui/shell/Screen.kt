package hka.awp.temi_cgi_app.ui.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import hka.awp.temi_cgi_app.R

/**
 * Represents the different navigation destinations within the application's shell.
 *
 * Each screen definition contains the necessary metadata for navigation handling,
 * UI labeling, and icon rendering.
 *
 * @property route The unique identifier used for navigation routing.
 * @property title The localized display name shown in the UI (e.g., in the navigation drawer or top bar).
 * @property icon An optional [ImageVector] for standard Material icons.
 * @property iconRes An optional drawable resource ID for custom graphical icons.
 * @property contentDescription A localized description of the icon for accessibility services (Screen Readers).
 * @property isCustomIcon A flag indicating if the icon requires special rendering logic beyond standard vector/resource handling.
 */
sealed class Screen(
    val route: String,
    @StringRes val title: Int,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    @StringRes val contentDescription: Int,
    val isCustomIcon: Boolean = false
) {
    companion object {
        val navScreens by lazy { listOf(Dashboard, Webserver, Weather, Navigation, Mode, Settings) }
    }

    data object Dashboard : Screen(
        route = "dashboard",
        title = R.string.hauptmenu,
        icon = Icons.Rounded.Home,
        contentDescription = R.string.home_description
    )

    data object Webserver : Screen(
        route = "webserver",
        title = R.string.webserver,
        icon = Icons.Rounded.Storage,
        contentDescription = R.string.webserver_description
    )

    data object Weather : Screen(
        route = "weather",
        title = R.string.wetter,
        iconRes = R.drawable.partly_cloudy_day,
        contentDescription = R.string.weather_description
    )

    data object Navigation : Screen(
        route = "navigation",
        title = R.string.navigation,
        icon = Icons.Rounded.Navigation,
        contentDescription = R.string.navigation_description
    )

    data object Mode : Screen(
        route = "mode",
        title = R.string.modus,
        isCustomIcon = true,
        contentDescription = R.string.mode_description
    )

    data object Settings : Screen(
        route = "settings",
        title = R.string.settings,
        icon = Icons.Rounded.Settings,
        contentDescription = R.string.settings_description
    )

    data object DisplaySettings : Screen(
        route = "display_settings",
        title = R.string.settings_display_title,
        icon = Icons.Rounded.Settings,
        contentDescription = R.string.settings_display_subtitle
    )

    data object NotificationSettings : Screen(
        route = "notification_settings",
        title = R.string.settings_notifications_title,
        icon = Icons.Rounded.Notifications,
        contentDescription = R.string.settings_notifications_subtitle
    )

    data object BatterySettings : Screen(
        route = "battery_settings",
        title = R.string.settings_battery_title,
        icon = Icons.Rounded.BatteryFull,
        contentDescription = R.string.settings_battery_subtitle
    )
}
