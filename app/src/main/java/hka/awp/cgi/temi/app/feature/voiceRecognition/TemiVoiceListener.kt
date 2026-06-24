package hka.awp.cgi.temi.app.feature.voiceRecognition

import android.media.ToneGenerator
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.voice.WakeupOrigin
import hka.awp.cgi.temi.app.feature.settings.adminPanel.SpeakerVector
import hka.awp.cgi.temi.app.feature.webserver.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import kotlin.time.Duration.Companion.milliseconds

class TemiVoiceListener(
    private val voiceManager: TemiVoiceManager,
    private val robot: Robot?,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val appConfigRepository: AppConfigRepository,
) : RecognitionListener, Robot.WakeupWordListener {

    enum class EnrollmentStatus {
        IDLE, TOO_SHORT, NO_VECTOR, SUCCESS
    }

    companion object {
        private const val SAMPLE_RATE = 16000.0f
        private val VOSK_WAKE_WORDS = setOf("okay assistent", "ok assistent", "okay assistiert")

        // NOTE: Vosk `spk_frames` are speaker-feature frames (~100 frames/second)
        // 300 frames ~= 3 seconds of valid voiced speech.
        private const val SPK_FRAMES_PER_SECOND = 100.0f
        private const val MIN_ENROLLMENT_FRAMES = 300

        private const val TEMI_ASR_GATE_WINDOW_MS = 10_000L
        private const val VOSK_COMMAND_WINDOW_MS = 10_000L
    }

    private var speechService: SpeechService? = null

    private val _isEnrollmentActive = MutableStateFlow(value = false)
    val isEnrollmentActive: StateFlow<Boolean> = _isEnrollmentActive.asStateFlow()
    private val _enrollmentStatus = MutableStateFlow(EnrollmentStatus.IDLE)

    private val _isSpeakerGateOpen = MutableStateFlow(value = false)

    private val _verifiedCommandFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val verifiedCommandFlow: SharedFlow<String> = _verifiedCommandFlow.asSharedFlow()

    @Volatile
    private var isAwaitingVoskCommand: Boolean = false

    private var commandCaptureJob: Job? = null

    @Volatile
    private var voiceProfiles: Map<String, SpeakerVector> = emptyMap()

    // Ensures the speaker verification toggle is immediately synchronized between the
    // UI/Main thread and the C++ audio processing thread to avoid security bypass race conditions.
    @Volatile
    private var isSpeakerVerificationEnabled: Boolean = false

    @Volatile
    private var speakerVerificationThreshold: Double = AppConfigRepository.DEFAULT_SPEAKER_VERIFICATION_THRESHOLD

    private var enrollmentName: String = "Default"
    private var lastEnrollmentPartial: String = ""

    @Volatile
    private var isInitialized = false

    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var gateResetJob: Job? = null
    private var enrollmentSaveJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }
    private var listeningCueTone: ToneGenerator? = null

    init {
        listenerScope.launch(Dispatchers.IO) {
            combine(
                voiceProfileRepository.voiceProfiles,
                appConfigRepository.isSpeakerVerificationEnabled,
                appConfigRepository.speakerVerificationThreshold,
            ) { profiles, enabled, threshold ->
                voiceProfiles = profiles
                isSpeakerVerificationEnabled = enabled
                speakerVerificationThreshold = threshold
            }.take(1).collect()

            isInitialized = true
            Timber.d(
                "TemiVoiceListener initialization completed. Profiles: %d, Verification: %b, Threshold: %.2f",
                voiceProfiles.size,
                isSpeakerVerificationEnabled,
                speakerVerificationThreshold
            )

            listenerScope.launch {
                robot?.addWakeupWordListener(this@TemiVoiceListener)
                syncRuntimeState()
            }
        }

        // Keep profiles and verification settings in sync
        listenerScope.launch(Dispatchers.IO) {
            voiceProfileRepository.voiceProfiles.collect {
                voiceProfiles = it
                listenerScope.launch {
                    syncRuntimeState()
                }
            }
        }

        listenerScope.launch(Dispatchers.IO) {
            appConfigRepository.isSpeakerVerificationEnabled.collect { enabled ->
                isSpeakerVerificationEnabled = enabled
                Timber.i(
                    "Speaker verification toggled: %b (profiles=%d, threshold=%.2f).",
                    enabled,
                    voiceProfiles.size,
                    speakerVerificationThreshold
                )
                listenerScope.launch {
                    syncRuntimeState()
                }
            }
        }

        listenerScope.launch(Dispatchers.IO) {
            appConfigRepository.speakerVerificationThreshold.collect { threshold ->
                speakerVerificationThreshold = threshold
                Timber.i("Speaker verification threshold updated: %.2f", threshold)
            }
        }
    }

    private fun shouldUseKioskVoiceMatch(): Boolean {
        return isSpeakerVerificationEnabled && !_isEnrollmentActive.value
    }

    private fun syncRuntimeState() {
        if (!isInitialized) {
            return
        }

        val shouldDisableTemiWakeWord = _isEnrollmentActive.value || shouldUseKioskVoiceMatch()
        setTemiWakeupWordDisabled(shouldDisableTemiWakeWord)

        // In Vosk-first mode we force-close any accidental Temi conversation overlays.
        if (shouldUseKioskVoiceMatch()) {
            robot?.finishConversation()
        }

        when {
            _isEnrollmentActive.value -> startListening()
            shouldUseKioskVoiceMatch() -> startListening()
            else -> stopListening()
        }
    }

    fun startListening() {
        if (!isInitialized) {
            Timber.w("TemiVoiceListener not initialized yet. Skipping startListening().")
            return
        }

        val isEnrollmentMode = _isEnrollmentActive.value
        val isKioskVoiceMatchMode = shouldUseKioskVoiceMatch()
        if (!isEnrollmentMode && !isKioskVoiceMatchMode) {
            Timber.i("Skipping Vosk SpeechService start outside enrollment/kiosk mode.")
            return
        }

        if (speechService != null) {
            Timber.d("SpeechService already running.")
            return
        }

        val model = voiceManager.model
        val spkModel = voiceManager.speakerModel

        if ((model == null) || (spkModel == null)) {
            Timber.w("Model or speaker model not loaded yet.")
            return
        }

        try {
            val recognizer = Recognizer(model, SAMPLE_RATE, spkModel)
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(this)

            Timber.i(
                "🎤 SpeechService started. mode=%s, profiles=%d, verification=%b",
                if (isEnrollmentMode) "enrollment" else "kiosk-voice-match",
                voiceProfiles.size,
                isSpeakerVerificationEnabled
            )
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            Timber.e(e, "Error starting SpeechService")
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
        Timber.v("Microphone closed.")
    }

    fun release() {
        stopListening()
        robot?.removeWakeupWordListener(this)
        setTemiWakeupWordDisabled(disabled = false)
        gateResetJob?.cancel()
        commandCaptureJob?.cancel()
        enrollmentSaveJob?.cancel()
        closeSpeakerGate()
        isAwaitingVoskCommand = false
        listeningCueTone?.release()
        listeningCueTone = null
        listenerScope.cancel()
        Timber.d("TemiVoiceListener released and cleaned up.")
    }

    fun setEnrollmentMode(active: Boolean, name: String? = null) {
        if (!active) {
            enrollmentSaveJob?.cancel()
            enrollmentSaveJob = null
            lastEnrollmentPartial = ""
            _isEnrollmentActive.value = false
            stopListening()
            Timber.i("Enrollment Mode stopped for '$enrollmentName'")
            syncRuntimeState()
            return
        }

        _isEnrollmentActive.value = true
        val normalizedName = name?.trim().orEmpty()
        if (normalizedName.isNotEmpty()) enrollmentName = normalizedName
        lastEnrollmentPartial = ""
        _enrollmentStatus.value = EnrollmentStatus.IDLE
        syncRuntimeState()

        Timber.i(
            "🎓 Enrollment Mode STARTED for '%s'. speechService=%s",
            enrollmentName,
            (speechService != null)
        )

        if (speechService == null) {
            Timber.w("⚠️ Enrollment active but SpeechService is null. Force-starting...")
            startListening()
        }
    }

    fun clearEnrollmentStatus() {
        _enrollmentStatus.value = EnrollmentStatus.IDLE
    }

    override fun onWakeupWord(wakeupWord: String, direction: Int, origin: WakeupOrigin) {
        Timber.i("🤖 Temi SDK Wake-Word detected: '%s' (Origin: %s, Direction: %d)", wakeupWord, origin.name, direction)

        if (_isEnrollmentActive.value) {
            Timber.w("Temi SDK Wake-Word ignored during enrollment.")
            return
        }

        val isScenarioA = !isSpeakerVerificationEnabled
        if (isScenarioA) {
            Timber.d(
                "Scenario A: Gate opened (verification=%b, profiles=%d).",
                isSpeakerVerificationEnabled,
                voiceProfiles.size
            )
            openSpeakerGateWithTimeout()
            return
        }

        Timber.w("Scenario B: Temi SDK Wake-Word ignoriert. Vosk-first aktiv.")
        robot?.finishConversation()
    }

    // --- Vosk Audio Callbacks (ENROLLMENT ONLY) ---

    override fun onPartialResult(hypothesis: String) {
        val partialResult = runCatching { json.decodeFromString<VoskPartialResult>(hypothesis) }.getOrNull()
        val partialText = partialResult?.partial?.lowercase().orEmpty()

        if (_isEnrollmentActive.value && (partialText.isNotBlank()) && (partialText != lastEnrollmentPartial)) {
            lastEnrollmentPartial = partialText
            Timber.d("Enrollment partial detected: '%s'", partialText)
        }

        if (shouldUseKioskVoiceMatch() && VOSK_WAKE_WORDS.any { partialText.contains(it) }) {
            Timber.v("Kiosk wake-word candidate detected in Vosk partial: '%s'", partialText)
        }
    }

    override fun onResult(hypothesis: String) {
        val result = parseFinalResult(hypothesis)
        val text = result?.text?.lowercase().orEmpty()
        val vector = result?.spk
        val frames = result?.spkFrames?.toInt() ?: 0

        if (text.isNotBlank()) {
            Timber.v("Vosk Background Result: '%s' (Frames: %d, HasVector: %b)", text, frames, vector != null)
        }

        if (result == null) {
            return
        }

        if (_isEnrollmentActive.value) {
            handleEnrollmentResult(text, vector, frames)
        } else {
            handleKioskVoiceMatchResult(text, vector)
        }
    }

    private fun handleKioskVoiceMatchResult(text: String, vector: SpeakerVector?) {
        if (!shouldUseKioskVoiceMatch()) return
        if (captureVoskCommandIfAwaiting(text)) return
        handleKioskWakeWordPhase(text, vector)
    }

    private fun captureVoskCommandIfAwaiting(text: String): Boolean {
        if (!isAwaitingVoskCommand) return false
        if (text.isNotBlank()) {
            commandCaptureJob?.cancel()
            commandCaptureJob = null
            isAwaitingVoskCommand = false
            Timber.i("🎙️ Vosk command captured: '%s'. Emitting to MQTT flow.", text)
            listenerScope.launch { _verifiedCommandFlow.emit(text) }
        }
        return true
    }

    private fun handleKioskWakeWordPhase(text: String, vector: SpeakerVector?) {
        if (!isVoskWakeWordMatch(text)) return
        if (vector == null) {
            Timber.w("Kiosk voice-match rejected: wake-word matched but no speaker vector available.")
            return
        }
        if (!isAnySpeakerAuthorized(vector)) {
            Timber.w("Kiosk voice-match rejected: speaker not authorized.")
            robot?.speak(
                TtsRequest.create(
                    speech = "Entschuldigung, aber dir darf ich leider nicht antworten.",
                    isShowOnConversationLayer = false
                )
            )
            return
        }
        startVoskCommandCapture(text)
    }

    private fun startVoskCommandCapture(matchedText: String) {
        if (isAwaitingVoskCommand) {
            Timber.w("Already awaiting Vosk command, ignoring duplicate wake-word.")
            return
        }
        Timber.v("✅ Speaker verified for '%s'. Awaiting Vosk command input...", matchedText)
        robot?.speak(TtsRequest.create(speech = "Ich höre jetzt zu!", isShowOnConversationLayer = false))
        isAwaitingVoskCommand = true

        commandCaptureJob?.cancel()
        commandCaptureJob = listenerScope.launch {
            delay(VOSK_COMMAND_WINDOW_MS.milliseconds)
            if (isAwaitingVoskCommand) {
                Timber.w(
                    "⏱️ Vosk command capture timeout after %d ms. No command received.",
                    VOSK_COMMAND_WINDOW_MS
                )
                isAwaitingVoskCommand = false
                commandCaptureJob = null
            }
        }
    }

    private fun isVoskWakeWordMatch(text: String): Boolean {
        // Keep Vosk trigger strict to avoid accidental activations in verification mode.
        return VOSK_WAKE_WORDS.any { wakeWord -> text.contains(wakeWord) }
    }

    private fun isAnySpeakerAuthorized(currentVector: SpeakerVector): Boolean {
        val threshold = speakerVerificationThreshold

        if (voiceProfiles.isEmpty()) {
            Timber.w("No voice profiles available. Accepting speaker.")
            return true
        }

        var bestName: String? = null
        var bestScore = Double.NEGATIVE_INFINITY

        for ((name, reference) in voiceProfiles) {
            val similarity = currentVector cosineSimilarityWith reference
            Timber.i(
                "Voice match check: profile='%s', similarity=%.4f, threshold=%.2f",
                name,
                similarity,
                threshold
            )
            if (similarity > bestScore) {
                bestScore = similarity
                bestName = name
            }
        }

        val isAuthorized = bestScore >= threshold
        if (isAuthorized) {
            Timber.i(
                "Voice match accepted: profile='%s', similarity=%.4f",
                bestName,
                bestScore
            )
        } else {
            Timber.w(
                "Voice match rejected: bestProfile='%s', similarity=%.4f, threshold=%.2f",
                bestName,
                bestScore,
                threshold
            )
        }
        return isAuthorized
    }

    private fun handleEnrollmentResult(text: String, vector: SpeakerVector?, frames: Int) {
        val hasVector = vector != null
        if (vector == null) {
            _enrollmentStatus.value = EnrollmentStatus.NO_VECTOR
            Timber.w("Enrollment failed: No speaker vector in result.")
        } else {
            if (frames >= MIN_ENROLLMENT_FRAMES) {
                Timber.i("Enrollment successful! $frames frames collected.")
                saveReferenceVector(enrollmentName, vector)
            } else {
                _enrollmentStatus.value = EnrollmentStatus.TOO_SHORT
                Timber.w(
                    "⏱️ Enrollment too short: %d/%d frames (~%.2f seconds of valid speech). %s %s",
                    frames,
                    MIN_ENROLLMENT_FRAMES,
                    frames / SPK_FRAMES_PER_SECOND,
                    "Possible reasons: too quiet, unclear, noise or pauses.",
                    "Try: louder, clearer, avoid silence."
                )
            }
        }
        logEnrollmentSummary(text, frames, hasVector)
    }

    override fun onFinalResult(hypothesis: String) {
        val result = runCatching { json.decodeFromString<VoskFinalResult>(hypothesis) }.getOrNull()
        Timber.d("Final result: %s", result?.text)
    }

    override fun onError(e: Exception) {
        Timber.e(e, "❌ Error in audio recognition: %s", e.message)
    }

    override fun onTimeout() {
        if (_isEnrollmentActive.value) {
            Timber.w("⏱️ Vosk enrollment timeout. Stopping microphone.")
            stopListening()
            return
        }

        if (shouldUseKioskVoiceMatch()) {
            Timber.w("⏱️ Vosk kiosk voice-match timeout. Restarting microphone.")
            stopListening()
            startListening()
        }
    }

    private fun saveReferenceVector(name: String, vector: SpeakerVector) {
        enrollmentSaveJob?.cancel()
        enrollmentSaveJob = listenerScope.launch(Dispatchers.IO) {
            if (!_isEnrollmentActive.value) {
                Timber.i("Enrollment stopped. Saving cancelled.")
                return@launch
            }
            voiceProfileRepository.saveVoiceProfile(name, vector.values)
            if (!_isEnrollmentActive.value) {
                Timber.i("Enrollment stopped during save. Result discarded.")
                return@launch
            }
            _enrollmentStatus.value = EnrollmentStatus.SUCCESS
            _isEnrollmentActive.value = false
            enrollmentSaveJob = null
            Timber.i("Voice Profile '$name' successfully saved!")
            stopListening()
            syncRuntimeState()
        }
    }

    private fun parseFinalResult(hypothesis: String): VoskFinalResult? {
        return runCatching {
            json.decodeFromString<VoskFinalResult>(hypothesis)
        }.onFailure { e ->
            if (_isEnrollmentActive.value) {
                Timber.e("❌ JSON Parse error: %s", e.message)
            }
        }.getOrNull()
    }

    private fun logEnrollmentSummary(text: String, frames: Int, hasVector: Boolean) {
        Timber.d("Enrollment final text='%s', frames=%d, vector=%s", text, frames, hasVector)
        Timber.i(
            "📊 spk_frames=%d (~%.2fs). Das ist NUR valide Sprache. Text: '%s'",
            frames,
            frames / SPK_FRAMES_PER_SECOND,
            text
        )
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
        if (!isSpeakerVerificationEnabled) return true
        return consumeSpeakerGate()
    }

    fun resumeWakeWordListening() {
        if (shouldUseKioskVoiceMatch()) {
            startListening()
            return
        }
    }

    private fun openSpeakerGateWithTimeout() {
        _isSpeakerGateOpen.value = true
        Timber.v("Temi ASR gate opened. Waiting for SDK onAsrResult up to %d ms.", TEMI_ASR_GATE_WINDOW_MS)
        gateResetJob?.cancel()
        gateResetJob = listenerScope.launch {
            delay(TEMI_ASR_GATE_WINDOW_MS.milliseconds)
            if (_isSpeakerGateOpen.value) {
                Timber.d("Temi ASR Gate timeout reached. Kein SDK onAsrResult angekommen. Closing gate.")
                closeSpeakerGate()
                if (shouldUseKioskVoiceMatch()) {
                    Timber.v("Resuming kiosk voice-match listening after Temi ASR timeout.")
                    startListening()
                }
            }
        }
    }

    private fun closeSpeakerGate() {
        _isSpeakerGateOpen.value = false
    }

    private fun setTemiWakeupWordDisabled(disabled: Boolean) {
        try {
            robot?.toggleWakeup(disabled = disabled)
            Timber.d("Temi built-in wake-word disabled=%b", disabled)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error setting Temi wake-word status")
        }
    }
}
