package hka.awp.cgi.temi.app.feature.voiceRecognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.feature.webserver.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TemiVoiceRecognitionViewModel(
    private val voiceManager: TemiVoiceManager,
    private val temiVoiceListener: TemiVoiceListener,
    private val appConfigRepository: AppConfigRepository
) :
    ViewModel() {

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    val isEnrollmentActive: StateFlow<Boolean> = temiVoiceListener.isEnrollmentActive
    val enrollmentStatus: StateFlow<TemiVoiceListener.EnrollmentStatus> = temiVoiceListener.enrollmentStatus

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
                startListening()
                return@launch
            }

            val success = voiceManager.initModel()
            if (success) {
                _isModelLoaded.value = true
                startListening()
            }
        }
    }

    fun startListening() {
        temiVoiceListener.startListening()
    }

    fun stopListening() {
        temiVoiceListener.stopListening()
    }

    fun toggleEnrollment(active: Boolean, name: String? = null) {
        temiVoiceListener.setEnrollmentMode(active, name)
    }

    fun clearEnrollmentStatus() {
        temiVoiceListener.clearEnrollmentStatus()
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        temiVoiceListener.release()
        voiceManager.release()
    }
}
