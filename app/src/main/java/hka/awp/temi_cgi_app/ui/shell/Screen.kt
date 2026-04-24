package hka.awp.temi_cgi_app.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import hka.awp.temi_cgi_app.R

sealed class Screen(
    val route: String,
    val title: String,
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
        title = "Hauptmenü",
        icon = Icons.Rounded.Home,
        contentDescription = "Home icon for the main dashboard"
    )

    data object Webserver : Screen(
        route = "webserver",
        title = "WebServer",
        icon = Icons.Rounded.Storage,
        contentDescription = "Storage icon representing server status"
    )

    data object Weather : Screen(
        route = "weather",
        title = "Wetter",
        iconRes = R.drawable.partly_cloudy_day,
        contentDescription = "Sun and cloud symbol for local weather"
    )

    data object Navigation : Screen(
        route = "navigation",
        title = "Navigation",
        icon = Icons.Rounded.Navigation,
        contentDescription = "Arrow icon for robot navigation"
    )

    data object Mode : Screen(
        route = "mode",
        "Modus",
        isCustomIcon = true,
        contentDescription = "Dual toggle switches for operation mode selection"
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Rounded.Settings,
        contentDescription = "Gear icon for system settings"
    )
}