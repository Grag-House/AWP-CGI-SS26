package hka.awp.cgi.temi.app.feature.voiceRecognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.data.repository.GeneralConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Temi's voice recognition features.
 *
 * This ViewModel handles model initialization, toggling enrollment mode,
 * and reacting to speaker verification settings.
 *
 * @property voiceManager Manager for loading Vosk voice models.
 * @property temiVoiceListener Listener that handles actual recognition and verification logic.
 * @property generalConfigRepository Repository for accessing application-wide settings.
 */
class TemiVoiceRecognitionViewModel(
    private val voiceManager: TemiVoiceManager,
    private val temiVoiceListener: TemiVoiceListener,
    private val generalConfigRepository: GeneralConfigRepository
) :
    ViewModel() {

    private val _isModelLoaded = MutableStateFlow(false)

    /** State flow indicating if the voice enrollment process is currently active. */
    val isEnrollmentActive = temiVoiceListener.isEnrollmentActive

    init {
        viewModelScope.launch {
            generalConfigRepository.isSpeakerVerificationEnabled.collect { enabled ->
                if (enabled) {
                    initializeVoiceAi()
                } else {
                    stopListening()
                }
            }
        }
    }

    /**
     * Initializes the voice models and starts listening if speaker verification is enabled.
     */
    fun initializeVoiceAi() {
        viewModelScope.launch {
            if (voiceManager.isReady()) {
                _isModelLoaded.value = true
                temiVoiceListener.syncRuntimeState()
                return@launch
            }

            val success = voiceManager.initModel()
            if (success) {
                _isModelLoaded.value = true
                temiVoiceListener.syncRuntimeState()
            }
        }
    }

    /**
     * Stops the voice listener.
     */
    fun stopListening() {
        temiVoiceListener.stopListening()
    }

    /**
     * Toggles the enrollment mode for a specific speaker profile.
     *
     * @param active Whether enrollment should be enabled or disabled.
     * @param name The name of the speaker being enrolled (optional).
     */
    fun toggleEnrollment(active: Boolean, name: String? = null) {
        if (!active) {
            temiVoiceListener.setEnrollmentMode(false, name)
            return
        }

        viewModelScope.launch {
            if (!voiceManager.isReady()) {
                val success = voiceManager.initModel()
                if (!success) {
                    return@launch
                }
                _isModelLoaded.value = true
            } else {
                _isModelLoaded.value = true
            }

            temiVoiceListener.setEnrollmentMode(true, name)
        }
    }

    /**
     * Releases resources when the ViewModel is destroyed.
     */
    override fun onCleared() {
        stopListening()
        temiVoiceListener.release()
        voiceManager.release()
    }
}
