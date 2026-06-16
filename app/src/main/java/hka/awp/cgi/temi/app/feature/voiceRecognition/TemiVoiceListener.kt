package hka.awp.cgi.temi.app.feature.voiceRecognition

import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.feature.webserver.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import timber.log.Timber
import kotlin.math.sqrt

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
        private const val MIN_ENROLLMENT_FRAMES = 20

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

    private var voiceProfiles: Map<String, List<Float>> = emptyMap()
    private var isSpeakerVerificationEnabled: Boolean = false
    private var enrollmentName: String = "Default"

    init {
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

    fun setEnrollmentMode(active: Boolean, name: String = "Default") {
        _isEnrollmentActive.value = active
        enrollmentName = name
        if (active) {
            Timber.i("Enrollment Mode gestartet für '$name'. Bitte sprich jetzt...")
        }
    }

    // --- 2. Die Vosk Callbacks ---

    override fun onPartialResult(hypothesis: String) {
        val partialResult =
            runCatching { jsonParser.decodeFromString<VoskPartialResult>(hypothesis) }.getOrNull()

        if (partialResult?.partial?.lowercase()?.contains(WAKE_WORD) == true) {
            val vector = partialResult.spk
            if (vector != null) {
                if (!isSpeakerVerificationEnabled || isAnySpeakerAuthorized(vector)) {
                    Timber.i("Wake Word erkannt und Speaker verifiziert (oder Gatekeeper aus)!")
                    handleWakeWordDetected()
                } else {
                    Timber.w("Wake Word erkannt, aber Speaker NICHT verifiziert.")
                }
            }
        }
    }

    override fun onResult(hypothesis: String) {
        val result = runCatching { jsonParser.decodeFromString<VoskFinalResult>(hypothesis) }.getOrNull()
        val text = result?.text
        val vector = result?.spk

        Timber.d("Vosk onResult: text='$text', spkFrames=${result?.spkFrames}, vectorSize=${vector?.size}")

        // Enrollment Handling
        if (_isEnrollmentActive.value && vector != null) {
            val frames = result.spkFrames
            if (frames > MIN_ENROLLMENT_FRAMES) {
                Timber.i("Enrollment erfolgreich! $frames Frames gesammelt.")
                saveReferenceVector(enrollmentName, vector)
                setEnrollmentMode(false)
            } else {
                Timber.w("Enrollment läuft... erst $frames/$MIN_ENROLLMENT_FRAMES Frames.")
            }
        }

        if (text?.lowercase()?.contains(WAKE_WORD) == true) {
            if (vector != null) {
                if (!isSpeakerVerificationEnabled || isAnySpeakerAuthorized(vector)) {
                    handleWakeWordDetected()
                }
            } else if (!isSpeakerVerificationEnabled) {
                // Fallback if verification is disabled but no vector yet
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

    private fun isAnySpeakerAuthorized(currentVector: List<Float>): Boolean {
        if (voiceProfiles.isEmpty()) {
            Timber.w("Keine Voice Profiles vorhanden. Akzeptiere alle Sprecher.")
            return true
        }

        // Check against all stored profiles
        for ((name, reference) in voiceProfiles) {
            val similarity = calculateCosineSimilarity(currentVector, reference)
            if (similarity >= SIMILARITY_THRESHOLD) {
                Timber.d("Speaker verifiziert als '$name' (Score: $similarity)")
                return true
            }
        }

        Timber.d("Sprecher konnte KEINEM Profil zugeordnet werden.")
        return false
    }

    private fun calculateCosineSimilarity(v1: List<Float>, v2: List<Float>): Double {
        if (v1.size != v2.size) return 0.0

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in v1.indices) {
            dotProduct += (v1[i] * v2[i]).toDouble()
            normA += (v1[i] * v1[i]).toDouble()
            normB += (v2[i] * v2[i]).toDouble()
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator != 0.0) dotProduct / denominator else 0.0
    }

    private fun saveReferenceVector(name: String, vector: List<Float>) {
        CoroutineScope(Dispatchers.IO).launch {
            voiceProfileRepository.saveVoiceProfile(name, vector)
            Timber.i("Voice Profile '$name' erfolgreich gespeichert!")
        }
    }

    private fun handleWakeWordDetected() {
        stopListening()
        CoroutineScope(Dispatchers.Main).launch {
            Timber.v("Wecke Temi auf...")
            robot?.wakeup()
        }
    }
}
