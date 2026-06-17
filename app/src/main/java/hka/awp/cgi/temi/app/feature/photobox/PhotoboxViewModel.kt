package hka.awp.cgi.temi.app.feature.photobox

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PhotoboxPhase { IDLE, COUNTDOWN, CAPTURE, PREVIEW }

data class PhotoboxUiState(
    val phase: PhotoboxPhase = PhotoboxPhase.IDLE,
    val selectedDuration: Int = DEFAULT_DURATION,
    val countdownRemaining: Int = DEFAULT_DURATION,
    val capturedBitmap: Bitmap? = null
)

private const val DEFAULT_DURATION = 3
private const val TICK_MS = 1000L

class PhotoboxViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoboxUiState())
    val uiState: StateFlow<PhotoboxUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun setDuration(seconds: Int) {
        _uiState.update { it.copy(selectedDuration = seconds, countdownRemaining = seconds) }
    }

    fun startSession() = startCountdown()

    fun takeAnotherPhoto() = startCountdown()

    fun onPhotoCaptured(bitmap: Bitmap) {
        _uiState.update { it.copy(phase = PhotoboxPhase.PREVIEW, capturedBitmap = bitmap) }
    }

    fun reset() {
        countdownJob?.cancel()
        _uiState.update { state ->
            PhotoboxUiState(
                selectedDuration = state.selectedDuration,
                countdownRemaining = state.selectedDuration
            )
        }
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
            // Enter CAPTURE — stays here until onPhotoCaptured() is called
            _uiState.update { it.copy(phase = PhotoboxPhase.CAPTURE) }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
