package hka.awp.cgi.temi.app.feature.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.CorporateFare
import androidx.compose.material.icons.rounded.EvStation
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.ui.graphics.vector.ImageVector
import hka.awp.cgi.temi.app.R

/**
 * Represents the available navigation destinations within the application.
 *
 * This sealed class defines specific points of interest (POIs) that the user or robot can navigate to.
 * Each destination is associated with a localized string resource for its label and a vector icon
 * for visual representation in the UI.
 *
 * @property stringResource The resource ID for the localized name of the destination.
 * @property icon The [ImageVector] used to visually identify the destination.
 */
sealed class DestinationItems(@StringRes val stringResource: Int, val icon: ImageVector) {
    companion object {
        val destinations by lazy {
            listOf(
                Kitchen,
                Coffee,
                MeetingRoom,
                Laptop,
                WC,
                ChargingStation
            )
        }
    }

    data object Office : DestinationItems(R.string.location_office, Icons.Rounded.CorporateFare)

    data object Kitchen : DestinationItems(R.string.location_kitchen, Icons.Rounded.Kitchen)

    data object Coffee : DestinationItems(R.string.location_kitchen, Icons.Rounded.Coffee)

    data object MeetingRoom :
        DestinationItems(R.string.location_reception, Icons.Rounded.MeetingRoom)

    data object Laptop : DestinationItems(R.string.location_office, Icons.Rounded.Laptop)

    data object WC : DestinationItems(R.string.location_wc, Icons.Rounded.Wc)

    data object ChargingStation :
        DestinationItems(R.string.location_charging, Icons.Rounded.EvStation)
}
