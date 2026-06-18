package hka.awp.cgi.temi.app.feature.photobox

import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.camera.core.ViewPort
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

enum class PhotoboxPhase { IDLE, COUNTDOWN, CAPTURE, PREVIEW }

data class PhotoboxUiState(
    val phase: PhotoboxPhase = PhotoboxPhase.IDLE,
    val selectedDuration: Int = DEFAULT_DURATION,
    val countdownRemaining: Int = DEFAULT_DURATION,
    val capturedBitmap: Bitmap? = null
)

private const val DEFAULT_DURATION = 3
private const val TICK_MS = 1000L

class PhotoboxViewModel(
    private val cameraManager: PhotoboxCameraManager,
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoboxUiState())
    val uiState: StateFlow<PhotoboxUiState> = _uiState.asStateFlow()

    val cameraState: StateFlow<PhotoboxCameraState> = cameraManager.cameraState

    val overlayEnabled: StateFlow<Boolean> = appConfigRepository.photoboxOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var countdownJob: Job? = null

    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider, viewPort: ViewPort?) {
        cameraManager.bindToLifecycle(lifecycleOwner, surfaceProvider, viewPort)
    }

    fun setDuration(seconds: Int) {
        _uiState.update { it.copy(selectedDuration = seconds, countdownRemaining = seconds) }
    }

    fun startSession() = startCountdown()

    // Returns to the duration picker instead of immediately re-starting the countdown.
    fun takeAnotherPhoto() = reset()

    fun reset() {
        countdownJob?.cancel()
        _uiState.update { state ->
            PhotoboxUiState(
                selectedDuration = state.selectedDuration,
                countdownRemaining = state.selectedDuration
            )
        }
    }

    /** Called when the Photobox tab is left (e.g. the user switches to another tab). */
    fun onScreenStopped() {
        reset()
        cameraManager.unbind()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        val duration = _uiState.value.selectedDuration
        _uiState.update {
            it.copy(
                phase = PhotoboxPhase.COUNTDOWN,
                countdownRemaining = duration,
                capturedBitmap = null
            )
        }
        countdownJob = viewModelScope.launch {
            var remaining = duration
            while (remaining > 0 && isActive) {
                _uiState.update { it.copy(countdownRemaining = remaining) }
                delay(TICK_MS)
                remaining--
            }
            if (!isActive) return@launch
            // Enter CAPTURE — stays here until the camera callback resolves the photo
            _uiState.update { it.copy(phase = PhotoboxPhase.CAPTURE) }
            capturePhoto()
        }
    }

    private fun capturePhoto() {
        cameraManager.capturePhoto { result ->
            result.fold(
                onSuccess = { bitmap ->
                    _uiState.update { it.copy(phase = PhotoboxPhase.PREVIEW, capturedBitmap = bitmap) }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to capture photobox photo")
                    _uiState.update { it.copy(phase = PhotoboxPhase.IDLE) }
                }
            )
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        cameraManager.unbind()
        super.onCleared()
    }
}
