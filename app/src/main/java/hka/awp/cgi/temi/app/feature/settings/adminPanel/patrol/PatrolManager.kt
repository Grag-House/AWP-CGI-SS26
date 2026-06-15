package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import timber.log.Timber

class PatrolManager(
    private val robot: Robot?
                   ) : OnGoToLocationStatusChangedListener {

    private var activeRoute: List<String> = emptyList()
    private var activeIndex = 0
    private var isRunning = false

    init {
        robot?.addOnGoToLocationStatusChangedListener(this)
    }

    fun startImmediatePatrol(route: List<String>) {
        if (route.isEmpty()) {
            Timber.w("Keine Kontrollroute konfiguriert.")
            return
        }

        if (isRunning) {
            Timber.w("Kontrollfahrt läuft bereits.")
            return
        }

        activeRoute = route
        activeIndex = 0
        isRunning = true

        val firstLocation = activeRoute.first()

        Timber.i("Starte Kontrollfahrt: $activeRoute")
        Timber.d("Fahre zu Kontrollpunkt 1/${activeRoute.size}: $firstLocation")

        robot?.goTo(firstLocation)
    }

    fun stopPatrol() {
        Timber.i("Kontrollfahrt gestoppt.")
        activeRoute = emptyList()
        activeIndex = 0
        isRunning = false
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
                                            ) {
        if (!isRunning) return

        when (status.lowercase()) {
            "complete" -> goToNextLocation()
            "abort", "cancel", "cancelled" -> {
                Timber.w("Kontrollfahrt abgebrochen bei $location.")
                stopPatrol()
            }
        }
    }

    private fun goToNextLocation() {
        activeIndex++

        if (activeIndex >= activeRoute.size) {
            Timber.i("Kontrollfahrt abgeschlossen.")
            stopPatrol()
            return
        }

        val nextLocation = activeRoute[activeIndex]

        Timber.d("Fahre zu Kontrollpunkt ${activeIndex + 1}/${activeRoute.size}: $nextLocation")

        robot?.goTo(nextLocation)
    }

    fun clear() {
        robot?.removeOnGoToLocationStatusChangedListener(this)
    }
}
