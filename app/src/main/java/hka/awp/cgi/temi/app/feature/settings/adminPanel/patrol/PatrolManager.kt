package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class PatrolManager(
    private val robot: Robot?
                   ) : OnGoToLocationStatusChangedListener {

    private val scope = CoroutineScope(Dispatchers.Main)

    private var activeRoute: List<String> = emptyList()
    private var activeIndex = 0
    private var isRunning = false
    private var scanJob: Job? = null
    private var activeCameraTiltAngle = 0

    init {
        robot?.addOnGoToLocationStatusChangedListener(this)
    }

    private companion object {
        private const val MIN_TILT_ANGLE = -30
        private const val MAX_TILT_ANGLE = 50
        private const val CAMERA_TILT_SPEED = 0.4f
    }

    fun startImmediatePatrol(
        route: List<String>,
        cameraTiltAngle: Int = 0
                            ) {
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

        moveToCurrentLocation()
    }

    private fun moveToCurrentLocation() {
        val location = activeRoute[activeIndex]

        Timber.d("Fahre zu Kontrollpunkt ${activeIndex + 1}/${activeRoute.size}: $location")

        robot?.goTo(location)
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
                                            ) {
        if (!isRunning) return

        when (status.lowercase()) {
            "complete" -> {
                Timber.d("Kontrollpunkt erreicht: $location")
                scanAtCurrentPoint(activeCameraTiltAngle)
            }

            "abort", "cancel", "cancelled" -> {
                Timber.w("Kontrollfahrt abgebrochen bei $location.")
                stopPatrol()
            }
        }
    }

    private fun scanAtCurrentPoint(cameraTiltAngle: Int) {
        scanJob?.cancel()
        scanJob = scope.launch {
            Timber.d("Starte Scan-Sequenz an Kontrollpunkt ${activeIndex + 1}")

            setCameraTilt(cameraTiltAngle)
            delay(1500L)

            activateCameraPlaceholder()

            rotateSlowly()

            goToNextLocation()
        }
    }

    private suspend fun rotateSlowly() {
        Timber.d("Starte Drehung")

        robot?.turnBy(90)
        delay(2500L)

        robot?.turnBy(90)
        delay(2500L)

        robot?.turnBy(90)
        delay(2500L)

        robot?.turnBy(90)
        delay(2500L)

        Timber.d("Drehung beendet")
    }

    private fun activateCameraPlaceholder() {
        Timber.d("Kamera aktivieren / Bildstream starten TODO")
        // TODO:
        // - Android CameraX oder Temi Kamera-API öffnen
        // - Frame/Bild erfassen
        // - später an Webserver senden
    }

    private fun setCameraTilt(angle: Int) {
        val safeAngle = angle.coerceIn(MIN_TILT_ANGLE, MAX_TILT_ANGLE)

        Timber.d("Setze Kamerawinkel auf $safeAngle Grad")

        robot?.tiltAngle(
            degrees = safeAngle,
            speed = CAMERA_TILT_SPEED
                        )
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

    fun stopPatrol() {
        scanJob?.cancel()
        scanJob = null
        activeRoute = emptyList()
        activeIndex = 0
        isRunning = false
        robot?.stopMovement()
    }

    fun clear() {
        stopPatrol()
        robot?.removeOnGoToLocationStatusChangedListener(this)
    }
}
