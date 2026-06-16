package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class PatrolManager(
    private val robot: Robot?,
    private val cameraStreamManager: PatrolCameraStreamManager
) : OnGoToLocationStatusChangedListener {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val scanner = PatrolScanner(robot)
    private val scheduler = PatrolScheduler(scope, ::startAutomaticPatrol)

    private var activeRoute: List<String> = emptyList()
    private var activeIndex = 0
    private var isRunning = false
    private var scanJob: Job? = null
    private var activeCameraTiltAngle = 0

    init {
        robot?.addOnGoToLocationStatusChangedListener(this)
        cameraStreamManager.connect()
    }

    fun startImmediatePatrol(route: List<String>, cameraTiltAngle: Int = 0) {
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
        activeCameraTiltAngle = cameraTiltAngle

        Timber.i("Starte Kontrollfahrt: $activeRoute")
        cameraStreamManager.connect()
        cameraStreamManager.startStream()

        moveToCurrentLocation()
    }

    private fun startAutomaticPatrol(route: List<String>) {
        // Callback für den Scheduler (nutzt Standard-Tilt 0)
        startImmediatePatrol(route, cameraTiltAngle = 0)
    }

    private fun moveToCurrentLocation() {
        val location = activeRoute.getOrNull(activeIndex) ?: return
        Timber.d("Fahre zu Kontrollpunkt ${activeIndex + 1}/${activeRoute.size}: $location")
        robot?.goTo(location)
    }

    override fun onGoToLocationStatusChanged(location: String, status: String, descriptionId: Int, description: String) {
        if (!isRunning) return

        when (status.lowercase()) {
            "complete" -> {
                Timber.d("Kontrollpunkt erreicht: $location")
                scanAtCurrentPoint()
            }
            "abort", "cancel", "cancelled" -> {
                Timber.w("Kontrollfahrt abgebrochen bei $location.")
                stopPatrol()
            }
        }
    }

    private fun scanAtCurrentPoint() {
        scanJob?.cancel()
        scanJob = scope.launch {
            val currentLoc = activeRoute[activeIndex]

            // Aufruf des ausgelagerten Scanners
            scanner.executeScanSequence(activeCameraTiltAngle) {
                cameraStreamManager.sendPatrolPointReached(currentLoc)
            }

            goToNextLocation()
        }
    }

    private fun goToNextLocation() {
        activeIndex++
        if (activeIndex >= activeRoute.size) {
            Timber.i("Kontrollfahrt abgeschlossen.")
            stopPatrol()
            return
        }
        moveToCurrentLocation()
    }

    fun updateSchedule(settings: PatrolSettings) {
        scheduler.updateSchedule(settings)
    }

    fun stopPatrol() {
        scanJob?.cancel()
        scanJob = null
        activeRoute = emptyList()
        activeIndex = 0
        isRunning = false
        robot?.stopMovement()
        cameraStreamManager.stopStream()
    }

    fun clear() {
        scheduler.cancel()
        stopPatrol()
        robot?.removeOnGoToLocationStatusChangedListener(this)
        cameraStreamManager.disconnect()
    }
}
