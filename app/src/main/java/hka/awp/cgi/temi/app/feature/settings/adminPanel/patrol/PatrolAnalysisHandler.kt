package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class PatrolAnalysisHandler(
    private val robot: Robot?,
    private val scope: CoroutineScope,
    private val cameraStreamManager: PatrolCameraStreamManager,
    private val onEmergencyDetected: () -> Unit
) {
    private var observationJob: Job? = null
    private var latestState: String? = null

    private var collectJob: Job? = null

    private var lyingCountAfterFirstDetection = 0
    private var isObservingLying = false

    fun start() {
        if (collectJob?.isActive == true) return

        collectJob = scope.launch {
            cameraStreamManager.textMessages.collectLatest { message ->
                val state = message.trim().lowercase()
                latestState = state
                if (state == LYING_STATE) {
                    handleLyingDetected()
                }
            }
        }
    }

    private fun handleLyingDetected() {
        if (!isObservingLying) {
            isObservingLying = true
            lyingCountAfterFirstDetection = 0

            Timber.w("Lying erstmals erkannt. Stoppe Temi und starte Beobachtung.")

            onEmergencyDetected()
            robot?.stopMovement()
            speak("ALARM")

            startObservationTimer()
            return
        }

        lyingCountAfterFirstDetection++

        Timber.w(
            "Lying weiterhin erkannt: %s/%s",
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
            delay(OBSERVATION_TIME_MS)

            if (isObservingLying && lyingCountAfterFirstDetection >= REQUIRED_LYING_CONFIRMATIONS) {
                triggerFinalAlarm()
            } else {
                Timber.d("Beobachtungszeit vorbei, aber Lying nicht oft genug bestätigt.")
            }

            resetLyingObservation()
        }
    }

    private fun triggerFinalAlarm() {
        if (observationJob?.isActive != true && !isObservingLying) return

        Timber.w("Lying bestätigt. Starte finalen Alarm.")

        observationJob?.cancel()

        scope.launch {
            repeat(FINAL_ALARM_REPEAT_COUNT) {
                speak("ALARM, Arbeitsverweigerer erkannt, ALARM!")
                delay(FINAL_ALARM_DELAY_MS)
            }

            resetLyingObservation()
        }
    }

    private fun resetLyingObservation() {
        isObservingLying = false
        lyingCountAfterFirstDetection = 0
        observationJob?.cancel()
        observationJob = null
    }

    private fun speak(text: String) {
        robot?.speak(
            TtsRequest.create(
                speech = text,
                isShowOnConversationLayer = false
            )
        )
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
        observationJob?.cancel()
        observationJob = null
        latestState = null
    }

    private companion object {
        private const val LYING_STATE = "Lying"
        private const val OBSERVATION_TIME_MS = 10_000L
        private const val REQUIRED_LYING_CONFIRMATIONS = 5
        private const val FINAL_ALARM_REPEAT_COUNT = 3
        private const val FINAL_ALARM_DELAY_MS = 1_500L
    }
}
