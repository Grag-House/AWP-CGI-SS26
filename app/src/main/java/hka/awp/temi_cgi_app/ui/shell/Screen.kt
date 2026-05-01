package hka.awp.temi_cgi_app.ui.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Navigation
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
    val contentDescription: String,
    val isCustomIcon: Boolean = false
) {
    companion object {
        val navScreens by lazy { listOf(Dashboard, Webserver, Weather, Navigation, Mode, Settings) }
    }

    data object Dashboard : Screen(
        route = "dashboard",
        title = R.string.hauptmenu,
        icon = Icons.Rounded.Home,
        contentDescription = "Home icon for the main dashboard"
    )

    data object Webserver : Screen(
        route = "webserver",
        title = R.string.webserver,
        icon = Icons.Rounded.Storage,
        contentDescription = "Storage icon representing server status"
    )

    data object Weather : Screen(
        route = "weather",
        title = R.string.wetter,
        iconRes = R.drawable.partly_cloudy_day,
        contentDescription = "Sun and cloud symbol for local weather"
    )

    data object Navigation : Screen(
        route = "navigation",
        title = R.string.navigation,
        icon = Icons.Rounded.Navigation,
        contentDescription = "Arrow icon for robot navigation"
    )

    data object Mode : Screen(
        route = "mode",
        title = R.string.modus,
        isCustomIcon = true,
        contentDescription = "Dual toggle switches for operation mode selection"
    )

    data object Settings : Screen(
        route = "settings",
        title = R.string.settings,
        icon = Icons.Rounded.Settings,
        contentDescription = "Gear icon for system settings"
    )
}
