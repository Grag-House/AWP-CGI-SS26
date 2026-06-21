package hka.awp.cgi.temi.app.feature.photobox

import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.camera.core.ViewPort
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCameraManager
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCameraState
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCaptureCallbacks
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCaptureSequencer
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxSessionFinalizer
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadOutcomeHandler
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadQueue
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadRepository
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class PhotoboxPhase { MODE_SELECT, IDLE, COUNTDOWN, CAPTURE, PREVIEW }

enum class PhotoboxMode { STANDARD, STRIP }

enum class PhotoboxUploadState { NONE, UPLOADING, SUCCESS, FAILED, QUEUED }

data class PhotoboxUiState(
    val phase: PhotoboxPhase = PhotoboxPhase.MODE_SELECT,
    val mode: PhotoboxMode = PhotoboxMode.STANDARD,
    val selectedDuration: Int = DEFAULT_DURATION,
    val countdownRemaining: Int = DEFAULT_DURATION,
    val stripDelaySeconds: Int = DEFAULT_STRIP_DELAY,
    val isBetweenShots: Boolean = false,
    val shotsTaken: Int = 0,
    val capturedBitmap: Bitmap? = null,
    val uploadState: PhotoboxUploadState = PhotoboxUploadState.NONE,
    val uploadedPhotoUrl: String? = null,
    val uploadedPhotoExpiresAt: Long? = null,
    val showQrCode: Boolean = false
) {
    val totalShots: Int get() = if (mode == PhotoboxMode.STRIP) STRIP_SHOT_COUNT else 1
}

const val STRIP_SHOT_COUNT = 3
private const val DEFAULT_DURATION = 3
private const val DEFAULT_STRIP_DELAY = 10

class PhotoboxViewModel(
    private val cameraManager: PhotoboxCameraManager,
    appConfigRepository: AppConfigRepository,
    uploadRepository: PhotoboxUploadRepository,
    uploadQueue: PhotoboxUploadQueue
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoboxUiState())
    val uiState: StateFlow<PhotoboxUiState> = _uiState.asStateFlow()

    val cameraState: StateFlow<PhotoboxCameraState> = cameraManager.cameraState

    val isFrontCamera: StateFlow<Boolean> = cameraManager.isFrontCamera

    val overlayEnabled: StateFlow<Boolean> = appConfigRepository.photoboxOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val captureSequencer = PhotoboxCaptureSequencer(cameraManager, viewModelScope)
    private val sessionFinalizer = PhotoboxSessionFinalizer(uploadRepository, uploadQueue, viewModelScope)
    private val uploadOutcomeHandler = PhotoboxUploadOutcomeHandler(uploadQueue, _uiState, viewModelScope)

    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider, viewPort: ViewPort?) {
        cameraManager.bindToLifecycle(lifecycleOwner, surfaceProvider, viewPort)
    }

    fun selectMode(mode: PhotoboxMode) {
        _uiState.update { it.copy(mode = mode, phase = PhotoboxPhase.IDLE) }
    }

    fun backToModeSelect() {
        _uiState.update { it.copy(phase = PhotoboxPhase.MODE_SELECT) }
    }

    fun setDuration(seconds: Int) {
        _uiState.update { it.copy(selectedDuration = seconds, countdownRemaining = seconds) }
    }

    fun setStripDelay(seconds: Int) {
        _uiState.update { it.copy(stripDelaySeconds = seconds) }
    }

    fun startSession() {
        val state = _uiState.value
        captureSequencer.start(
            mode = state.mode,
            firstShotDelaySeconds = state.selectedDuration,
            betweenShotsDelaySeconds = state.stripDelaySeconds,
            callbacks = PhotoboxCaptureCallbacks(
                onTick = { remaining, isBetweenShots, shotsTaken ->
                    _uiState.update {
                        it.copy(
                            phase = PhotoboxPhase.COUNTDOWN,
                            countdownRemaining = remaining,
                            isBetweenShots = isBetweenShots,
                            shotsTaken = shotsTaken
                        )
                    }
                },
                onCapturing = {
                    _uiState.update { it.copy(phase = PhotoboxPhase.CAPTURE) }
                },
                onShotsReady = { shots ->
                    sessionFinalizer.finalizeAndUpload(
                        mode = state.mode,
                        shots = shots,
                        withOverlay = overlayEnabled.value,
                        onFinalImageReady = { finalImage ->
                            _uiState.update {
                                it.copy(
                                    phase = PhotoboxPhase.PREVIEW,
                                    capturedBitmap = finalImage,
                                    uploadState = PhotoboxUploadState.UPLOADING,
                                    uploadedPhotoUrl = null,
                                    uploadedPhotoExpiresAt = null
                                )
                            }
                        },
                        onUploadResult = uploadOutcomeHandler::handle
                    )
                },
                onFailed = {
                    _uiState.update { it.copy(phase = PhotoboxPhase.IDLE) }
                }
            )
        )
    }

    // Resets back to the mode/duration picker — used both for "take another photo" and "cancel".
    fun reset() {
        captureSequencer.cancel()
        uploadOutcomeHandler.cancel()
        _uiState.update { state ->
            PhotoboxUiState(
                mode = state.mode,
                selectedDuration = state.selectedDuration,
                countdownRemaining = state.selectedDuration,
                stripDelaySeconds = state.stripDelaySeconds
            )
        }
    }

    /** Called when the Photobox tab is left (e.g. the user switches to another tab). */
    fun onScreenStopped() {
        reset()
        cameraManager.unbind()
    }

    fun setQrCodeVisible(visible: Boolean) {
        if (visible && _uiState.value.uploadedPhotoUrl == null) return
        _uiState.update { it.copy(showQrCode = visible) }
    }

    override fun onCleared() {
        captureSequencer.cancel()
        cameraManager.unbind()
        super.onCleared()
    }
}
