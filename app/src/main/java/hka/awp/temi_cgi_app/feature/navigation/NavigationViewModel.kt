package hka.awp.temi_cgi_app.feature.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToLocationChangedListener
import com.robotemi.sdk.navigation.model.Position
import hka.awp.temi_cgi_app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * ViewModel for the Navigation screen.
 * Handles interaction with the temi Robot SDK for movement and location management.
 */
class NavigationViewModel(
    private val robot: Robot,
    application: Application
) : 
    AndroidViewModel(application), 
    OnRobotReadyListener, 
    OnDistanceToLocationChangedListener,
    OnCurrentPositionChangedListener {

    private val _currentLocation = MutableStateFlow(getApplication<Application>().getString(R.string.location_status_locating))
    val currentLocation: StateFlow<String> = _currentLocation.asStateFlow()

    init {
        robot.addOnRobotReadyListener(this)
        robot.addOnDistanceToLocationChangedListener(this)
        robot.addOnCurrentPositionChangedListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        robot.removeOnRobotReadyListener(this)
        robot.removeOnDistanceToLocationChangedListener(this)
        robot.removeOnCurrentPositionChangedListener(this)
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            val savedLocations = robot.locations
            Timber.d("Robot ready, gespeicherte Orte: $savedLocations")
            
            if (savedLocations.isEmpty()) {
                _currentLocation.value = getApplication<Application>().getString(R.string.location_status_none)
            }
            
            robot.getPosition()
        }
    }

    override fun onCurrentPositionChanged(position: Position) {
        Timber.v("Position: x=${position.x}, y=${position.y}")
    }

    override fun onDistanceToLocationChanged(distances: Map<String, Float>) {
        if (distances.isEmpty()) return

        val nearestLocationEntry = distances.minByOrNull { it.value }
        
        nearestLocationEntry?.let { (systemName, distance) ->
            val destination = DestinationItems.fromSystemName(systemName)
            val displayName = if (destination != null) {
                getApplication<Application>().getString(destination.stringResource)
            } else {
                systemName
            }

            if (_currentLocation.value != displayName) {
                _currentLocation.value = displayName
                Timber.d("Standort: $displayName (%.2f m)".format(distance))
            }
        }
    }

    /**
     * Navigiert zum Ort basierend auf der Resource-ID.
     */
    fun goToLocationByResId(resId: Int) {
        val destination = DestinationItems.fromResId(resId)
        if (destination != null) {
            Timber.d("Navigation zu (ResID $resId): ${destination.systemName}")
            try {
                robot.goTo(destination.systemName)
            } catch (t: Throwable) {
                Timber.e(t, "Fehler bei Navigation")
            }
        }
    }

    /**
     * Fallback für direkte String-Navigation (z.B. bei manueller Eingabe).
     */
    fun goToLocation(displayName: String) {
        // Suche zuerst in unseren Destinations
        val destination = DestinationItems.all.find { 
            getApplication<Application>().getString(it.stringResource) == displayName 
        }
        val targetLocation = destination?.systemName ?: displayName.lowercase().trim()
        
        Timber.d("Navigation zu: $targetLocation")
        try {
            robot.goTo(targetLocation)
        } catch (t: Throwable) {
            Timber.e(t, "Fehler bei Navigation")
        }
    }

    fun showMap() {
        Timber.w("Map-Funktion aktuell nicht implementiert.")
    }
}
