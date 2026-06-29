package hka.awp.cgi.temi.app.feature.patrol

import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("TooManyFunctions")
class PatrolManager(
    private val robot: Robot?,
    private val cameraStreamManager: PatrolCameraStreamManager,
    private val mqttManager: MqttManager
) : OnGoToLocationStatusChangedListener {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val scanner = PatrolScanner(robot)
    private val scheduler = PatrolScheduler(scope, ::startAutomaticPatrol)

    private var activeRoute: List<String> = emptyList()
    private var activeIndex = 0
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private var scanJob: Job? = null
    private var activeCameraTiltAngle = 0
    private val analysisHandler = PatrolAnalysisHandler(
        robot = robot,
        scope = scope,
        cameraStreamManager = cameraStreamManager,
        onEmergencyDetected = {
            pausePatrolForEmergency()
        },
        onObservationFinished = {
            resumePatrolAfterEmergency()
        }
    )
    private val _countdownSeconds = MutableStateFlow<Int?>(null)
    private var ignoreAbortUntilMs = 0L
    val countdownSeconds: StateFlow<Int?> = _countdownSeconds.asStateFlow()
    private companion object {
        private const val PATROL_COUNTDOWN_SECONDS = 30
        private const val ANNOUNCEMENT_TIMEOUT_MS = 12_000L
        private const val CAMERA_TILT_SPEED = 0.4f
        private const val STABILIZATION_DELAY_MS = 700L
        private const val STABILIZATION_REPEATS = 3
        private const val DEFAULT_CAMERA_ANGLE = 0

        private const val AUTOMATIC_PATROL_DELAY = 1_000L
    }

    init {
        robot?.addOnGoToLocationStatusChangedListener(this)
    }

    fun startImmediatePatrol(route: List<String>, cameraTiltAngle: Int = 0): Boolean {
        if (route.isEmpty()) {
            Timber.w("Keine Kontrollroute konfiguriert.")
            return false
        }
        if (_isRunning.value) {
            Timber.w("Kontrollfahrt läuft bereits.")
            return false
        }
        _countdownSeconds.value = null

        scope.launch {
            announcePatrolStart()
        }

        robot?.toggleNavigationBillboard(disabled = true)

        activeRoute = route
        activeIndex = 0
        _isRunning.value = true
        activeCameraTiltAngle = cameraTiltAngle

        Timber.i("Starte Kontrollfahrt: $activeRoute")
        cameraStreamManager.startStream()
        analysisHandler.start()

        moveToCurrentLocation()
        return true
    }

    private suspend fun announcePatrolStart() {
        Timber.d("Sende Patrol Announcement Prompt über MQTT")
        mqttManager.publishPatrolAnnouncementPrompt()

        mqttManager.waitForTtsCompleted(timeoutMs = ANNOUNCEMENT_TIMEOUT_MS)

        robot?.finishConversation()
    }

    private suspend fun startAutomaticPatrol(route: List<String>) {
        for (seconds in PATROL_COUNTDOWN_SECONDS downTo 1) {
            _countdownSeconds.value = seconds
            delay(AUTOMATIC_PATROL_DELAY)
        }

        _countdownSeconds.value = null

        startImmediatePatrol(route, cameraTiltAngle = 0)
    }

    private fun moveToCurrentLocation() {
        val location = activeRoute.getOrNull(activeIndex) ?: return
        Timber.d("Fahre zu Kontrollpunkt ${activeIndex + 1}/${activeRoute.size}: $location")
        robot?.goTo(location)
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        if (!_isRunning.value) return

        when (status.lowercase()) {
            "complete" -> {
                if (analysisHandler.isObserving) {
                    Timber.d("Complete während Lying-Beobachtung ignoriert.")
                    return
                }

                Timber.d("Kontrollpunkt erreicht: $location")
                stabilizeCameraAndScan()
            }

            "abort", "cancel", "cancelled" -> {
                if (analysisHandler.isObserving || System.currentTimeMillis() < ignoreAbortUntilMs) {
                    Timber.w("GoTo-Abbruch wegen Lying/Emergency ignoriert.")
                    return
                }

                Timber.w("Kontrollfahrt abgebrochen bei $location.")
                stopPatrol()
            }
        }
    }

    private fun stabilizeCameraAndScan() {
        scanJob?.cancel()
        scanJob = scope.launch {
            repeat(STABILIZATION_REPEATS) {
                robot?.tiltAngle(degrees = DEFAULT_CAMERA_ANGLE, speed = CAMERA_TILT_SPEED)
                delay(STABILIZATION_DELAY_MS)
            }

            scanAtCurrentPoint()
        }
    }

    private fun scanAtCurrentPoint() {
        scanJob?.cancel()
        scanJob = scope.launch {
            val currentLoc = activeRoute[activeIndex]

            scanner.executeScanSequence {
                cameraStreamManager.sendPatrolPointReached(currentLoc)
            }

            goToNextLocation()
        }
    }

    @Suppress("MagicNumber")
    private fun pausePatrolForEmergency() {
        Timber.w("Kontrollfahrt pausiert wegen Lying-Erkennung.")

        scanJob?.cancel()
        scanJob = null

        ignoreAbortUntilMs = System.currentTimeMillis() + 3_000L
        robot?.stopMovement()
        scope.launch {
            repeat(STABILIZATION_REPEATS) {
                robot?.tiltAngle(degrees = DEFAULT_CAMERA_ANGLE, speed = CAMERA_TILT_SPEED)
                delay(STABILIZATION_DELAY_MS)
            }
        }
    }

    private fun resumePatrolAfterEmergency() {
        Timber.i("Lying-Beobachtung beendet. Setze Kontrollfahrt fort.")

        if (!_isRunning.value) return
        moveToCurrentLocation()
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
        _isRunning.value = false
        robot?.stopMovement()
        cameraStreamManager.stopStream()
        analysisHandler.stop()
        robot?.toggleNavigationBillboard(disabled = false)
        _countdownSeconds.value = null
    }
}
