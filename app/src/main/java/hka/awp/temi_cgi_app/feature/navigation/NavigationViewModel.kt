package hka.awp.temi_cgi_app.feature.navigation

import androidx.lifecycle.ViewModel
import timber.log.Timber

class NavigationViewModel : ViewModel() {
    fun onNavigationClick(destination: DestinationItems) {
        when (destination) {
            DestinationItems.Kitchen -> Timber.d("Kitchen navigation triggered!")
            DestinationItems.ChargingStation -> TODO()
            DestinationItems.Coffee -> TODO()
            DestinationItems.Laptop -> TODO()
            DestinationItems.MeetingRoom -> TODO()
            DestinationItems.Office -> TODO()
            DestinationItems.WC -> TODO()
        }
    }
}