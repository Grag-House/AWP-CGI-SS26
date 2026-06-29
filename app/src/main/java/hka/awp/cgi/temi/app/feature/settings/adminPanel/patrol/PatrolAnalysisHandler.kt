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

    fun start() {
        if (collectJob?.isActive == true) return

        Timber.d("PatrolAnalysisHandler gestartet")

        collectJob = scope.launch {
            cameraStreamManager.textMessages.collectLatest { message ->
                val state = message.trim().lowercase()
                Timber.d("PatrolAnalysisHandler Nachricht erhalten: $state")
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
            Timber.d("Lying ignoriert wegen Cooldown.")
            return
        }

        if (finalAlarmTriggered) {
            Timber.d("Lying ignoriert, finaler Alarm läuft bereits.")
            return
        }

        if (!isObservingLying) {
            isObservingLying = true
            lyingCountAfterFirstDetection = 0

            Timber.w("Lying erstmals erkannt. Stoppe Temi und starte Beobachtung.")

            onEmergencyDetected()
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
            onObservationFinished()
        }
    }

    private fun triggerFinalAlarm() {
        if (finalAlarmTriggered) return
        finalAlarmTriggered = true

        Timber.w("Lying bestätigt. Starte finalen Alarm.")

        observationJob?.cancel()

        scope.launch {
            speak("ALARM, Arbeitsverweigerer erkannt, ALARM!")
            delay(FINAL_ALARM_DELAY_MS)

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
