package hka.awp.cgi.temi.app.feature.voiceRecognition

import com.robotemi.sdk.Robot
import com.robotemi.sdk.voice.WakeupOrigin
import hka.awp.cgi.temi.app.data.repository.GeneralConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import timber.log.Timber
import java.io.IOException

/**
 * Listener class that integrates Temi's voice SDK with Vosk offline recognition.
 * Handles wake-word detection, speaker verification, and enrollment.
 */
class TemiVoiceListener(
    private val voiceManager: TemiVoiceManager,
    private val robot: Robot?,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val generalConfigRepository: GeneralConfigRepository,
) {

    companion object {
        /** The sample rate required by the Vosk model. */
        private const val SAMPLE_RATE = 16000.0f
    }

    private var speechService: SpeechService? = null
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _verifiedCommandFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Flow of voice commands that have been verified to come from a known speaker. */
    val verifiedCommandFlow: SharedFlow<String> = _verifiedCommandFlow.asSharedFlow()

    private val gate = SpeakerGate(listenerScope)
    private val enrollment = VoiceEnrollmentManager(listenerScope, voiceProfileRepository) { syncRuntimeState() }
    private val verification = SpeakerVerificationManager(listenerScope, robot, _verifiedCommandFlow)

    /** State flow indicating if the listener is currently in enrollment mode. */
    val isEnrollmentActive: StateFlow<Boolean> = enrollment.isActive

    @Volatile
    private var voiceProfiles: Map<String, SpeakerVector> = emptyMap()

    @Volatile
    private var isSpeakerVerificationEnabled: Boolean = false

    @Volatile
    private var speakerVerificationThreshold: Double = GeneralConfigRepository.DEFAULT_SPEAKER_VERIFICATION_THRESHOLD

    @Volatile
    private var isInitialized = false

    private val json = Json { ignoreUnknownKeys = true }
    private val internalListener = InternalListener()

    init {
        setupStateFlows()
    }

    private fun setupStateFlows() {
        listenerScope.launch(Dispatchers.IO) {
            combine(
                voiceProfileRepository.voiceProfiles,
                generalConfigRepository.isSpeakerVerificationEnabled,
                generalConfigRepository.speakerVerificationThreshold,
            ) { profiles, enabled, threshold ->
                voiceProfiles = profiles
                isSpeakerVerificationEnabled = enabled
                speakerVerificationThreshold = threshold
            }.first()

            isInitialized = true
            Timber.d("TemiVoiceListener ready. Profiles: %d", voiceProfiles.size)

            listenerScope.launch {
                robot?.addWakeupWordListener(internalListener)
                syncRuntimeState()
            }
        }

        listenerScope.launch(Dispatchers.IO) {
            voiceProfileRepository.voiceProfiles.collect { voiceProfiles = it }
        }
        listenerScope.launch(Dispatchers.IO) {
            generalConfigRepository.isSpeakerVerificationEnabled.collect { enabled ->
                isSpeakerVerificationEnabled = enabled
                listenerScope.launch { syncRuntimeState() }
            }
        }
        listenerScope.launch(Dispatchers.IO) {
            generalConfigRepository.speakerVerificationThreshold.collect { speakerVerificationThreshold = it }
        }
    }

    private fun shouldUseVoskOnly(): Boolean {
        return isSpeakerVerificationEnabled && !enrollment.isActive.value
    }

    private fun syncRuntimeState() {
        if (!isInitialized) return

        val shouldDisableTemiWakeWord = enrollment.isActive.value || shouldUseVoskOnly()
        setTemiWakeupWordDisabled(shouldDisableTemiWakeWord)

        if (shouldUseVoskOnly()) {
            robot?.finishConversation()
        }

        if (enrollment.isActive.value || shouldUseVoskOnly()) {
            startListening()
        } else {
            stopListening()
        }
    }

    /**
     * Starts the Vosk offline recognition service.
     */
    fun startListening() {
        if (!isInitialized || speechService != null) return

        val model = voiceManager.model ?: return
        val spkModel = voiceManager.speakerModel ?: return

        try {
            val recognizer = Recognizer(model, SAMPLE_RATE, spkModel)
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(internalListener)
            Timber.i("Vosk listening started")
        } catch (e: IOException) {
            Timber.e(e, "Vosk start failed")
        }
    }

    /**
     * Stops the Vosk offline recognition service.
     */
    fun stopListening() {
        speechService?.stop()
        speechService = null
        Timber.v("Vosk stopped")
    }

    /**
     * Releases all resources and listeners.
     */
    fun release() {
        stopListening()
        robot?.removeWakeupWordListener(internalListener)
        setTemiWakeupWordDisabled(false)
        gate.release()
        enrollment.release()
        verification.release()
        listenerScope.cancel()
    }

    /**
     * Enables or disables enrollment mode for a speaker.
     */
    fun setEnrollmentMode(active: Boolean, name: String? = null) {
        if (active) enrollment.start(name) else enrollment.stop()
    }

    /**
     * Checks if a Temi ASR result should be allowed through based on speaker verification.
     */
    fun allowTemiAsrResult(): Boolean {
        if (!isSpeakerVerificationEnabled) return true
        return gate.consume()
    }

    /**
     * Resumes listening for the wake-word.
     */
    fun resumeWakeWordListening() {
        if (shouldUseVoskOnly()) startListening()
    }

    private fun setTemiWakeupWordDisabled(disabled: Boolean) {
        try {
            robot?.toggleWakeup(disabled)
            Timber.v("Temi wake-word disabled=%b", disabled)
        } catch (e: IllegalStateException) {
            Timber.e(e, "Robot not available")
        }
    }

    private inner class InternalListener : RecognitionListener, Robot.WakeupWordListener {
        override fun onWakeupWord(wakeupWord: String, direction: Int, origin: WakeupOrigin) {
            Timber.v("Wake-Word: '%s'", wakeupWord)
            if (enrollment.isActive.value) return
            if (!isSpeakerVerificationEnabled) {
                gate.open { if (shouldUseVoskOnly()) startListening() }
            } else {
                robot?.finishConversation()
            }
        }

        override fun onPartialResult(hypothesis: String) {
            val res = runCatching { json.decodeFromString<VoskPartialResult>(hypothesis) }.getOrNull() ?: return
            if (shouldUseVoskOnly()) {
                verification.handlePartialResult(
                    res.partial.lowercase(),
                    res.spk,
                    voiceProfiles,
                    speakerVerificationThreshold
                )
            }
        }

        override fun onResult(hypothesis: String) {
            val res = runCatching { json.decodeFromString<VoskFinalResult>(hypothesis) }.getOrNull() ?: return
            if (enrollment.isActive.value) {
                enrollment.handleResult(res.spk, res.spkFrames.toInt())
            } else if (shouldUseVoskOnly()) {
                verification.handleResult(
                    res.text.lowercase(),
                    res.spk,
                    voiceProfiles,
                    speakerVerificationThreshold
                )
            }
        }

        override fun onFinalResult(hypothesis: String) = Unit

        override fun onError(e: Exception) = Timber.e(e, "Vosk error")

        override fun onTimeout() {
            if (shouldUseVoskOnly()) {
                stopListening()
                startListening()
            }
        }
    }
}
