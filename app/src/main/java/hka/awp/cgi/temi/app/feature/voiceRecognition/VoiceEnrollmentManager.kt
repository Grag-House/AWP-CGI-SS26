package hka.awp.cgi.temi.app.feature.voiceRecognition

import hka.awp.cgi.temi.app.feature.settings.adminPanel.SpeakerVector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages the speaker enrollment process.
 */
class VoiceEnrollmentManager(
    private val scope: CoroutineScope,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val onSyncRequired: () -> Unit
) {
    enum class EnrollmentStatus {
        IDLE, TOO_SHORT, NO_VECTOR, SUCCESS
    }

    companion object {
        private const val MIN_ENROLLMENT_FRAMES = 1200
    }

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _status = MutableStateFlow(EnrollmentStatus.IDLE)
    val status: StateFlow<EnrollmentStatus> = _status.asStateFlow()

    private var enrollmentName: String = "Default"
    private var saveJob: Job? = null

    fun start(name: String?) {
        _isActive.value = true
        _status.value = EnrollmentStatus.IDLE
        enrollmentName = name?.trim()?.takeIf { it.isNotEmpty() } ?: "Default"
        Timber.i("Enrollment started for '%s'", enrollmentName)
        onSyncRequired()
    }

    fun stop() {
        _isActive.value = false
        saveJob?.cancel()
        saveJob = null
        Timber.i("Enrollment stopped")
        onSyncRequired()
    }

    fun handleResult(vector: SpeakerVector?, frames: Int) {
        if (!_isActive.value) return

        if (vector == null) {
            _status.value = EnrollmentStatus.NO_VECTOR
            Timber.w("Enrollment failed: No vector")
            return
        }

        if (frames < MIN_ENROLLMENT_FRAMES) {
            _status.value = EnrollmentStatus.TOO_SHORT
            Timber.w("Enrollment too short: %d/%d frames", frames, MIN_ENROLLMENT_FRAMES)
            return
        }

        Timber.i("Enrollment success: %d frames. Saving...", frames)
        save(enrollmentName, vector)
    }

    private fun save(name: String, vector: SpeakerVector) {
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            voiceProfileRepository.saveVoiceProfile(name, vector.values)
            _status.value = EnrollmentStatus.SUCCESS
            _isActive.value = false
            Timber.i("Voice profile '%s' saved", name)
            scope.launch { onSyncRequired() }
        }
    }

    fun release() {
        saveJob?.cancel()
    }
}
