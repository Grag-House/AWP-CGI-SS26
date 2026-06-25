package hka.awp.cgi.temi.app.feature.voiceRecognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TemiVoiceRecognitionViewModel(
    private val voiceManager: TemiVoiceManager,
    private val temiVoiceListener: TemiVoiceListener,
    private val appConfigRepository: AppConfigRepository
) :
    ViewModel() {

    private val _isModelLoaded = MutableStateFlow(false)
    val isEnrollmentActive = temiVoiceListener.isEnrollmentActive

    init {
        viewModelScope.launch {
            appConfigRepository.isSpeakerVerificationEnabled.collect { enabled ->
                if (enabled) {
                    initializeVoiceAi()
                } else {
                    stopListening()
                }
            }
        }
    }

    fun initializeVoiceAi() {
        viewModelScope.launch {
            if (voiceManager.isReady()) {
                _isModelLoaded.value = true
                temiVoiceListener.startListening()
                return@launch
            }

            val success = voiceManager.initModel()
            if (success) {
                _isModelLoaded.value = true
                temiVoiceListener.startListening()
            }
        }
    }

    fun stopListening() {
        temiVoiceListener.stopListening()
    }

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

    override fun onCleared() {
        stopListening()
        temiVoiceListener.release()
        voiceManager.release()
    }
}
