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
    private val cameraStreamManager: PatrolCameraStreamManager
                           ) {
    private var observationJob: Job? = null
    private var latestState: String? = null

    private var collectJob: Job? = null

    fun start() {
        if (collectJob?.isActive == true) return

        collectJob = scope.launch {
            cameraStreamManager.textMessages.collectLatest { message ->
                val state = message.trim().lowercase()
                latestState = state

                if (state == SITTING_STATE) {
                    handleSittingDetected()
                }
            }
        }
    }

    private fun handleSittingDetected() {
        if (observationJob?.isActive == true) return

        observationJob = scope.launch {
            Timber.w("Sitting erkannt. Beobachte Zustand 10 Sekunden.")

            speakAlarm()

            delay(OBSERVATION_TIME_MS)

            if (latestState == SITTING_STATE) {
                Timber.w("Sitting nach 10 Sekunden weiterhin erkannt. Starte Alarm-Wiederholung.")

                repeat(ALARM_REPEAT_COUNT) {
                    speakAlarm()
                    delay(ALARM_REPEAT_DELAY_MS)
                }
            } else {
                Timber.d("Sitting-Zustand hat sich geändert. Kein weiterer Alarm.")
            }
        }
    }

    private fun speakAlarm() {
        robot?.speak(
            TtsRequest.create(
                speech = "Alarm",
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
        private const val SITTING_STATE = "sitting"
        private const val OBSERVATION_TIME_MS = 10_000L
        private const val ALARM_REPEAT_COUNT = 3
        private const val ALARM_REPEAT_DELAY_MS = 1_500L
    }
}
