package hka.awp.cgi.temi.app.feature.patrol

import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Monitors the patrol camera feed for emergency states (e.g., "lying").
 * * This handler processes incoming stream messages and manages the confirmation lifecycle
 * to avoid false positives, including observation timers and alarm triggering.
 *
 * @param robot The Temi SDK [Robot] instance.
 * @param scope Coroutine scope for running observation and alarm timers.
 * @param cameraStreamManager The source of incoming camera state messages.
 * @param onEmergencyDetected Callback to pause patrol movement when a threat is identified.
 * @param onObservationFinished Callback to resume patrol once observation or alarms conclude.
 */
class PatrolAnalysisHandler(
    private val robot: Robot?,
    private val scope: CoroutineScope,
    private val cameraStreamManager: PatrolCameraStreamManager,
    private val onEmergencyDetected: () -> Unit,
    private val onObservationFinished: () -> Unit
) {
    private var observationJob: Job? = null
    private var latestState: String? = null
    private var collectJob: Job? = null

    private var lyingCountAfterFirstDetection = 0
    private var isObservingLying = false
    private var ignoreNewLyingUntilMs = 0L
    private var finalAlarmTriggered = false

    val isObserving: Boolean
        get() = isObservingLying

    /** Starts the flow collection of camera stream state messages. */
    fun start() {
        if (collectJob?.isActive == true) return

        Timber.d("PatrolAnalysisHandler started")

        collectJob = scope.launch {
            cameraStreamManager.textMessages.collectLatest { message ->
                val state = message.trim().lowercase()
                Timber.d("PatrolAnalysisHandler received message: %s", state)
                latestState = state
                if (state == LYING_STATE) {
                    handleLyingDetected()
                }
            }
        }
    }

    private fun handleLyingDetected() {
        val now = System.currentTimeMillis()

        if (!isObservingLying && now < ignoreNewLyingUntilMs) {
            Timber.d("Lying state ignored due to cooldown.")
            return
        }

        if (finalAlarmTriggered) {
            Timber.d("Lying state ignored, final alarm already in progress.")
            return
        }

        if (!isObservingLying) {
            isObservingLying = true
            lyingCountAfterFirstDetection = 0

            Timber.w("Lying state detected for the first time. Stopping patrol and starting observation.")

            onEmergencyDetected()
            speak("ALARM")

            startObservationTimer()
            return
        }

        lyingCountAfterFirstDetection++

        Timber.w(
            "Lying state confirmed again: %d/%d",
            lyingCountAfterFirstDetection,
            REQUIRED_LYING_CONFIRMATIONS
        )

        if (lyingCountAfterFirstDetection >= REQUIRED_LYING_CONFIRMATIONS) {
            triggerFinalAlarm()
        }
    }

    private fun startObservationTimer() {
        observationJob?.cancel()
        observationJob = scope.launch {
            delay(OBSERVATION_TIME_MS.milliseconds)

            if (isObservingLying && lyingCountAfterFirstDetection >= REQUIRED_LYING_CONFIRMATIONS) {
                triggerFinalAlarm()
            } else {
                Timber.d("Observation time elapsed, but lying state was not confirmed sufficiently.")
            }

            resetLyingObservation()
            onObservationFinished()
        }
    }

    private fun triggerFinalAlarm() {
        if (finalAlarmTriggered) return
        finalAlarmTriggered = true

        Timber.w("Lying state confirmed. Triggering final alarm.")

        observationJob?.cancel()

        scope.launch {
            speak("ALARM, unauthorized worker detected, ALARM!")
            delay(FINAL_ALARM_DELAY_MS.milliseconds)

            resetLyingObservation()
            onObservationFinished()
        }
    }

    private fun resetLyingObservation() {
        isObservingLying = false
        lyingCountAfterFirstDetection = 0
        observationJob?.cancel()
        observationJob = null
        ignoreNewLyingUntilMs = System.currentTimeMillis() + LYING_COOLDOWN_MS
        finalAlarmTriggered = false
    }

    private fun speak(text: String) {
        robot?.speak(
            TtsRequest.create(
                speech = text,
                isShowOnConversationLayer = false
            )
        )
    }

    /** Stops collection and resets all internal observation states. */
    fun stop() {
        collectJob?.cancel()
        collectJob = null
        observationJob?.cancel()
        observationJob = null
        latestState = null
        isObservingLying = false
        lyingCountAfterFirstDetection = 0
        finalAlarmTriggered = false
    }

    private companion object {
        private const val LYING_STATE = "lying"
        private const val OBSERVATION_TIME_MS = 10_000L
        private const val REQUIRED_LYING_CONFIRMATIONS = 3
        private const val FINAL_ALARM_DELAY_MS = 1_500L
        private const val LYING_COOLDOWN_MS = 20_000L
    }
}
