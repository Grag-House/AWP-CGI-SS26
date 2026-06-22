package hka.awp.cgi.temi.app.feature.voiceRecognition

import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.feature.settings.adminPanel.SpeakerVector
import hka.awp.cgi.temi.app.feature.webserver.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
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

    enum class EnrollmentStatus {
        IDLE,
        TOO_SHORT,
        NO_VECTOR,
        SUCCESS
    }

    companion object {
        private const val SAMPLE_RATE = 16000.0f
        private const val WAKE_WORD = "hey temi"

        // NOTE: Vosk `spk_frames` are speaker-feature frames (~100 frames/second),
        // not raw 16kHz PCM samples. 300 frames ~= 3 seconds of valid voiced speech.
        private const val SPK_FRAMES_PER_SECOND = 100.0f
        private const val MIN_ENROLLMENT_FRAMES = 300

        // Threshold for speaker verification (Cosine Similarity)
        private const val SIMILARITY_THRESHOLD = 0.82
        private const val TEMI_ASR_GATE_WINDOW_MS = 10_000L
    }

    private var speechService: SpeechService? = null

    private val _isEnrollmentActive = MutableStateFlow(false)
    val isEnrollmentActive: StateFlow<Boolean> = _isEnrollmentActive.asStateFlow()
    private val _enrollmentStatus = MutableStateFlow(EnrollmentStatus.IDLE)
    val enrollmentStatus: StateFlow<EnrollmentStatus> = _enrollmentStatus.asStateFlow()

    private val _isSpeakerGateOpen = MutableStateFlow(false)

    // Cached voice profiles and verification setting updated by collectors below
    private var voiceProfiles: Map<String, SpeakerVector> = emptyMap()
    private var isSpeakerVerificationEnabled: Boolean = false
    private var enrollmentName: String = "Default"
    private var lastEnrollmentPartial: String = ""

    // Flag to track when initial data load is complete (prevents race conditions)
    private var isInitialized = false

    // Single lifecycle-bound scope for all listener coroutines.
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var gateResetJob: Job? = null
    private var enrollmentSaveJob: Job? = null

    init {
        // Wait for first combined value from both repos
        // This ensures voiceProfiles and isSpeakerVerificationEnabled have real data before
        // startListening() is called. Without this, startListening() could use stale/empty values.
        listenerScope.launch(Dispatchers.IO) {
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
            // Retry listening once initialization data is ready and feature is enabled.
            if (isSpeakerVerificationEnabled) {
                Timber.i("🔄 Init complete. Starting listening because Verification=true")
                startListening()
            } else {
                Timber.i("⏸️ Init complete. Verification=false. Waiting for manual start or toggle.")
            }
        }

        // Keep profiles and verification settings in sync
        // These collectors run indefinitely to handle updates after initialization
        listenerScope.launch(Dispatchers.IO) {
            voiceProfileRepository.voiceProfiles.collect {
                voiceProfiles = it
            }
        }

        listenerScope.launch(Dispatchers.IO) {
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

        if (speechService != null) {
            Timber.d("SpeechService läuft bereits.")
            return
        }

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
            Timber.i(
                "🎤 SpeechService gestartet. Enrollment=%s, Verification=%s",
                _isEnrollmentActive.value,
                isSpeakerVerificationEnabled
            )

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
        // Cancels all listener jobs (collectors, timers, pending async writes).
        stopListening()
        gateResetJob?.cancel()
        enrollmentSaveJob?.cancel()
        closeSpeakerGate()
        listenerScope.cancel()
        Timber.d("TemiVoiceListener released and cleaned up.")
    }

    fun setEnrollmentMode(active: Boolean, name: String? = null) {
        if (!active) {
            enrollmentSaveJob?.cancel()
            enrollmentSaveJob = null
        }
        _isEnrollmentActive.value = active
        if (active) {
            val normalizedName = name?.trim().orEmpty()
            if (normalizedName.isNotEmpty()) {
                enrollmentName = normalizedName
            }
            lastEnrollmentPartial = ""
            _enrollmentStatus.value = EnrollmentStatus.IDLE
            Timber.i(
                "🎓 Enrollment Mode STARTED für '%s'. speechService=%s, Listening=%b",
                enrollmentName,
                (speechService != null),
                isSpeakerVerificationEnabled
            )
            // Enrollment braucht Audio, unabhängig von isSpeakerVerificationEnabled.
            // Starte Listening falls nicht schon läuft.
            if (speechService == null) {
                Timber.w("⚠️ Enrollment aktiv aber SpeechService ist null. Force-Start...")
                startListening()
            }
        } else {
            lastEnrollmentPartial = ""
            Timber.i("Enrollment Mode stopped for '$enrollmentName'")
        }
    }

    fun clearEnrollmentStatus() {
        _enrollmentStatus.value = EnrollmentStatus.IDLE
    }

    // --- Vosk Audio Callbacks ---
    // These callbacks are invoked during real-time audio processing.
    // They run on background threads, so we use cached variables (voiceProfiles, isSpeakerVerificationEnabled)
    // instead of Flow.collect() which would be slower.

    override fun onPartialResult(hypothesis: String) {
        // Partial results are fired during speech - used for fast wake-word detection
        val partialResult =
            runCatching { VoskJsonParser.json.decodeFromString<VoskPartialResult>(hypothesis) }.getOrNull()

        if (_isEnrollmentActive.value && !partialResult?.partial.isNullOrBlank()) {
            val partial = partialResult.partial.trim()
            if (partial != lastEnrollmentPartial) {
                lastEnrollmentPartial = partial
                Timber.d("Enrollment partial erkannt: '%s'", partial)
            }
        }

        if (!isSpeakerVerificationEnabled && partialResult?.partial?.lowercase()?.contains(WAKE_WORD) == true) {
            Timber.i("Wake Word erkannt (Verification aus) via Partial Result.")
            handleWakeWordDetected()
        }
    }

    override fun onResult(hypothesis: String) {
        // Final result from Vosk after speech processing complete
        val result = parseFinalResult(hypothesis)

        if (result == null && _isEnrollmentActive.value) {
            return
        }
        val text = result?.text
        val vector = result?.spk

        val frames = result?.spkFrames?.toInt() ?: 0

        // ENROLLMENT: Vosk provides enrollment quality via spk_frames for this utterance.
        if (_isEnrollmentActive.value) {
            if (vector == null) {
                _enrollmentStatus.value = EnrollmentStatus.NO_VECTOR
                Timber.w("Enrollment fehlgeschlagen: Kein Speaker-Vector im Ergebnis.")
            } else {
                logEnrollmentSummary(text.orEmpty(), frames, true)
                if (frames >= MIN_ENROLLMENT_FRAMES) {
                    Timber.i("Enrollment erfolgreich! $frames Frames gesammelt.")
                    saveReferenceVector(enrollmentName, vector)
                } else {
                    _enrollmentStatus.value = EnrollmentStatus.TOO_SHORT
                    Timber.w(
                        "⏱️ Enrollment zu kurz: %d/%d frames (~%.2f Sekunden gültige Sprache). " +
                            "Mögliche Gründe: zu leise, undeutlich, Rauschen oder Pausen. " +
                            "Versuche: lauter, deutlicher, Stille vermeiden.",
                        frames,
                        MIN_ENROLLMENT_FRAMES,
                        frames / SPK_FRAMES_PER_SECOND
                    )
                }
            }
            if (vector == null) {
                logEnrollmentSummary(text.orEmpty(), frames, false)
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
        val result = runCatching { VoskJsonParser.json.decodeFromString<VoskFinalResult>(hypothesis) }.getOrNull()
        Timber.d("Endgültiges Ergebnis: ${result?.text}")
    }

    override fun onError(e: Exception) {
        Timber.e(e, "❌ Fehler bei der Audio-Erkennung: %s", e.message)
    }

    override fun onTimeout() {
        Timber.w("⏱️ Audio-Erkennung hat ein Timeout erreicht. Starte neu...")
        stopListening()
        if (isSpeakerVerificationEnabled) {
            startListening()
        }
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
        enrollmentSaveJob?.cancel()
        enrollmentSaveJob = listenerScope.launch(Dispatchers.IO) {
            if (!_isEnrollmentActive.value) {
                Timber.i("Enrollment gestoppt. Speichern abgebrochen.")
                return@launch
            }
            voiceProfileRepository.saveVoiceProfile(name, vector.values)
            if (!_isEnrollmentActive.value) {
                Timber.i("Enrollment während Speichern gestoppt. Ergebnis wird verworfen.")
                return@launch
            }
            _enrollmentStatus.value = EnrollmentStatus.SUCCESS
            _isEnrollmentActive.value = false
            enrollmentSaveJob = null
            Timber.i("Voice Profile '$name' erfolgreich gespeichert!")
        }
    }

    private fun parseFinalResult(hypothesis: String): VoskFinalResult? {
        return runCatching {
            VoskJsonParser.json.decodeFromString<VoskFinalResult>(hypothesis)
        }.onFailure { e ->
            if (_isEnrollmentActive.value) {
                Timber.w("❌ JSON Parse error: %s", e.message)
            }
        }.getOrNull()
    }

    private fun logEnrollmentSummary(text: String, frames: Int, hasVector: Boolean) {
        Timber.d(
            "Enrollment final text='%s', frames=%d, vector=%s",
            text,
            frames,
            hasVector
        )
        Timber.i(
            "📊 spk_frames=%d (~%.2fs). Das ist NUR valide Sprache. Text: '%s'",
            frames,
            frames / SPK_FRAMES_PER_SECOND,
            text
        )
    }

    private fun handleWakeWordDetected() {
        // Wake word detected and speaker verified - wake up the robot
        // Stop listening to prevent further audio processing
        stopListening()
        openSpeakerGateWithTimeout()
        // Execute on Main thread since robot.wakeup() may update UI
        listenerScope.launch {
            Timber.v("Wecke Temi auf...")
            robot?.wakeup()
        }
    }

    fun consumeSpeakerGate(): Boolean {
        if (!_isSpeakerGateOpen.value) {
            return false
        }
        gateResetJob?.cancel()
        closeSpeakerGate()
        return true
    }

    fun allowTemiAsrResult(): Boolean {
        if (!isSpeakerVerificationEnabled) {
            return true
        }
        return consumeSpeakerGate()
    }

    fun resumeWakeWordListening() {
        if (isSpeakerVerificationEnabled) {
            startListening()
        }
    }

    private fun openSpeakerGateWithTimeout() {
        _isSpeakerGateOpen.value = true
        gateResetJob?.cancel()
        gateResetJob = listenerScope.launch {
            delay(TEMI_ASR_GATE_WINDOW_MS)
            if (_isSpeakerGateOpen.value) {
                Timber.w("Temi ASR Gate Timeout erreicht. Schließe Gate und starte Wake-Listening neu.")
                closeSpeakerGate()
                if (isSpeakerVerificationEnabled) {
                    startListening()
                }
            }
        }
    }

    private fun closeSpeakerGate() {
        _isSpeakerGateOpen.value = false
    }
}
