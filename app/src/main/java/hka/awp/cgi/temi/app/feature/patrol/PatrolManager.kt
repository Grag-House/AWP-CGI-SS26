package hka.awp.cgi.temi.app.feature.patrol

import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import hka.awp.cgi.temi.app.data.model.PatrolSettings
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * Orchestrates the robot's patrol operations.
 *
 * This class acts as the central state machine for patrols, managing navigation,
 * scanning cycles, and emergency responses via [PatrolAnalysisHandler].
 *
 * @param robot The Temi SDK [Robot] instance.
 * @param cameraStreamManager Manager for the patrol camera stream.
 * @param mqttManager Interface for sending patrol notifications and TTS prompts.
 */
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

    /** Indicates whether a patrol is currently active. */
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var scanJob: Job? = null
    private var activeCameraTiltAngle = 0
    private val analysisHandler = PatrolAnalysisHandler(
        robot = robot,
        scope = scope,
        cameraStreamManager = cameraStreamManager,
        onEmergencyDetected = { pausePatrolForEmergency() },
        onObservationFinished = { resumePatrolAfterEmergency() }
    )

    private val _countdownSeconds = MutableStateFlow<Int?>(null)
    private var ignoreAbortUntilMs = 0L

    /** Returns the current countdown value if a patrol is scheduled to start. */
    val countdownSeconds: StateFlow<Int?> = _countdownSeconds.asStateFlow()

    private companion object {
        private const val PATROL_COUNTDOWN_SECONDS = 30
        private const val ANNOUNCEMENT_TIMEOUT_MS = 12_000L
        private const val CAMERA_TILT_SPEED = 0.4f
        private const val STABILIZATION_DELAY_MS = 700L
        private const val STABILIZATION_REPEATS = 3
        private const val DEFAULT_CAMERA_ANGLE = 0
        private const val AUTOMATIC_PATROL_DELAY = 1_000L
        private const val ABORT_IGNORE_WINDOW_MS = 3_000L
    }

    init {
        robot?.addOnGoToLocationStatusChangedListener(this)
    }

    /**
     * Initiates a manual, immediate patrol sequence.
     *
     * @param route The list of location names to navigate to.
     * @param cameraTiltAngle The desired camera tilt during movement.
     * @return True if the patrol started successfully, false if already running or invalid.
     */
    fun startImmediatePatrol(route: List<String>, cameraTiltAngle: Int = 0): Boolean {
        if (route.isEmpty()) {
            Timber.w("No patrol route configured.")
            return false
        }
        if (_isRunning.value) {
            Timber.w("Patrol is already running.")
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

        Timber.i("Starting patrol: %s", activeRoute)
        cameraStreamManager.startStream()
        analysisHandler.start()

        moveToCurrentLocation()
        return true
    }

    private suspend fun announcePatrolStart() {
        Timber.d("Sending patrol announcement prompt via MQTT")
        mqttManager.publishPatrolAnnouncementPrompt()
        mqttManager.waitForTtsCompleted(timeoutMs = ANNOUNCEMENT_TIMEOUT_MS)
        robot?.finishConversation()
    }

    private suspend fun startAutomaticPatrol(route: List<String>) {
        for (seconds in PATROL_COUNTDOWN_SECONDS downTo 1) {
            _countdownSeconds.value = seconds
            delay(AUTOMATIC_PATROL_DELAY.milliseconds)
        }
        _countdownSeconds.value = null
        startImmediatePatrol(route, cameraTiltAngle = 0)
    }

    private fun moveToCurrentLocation() {
        val location = activeRoute.getOrNull(activeIndex) ?: return
        Timber.d("Moving to patrol point %d/%d: %s", activeIndex + 1, activeRoute.size, location)
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
                    Timber.d("Ignored 'complete' status during emergency observation.")
                    return
                }
                Timber.d("Patrol point reached: %s", location)
                stabilizeCameraAndScan()
            }

            "abort", "cancel", "cancelled" -> {
                if (analysisHandler.isObserving || System.currentTimeMillis() < ignoreAbortUntilMs) {
                    Timber.w("Ignored navigation abort due to emergency/observation.")
                    return
                }
                Timber.w("Patrol aborted at: %s", location)
                stopPatrol()
            }
        }
    }

    private fun stabilizeCameraAndScan() {
        scanJob?.cancel()
        scanJob = scope.launch {
            repeat(STABILIZATION_REPEATS) {
                robot?.tiltAngle(degrees = DEFAULT_CAMERA_ANGLE, speed = CAMERA_TILT_SPEED)
                delay(STABILIZATION_DELAY_MS.milliseconds)
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

    private fun pausePatrolForEmergency() {
        Timber.w("Patrol paused due to emergency detection.")
        scanJob?.cancel()
        scanJob = null
        ignoreAbortUntilMs = System.currentTimeMillis() + ABORT_IGNORE_WINDOW_MS
        robot?.stopMovement()
        scope.launch {
            repeat(STABILIZATION_REPEATS) {
                robot?.tiltAngle(degrees = DEFAULT_CAMERA_ANGLE, speed = CAMERA_TILT_SPEED)
                delay(STABILIZATION_DELAY_MS.milliseconds)
            }
        }
    }

    private fun resumePatrolAfterEmergency() {
        Timber.i("Emergency observation finished. Resuming patrol.")
        if (!_isRunning.value) return
        moveToCurrentLocation()
    }

    private fun goToNextLocation() {
        activeIndex++
        if (activeIndex >= activeRoute.size) {
            Timber.i("Patrol completed.")
            stopPatrol()
            return
        }
        moveToCurrentLocation()
    }

    /** Updates the automated patrol schedule configuration. */
    fun updateSchedule(settings: PatrolSettings) {
        scheduler.updateSchedule(settings)
    }

    /** Stops the patrol, resets state, and cleans up resources. */
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
