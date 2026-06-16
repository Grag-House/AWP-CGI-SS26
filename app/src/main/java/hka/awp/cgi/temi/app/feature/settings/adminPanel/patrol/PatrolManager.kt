package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class PatrolManager(
    private val robot: Robot?,
    private val cameraStreamManager: PatrolCameraStreamManager
) : OnGoToLocationStatusChangedListener {

    private val scope = CoroutineScope(Dispatchers.Main)

    private var activeRoute: List<String> = emptyList()
    private var activeIndex = 0
    private var isRunning = false
    private var scanJob: Job? = null
    private var activeCameraTiltAngle = 0
    private var schedulerJob: Job? = null

    init {
        robot?.addOnGoToLocationStatusChangedListener(this)
        cameraStreamManager.connect()
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

        cameraStreamManager.connect()
        cameraStreamManager.startStream()

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

            cameraStreamManager.sendPatrolPointReached(activeRoute[activeIndex])

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
        cameraStreamManager.stopStream()
    }

    fun clear() {
        schedulerJob?.cancel()
        schedulerJob = null
        stopPatrol()
        robot?.removeOnGoToLocationStatusChangedListener(this)
        cameraStreamManager.disconnect()
    }

    private suspend fun runRandomSchedule(settings: PatrolSettings) {
        while (true) {
            val min = settings.minMinutes.coerceAtLeast(1)
            val max = settings.maxMinutes.coerceAtLeast(min)
            val delayMinutes = Random.nextInt(from = min, until = max + 1)

            Timber.d("Nächste zufällige Kontrollfahrt in $delayMinutes Minuten.")

            delay(delayMinutes * 60_000L)

            startImmediatePatrol(settings.route)
        }
    }

    private suspend fun runFixedSchedule(settings: PatrolSettings) {
        if (settings.hours.isEmpty()) {
            Timber.w("Keine festen Stunden für Kontrollfahrt ausgewählt.")
            return
        }

        while (true) {
            val nextRun = getNextFullHourRun(settings.hours)
            val delayMs = ChronoUnit.MILLIS.between(LocalDateTime.now(), nextRun)
                .coerceAtLeast(0)

            Timber.d("Nächste feste Kontrollfahrt um $nextRun in ${delayMs / 1000} Sekunden.")

            delay(delayMs)

            startImmediatePatrol(settings.route)
        }
    }

    private fun getNextFullHourRun(hours: Set<Int>): LocalDateTime {
        val validHours = hours
            .filter { it in 0..23 }
            .sorted()

        val now = LocalDateTime.now()
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val nextToday = validHours
            .map { hour -> now.withHour(hour) }
            .firstOrNull { time -> time.isAfter(LocalDateTime.now()) }

        if (nextToday != null) return nextToday

        val firstHourTomorrow = validHours.firstOrNull() ?: 0

        return now
            .plusDays(1)
            .withHour(firstHourTomorrow)
    }

    fun updateSchedule(settings: PatrolSettings) {
        schedulerJob?.cancel()
        schedulerJob = null

        if (!settings.isEnabled) {
            Timber.d("Automatische Kontrollfahrten deaktiviert.")
            return
        }

        if (settings.route.isEmpty()) {
            Timber.w("Keine Kontrollroute konfiguriert. Scheduler startet nicht.")
            return
        }

        schedulerJob = scope.launch {
            when (settings.mode) {
                PatrolMode.RANDOM -> runRandomSchedule(settings)
                PatrolMode.FIXED -> runFixedSchedule(settings)
            }
        }
    }
}
