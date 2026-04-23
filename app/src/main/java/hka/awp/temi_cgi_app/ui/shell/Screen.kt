package hka.awp.temi_cgi_app.ui.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import hka.awp.temi_cgi_app.R

sealed class Screen(
    val route: String,
    @StringRes val title: Int,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val contentDescription: String,
    val isCustomIcon: Boolean = false
) {
    object Dashboard : Screen(
        route = "dashboard",
        title = R.string.hauptmenu,
        icon = Icons.Rounded.Home,
        contentDescription = "Home icon for the main dashboard"
    )

    object Webserver : Screen(
        route = "webserver",
        title = R.string.webserver,
        icon = Icons.Rounded.Storage,
        contentDescription = "Storage icon representing server status"
    )

    object Weather : Screen(
        route = "weather",
        title = R.string.wetter,
        iconRes = R.drawable.partly_cloudy_day,
        contentDescription = "Sun and cloud symbol for local weather"
    )

    object Navigation : Screen(
        route = "navigation",
        title = R.string.navigation,
        icon = Icons.Rounded.Navigation,
        contentDescription = "Arrow icon for robot navigation"
    )

    object Mode : Screen(
        route = "mode",
        title = R.string.modus,
        isCustomIcon = true,
        contentDescription = "Dual toggle switches for operation mode selection"
    )

    object Settings : Screen(
        route = "settings",
        title = R.string.settings,
        icon = Icons.Rounded.Settings,
        contentDescription = "Gear icon for system settings"
    )
}
