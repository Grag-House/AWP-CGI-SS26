package hka.awp.cgi.temi.app.feature.voiceRecognition

import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import hka.awp.cgi.temi.app.feature.settings.adminPanel.SpeakerVector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles speaker verification, wake-word matching, and command capture in Vosk-only mode.
 */
class SpeakerVerificationManager(
    private val scope: CoroutineScope,
    private val robot: Robot?,
    private val verifiedCommandFlow: MutableSharedFlow<String>
) {
    companion object {
        private val VOSK_WAKE_WORDS = setOf("okay assistent", "ok assistent", "okay assistiert")
        private const val COMMAND_WINDOW_MS = 10_000L
    }

    private var isAwaitingCommand = false
    private var commandCaptureJob: Job? = null

    /**
     * Cache for speaker verification status during a single session to avoid redundant parallel math.
     */
    @Volatile
    private var isCurrentSpeakerVerified = false

    fun handleResult(
        text: String,
        vector: SpeakerVector?,
        profiles: Map<String, SpeakerVector>,
        threshold: Double
    ) {
        if (captureCommandIfAwaiting(text)) return

        if (!isWakeWordMatch(text)) return

        scope.launch {
            processVerification(vector, profiles, threshold, isFinal = true)
        }
    }

    /**
     * Pre-check verification during speech to reduce latency.
     */
    fun handlePartialResult(
        text: String,
        vector: SpeakerVector?,
        profiles: Map<String, SpeakerVector>,
        threshold: Double
    ) {
        if (isAwaitingCommand || isCurrentSpeakerVerified) return
        if (!isWakeWordMatch(text)) return

        scope.launch {
            processVerification(vector, profiles, threshold, isFinal = false)
        }
    }

    private suspend fun processVerification(
        vector: SpeakerVector?,
        profiles: Map<String, SpeakerVector>,
        threshold: Double,
        isFinal: Boolean
    ) {
        if (vector == null) {
            if (isFinal) Timber.w("Wake-word matched but no speaker vector")
            return
        }

        if (isAuthorized(vector, profiles, threshold)) {
            isCurrentSpeakerVerified = true
            startCommandCapture()
        } else if (isFinal) {
            rejectSpeaker()
        }
    }

    private fun isWakeWordMatch(text: String): Boolean {
        return VOSK_WAKE_WORDS.any { text.contains(it) }
    }

    private suspend fun isAuthorized(
        vector: SpeakerVector,
        profiles: Map<String, SpeakerVector>,
        threshold: Double
    ): Boolean = withContext(Dispatchers.Default) {
        if (profiles.isEmpty()) return@withContext true

        // Parallel processing of profiles for speed
        val results = profiles.map { (name, reference) ->
            async {
                val similarity = vector cosineSimilarityWith reference
                name to similarity
            }
        }.awaitAll()

        val (bestName, bestScore) = results.maxByOrNull { it.second }
            ?: (null to Double.NEGATIVE_INFINITY)

        val authorized = bestScore >= threshold
        if (authorized) {
            Timber.i("Verified: %s (score %.4f)", bestName, bestScore)
        } else {
            Timber.w("Rejected: %s (score %.4f, threshold %.2f)", bestName, bestScore, threshold)
        }
        authorized
    }

    private fun startCommandCapture() {
        if (isAwaitingCommand) return

        Timber.i("Speaker verified. Awaiting command...")
        robot?.speak(TtsRequest.create("Ich höre jetzt zu!", false))
        isAwaitingCommand = true

        commandCaptureJob?.cancel()
        commandCaptureJob = scope.launch {
            delay(COMMAND_WINDOW_MS.milliseconds)
            if (isAwaitingCommand) {
                Timber.w("Command timeout")
                isAwaitingCommand = false
                isCurrentSpeakerVerified = false
            }
        }
    }

    private fun captureCommandIfAwaiting(text: String): Boolean {
        if (!isAwaitingCommand) return false
        if (text.isNotBlank()) {
            isAwaitingCommand = false
            isCurrentSpeakerVerified = false
            commandCaptureJob?.cancel()
            Timber.i("Command captured: %s", text)
            scope.launch { verifiedCommandFlow.emit(text) }
            return true
        }
        return false
    }

    private fun rejectSpeaker() {
        robot?.speak(
            TtsRequest.create(
                "Entschuldigung, aber dir darf ich leider nicht antworten.",
                false
            )
        )
    }

    fun release() {
        commandCaptureJob?.cancel()
        isAwaitingCommand = false
        isCurrentSpeakerVerified = false
    }
}
