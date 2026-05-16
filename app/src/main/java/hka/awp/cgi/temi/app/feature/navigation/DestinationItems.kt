package hka.awp.cgi.temi.app.feature.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.EvStation
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.ui.graphics.vector.ImageVector
import hka.awp.cgi.temi.app.R

/**
 * Alle verfügbaren Navigationsziele des Roboters.
 * [systemName] muss exakt dem auf dem Roboter gespeicherten Wegpunkt-Namen entsprechen.
 */
sealed class DestinationItems(
    @StringRes val stringResource: Int,
    val icon: ImageVector,
    val systemName: String
) {
    data object Kitchen : DestinationItems(R.string.location_kitchen, Icons.Rounded.Kitchen, "keynote kitchen")
    data object Cafe : DestinationItems(R.string.location_cafe, Icons.Rounded.Coffee, "kaffeemaschine")
    data object Reception : DestinationItems(R.string.location_reception, Icons.Rounded.MeetingRoom, "eingang")
    data object Office : DestinationItems(R.string.location_office, Icons.Rounded.Laptop, "besprechungsraum")
    data object WC : DestinationItems(R.string.location_wc, Icons.Rounded.Wc, "toiletten")
    data object Charging : DestinationItems(R.string.location_charging, Icons.Rounded.EvStation, "home base")

    companion object {
        val all = listOf(Kitchen, Cafe, Reception, Office, WC, Charging)

        /** Gibt das Ziel zurück dessen [systemName] mit dem SDK-Wegpunkt-Namen übereinstimmt. */
        fun fromSystemName(name: String): DestinationItems? = all.find { it.systemName == name }

        /** Gibt das Ziel zurück dessen [stringResource] mit der übergebenen ID übereinstimmt. */
        fun fromResId(resId: Int): DestinationItems? = all.find { it.stringResource == resId }
    }
}
