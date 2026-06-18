package hka.awp.cgi.temi.app.feature.voiceRecognition

import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.feature.settings.adminPanel.SpeakerVector
import hka.awp.cgi.temi.app.feature.webserver.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import timber.log.Timber

@Suppress("TooManyFunctions")
class TemiVoiceListener(
    private val voiceManager: TemiVoiceManager,
    private val robot: Robot?,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val appConfigRepository: AppConfigRepository
) : RecognitionListener {

    companion object {
        private const val SAMPLE_RATE = 16000.0f
        private const val WAKE_WORD = "hey temi"

        // Enrollment requires ~3-5 seconds of audio for robust speaker embedding
        // At 16kHz: 3s = 48000 frames, 5s = 80000 frames
        // Using 48000 to match ~3 seconds of continuous speech
        private const val MIN_ENROLLMENT_FRAMES = 48000

        // Threshold for speaker verification (Cosine Similarity)
        private const val SIMILARITY_THRESHOLD = 0.82

        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private var speechService: SpeechService? = null

    private val _isEnrollmentActive = MutableStateFlow(false)
    val isEnrollmentActive: StateFlow<Boolean> = _isEnrollmentActive.asStateFlow()

    // Cached voice profiles and verification setting updated by collectors below
    private var voiceProfiles: Map<String, SpeakerVector> = emptyMap()
    private var isSpeakerVerificationEnabled: Boolean = false
    private var enrollmentName: String = "Default"

    // Flag to track when initial data load is complete (prevents race conditions)
    private var isInitialized = false

    // Job handle to cancel the initialization coroutine when listener is released
    private var initializationJob: Job? = null

    init {
        // Wait for first combined value from both repos
        // This ensures voiceProfiles and isSpeakerVerificationEnabled have real data before
        // startListening() is called. Without this, startListening() could use stale/empty values.
        initializationJob = CoroutineScope(Dispatchers.IO).launch {
            combine(
                voiceProfileRepository.voiceProfiles,
                appConfigRepository.isSpeakerVerificationEnabled
            ) { profiles, enabled ->
                voiceProfiles = profiles
                isSpeakerVerificationEnabled = enabled
            }.take(1) // Collect only the first combined emission
                .collect()

            // Mark as initialized so startListening() knows data is ready
            isInitialized = true
            Timber.d(
                "TemiVoiceListener initialization completed. Profiles: ${voiceProfiles.size}, " +
                    "Verification: $isSpeakerVerificationEnabled"
            )
        }

        // Keep profiles and verification settings in sync
        // These collectors run indefinitely to handle updates after initialization
        CoroutineScope(Dispatchers.IO).launch {
            voiceProfileRepository.voiceProfiles.collect {
                voiceProfiles = it
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            appConfigRepository.isSpeakerVerificationEnabled.collect {
                isSpeakerVerificationEnabled = it
            }
        }
    }

    fun startListening() {
        // Guard: Only start listening after initialization is complete.
        // This prevents using stale/empty voice profiles and verification settings.
        if (!isInitialized) {
            Timber.w("TemiVoiceListener not initialized yet. Skipping startListening().")
            return
        }

        if (speechService != null) return

        val model = voiceManager.model
        val spkModel = voiceManager.speakerModel

        if (model == null || spkModel == null) {
            Timber.w("Modell oder Speaker-Modell noch nicht geladen.")
            return
        }

        try {
            val recognizer = Recognizer(model, SAMPLE_RATE, spkModel)
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(this)

            Timber.d("Mikrofon offen. Verification: $isSpeakerVerificationEnabled. Lausche auf '$WAKE_WORD'...")
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception
        ) {
            Timber.e(e, "Fehler beim Starten des SpeechService")
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
        Timber.v("Mikrofon geschlossen.")
    }

    fun release() {
        // Clean up resources when this listener is no longer needed.
        // Cancels the initialization job to prevent ongoing coroutines holding references.
        stopListening()
        initializationJob?.cancel()
        Timber.d("TemiVoiceListener released and cleaned up.")
    }

    fun setEnrollmentMode(active: Boolean, name: String = "Default") {
        _isEnrollmentActive.value = active
        enrollmentName = name
        if (active) {
            Timber.i("Enrollment Mode started for '$name'. Please speak one full sentence.")
        } else {
            Timber.i("Enrollment Mode stopped for '$name'")
        }
    }

    // --- Vosk Audio Callbacks ---
    // These callbacks are invoked during real-time audio processing.
    // They run on background threads, so we use cached variables (voiceProfiles, isSpeakerVerificationEnabled)
    // instead of Flow.collect() which would be slower.

    override fun onPartialResult(hypothesis: String) {
        // Partial results are fired during speech - used for fast wake-word detection
        val partialResult =
            runCatching { jsonParser.decodeFromString<VoskPartialResult>(hypothesis) }.getOrNull()

        if (partialResult?.partial?.lowercase()?.contains(WAKE_WORD) == true) {
            val vector = partialResult.spk
            if (vector != null) {
                // Early trigger on partial result if: verification disabled OR speaker authorized
                if (!isSpeakerVerificationEnabled || isAnySpeakerAuthorized(vector)) {
                    Timber.i("Wake Word erkannt und Speaker verifiziert (oder Gatekeeper aus)!")
                    handleWakeWordDetected()
                }
            }
        }
    }

    override fun onResult(hypothesis: String) {
        // Final result from Vosk after speech processing complete
        val result = runCatching { jsonParser.decodeFromString<VoskFinalResult>(hypothesis) }.getOrNull()
        val text = result?.text
        val vector = result?.spk

        Timber.d("Vosk onResult: text='$text', spkFrames=${result?.spkFrames}, vectorSize=${vector?.values?.size}")

        // ENROLLMENT: Vosk provides enrollment quality via spk_frames for this utterance.
        if (_isEnrollmentActive.value && vector != null) {
            val frames = result.spkFrames
            if (frames >= MIN_ENROLLMENT_FRAMES) {
                Timber.i("Enrollment erfolgreich! $frames Frames gesammelt.")
                saveReferenceVector(enrollmentName, vector)
                setEnrollmentMode(false)
            } else {
                Timber.w("Enrollment zu kurz: $frames/$MIN_ENROLLMENT_FRAMES Frames. Bitte längeren Satz sprechen.")
            }
        }

        // RECOGNITION: Check if final text contains wake word
        if (text?.lowercase()?.contains(WAKE_WORD) == true) {
            if (vector != null) {
                // Final verification if: verification disabled OR speaker authorized
                if (!isSpeakerVerificationEnabled || isAnySpeakerAuthorized(vector)) {
                    handleWakeWordDetected()
                }
            } else if (!isSpeakerVerificationEnabled) {
                // Fallback: No speaker vector but verification is disabled - allow access anyway
                handleWakeWordDetected()
            }
        }
    }

    override fun onFinalResult(hypothesis: String) {
        val result = runCatching { jsonParser.decodeFromString<VoskFinalResult>(hypothesis) }.getOrNull()
        Timber.d("Endgültiges Ergebnis: ${result?.text}")
    }

    override fun onError(e: Exception) {
        Timber.e(e, "Fehler bei der Audio-Erkennung")
    }

    override fun onTimeout() {
        Timber.v("Audio-Erkennung hat ein Timeout erreicht")
    }

    private fun isAnySpeakerAuthorized(currentVector: SpeakerVector): Boolean {
        if (voiceProfiles.isEmpty()) {
            Timber.w("Keine Voice Profiles vorhanden. Akzeptiere alle Sprecher.")
            return true
        }

        // Check current speaker against all stored profiles
        for ((name, reference) in voiceProfiles) {
            val similarity = currentVector cosineSimilarityWith reference
            if (similarity >= SIMILARITY_THRESHOLD) {
                Timber.d("Speaker verifiziert als '$name' (Score: $similarity)")
                return true
            }
        }

        Timber.d("Sprecher konnte KEINEM Profil zugeordnet werden.")
        return false
    }

    private fun saveReferenceVector(name: String, vector: SpeakerVector) {
        // Persist the enrolled speaker vector to repository asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            voiceProfileRepository.saveVoiceProfile(name, vector.values)
            Timber.i("Voice Profile '$name' erfolgreich gespeichert!")
        }
    }

    private fun handleWakeWordDetected() {
        // Wake word detected and speaker verified - wake up the robot
        // Stop listening to prevent further audio processing
        stopListening()
        // Execute on Main thread since robot.wakeup() may update UI
        CoroutineScope(Dispatchers.Main).launch {
            Timber.v("Wecke Temi auf...")
            robot?.wakeup()
        }
    }
}
