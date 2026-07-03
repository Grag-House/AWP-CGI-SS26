package hka.awp.cgi.temi.app.feature.photobox

import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.camera.core.ViewPort
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.data.repository.PhotoboxConfigRepository
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCameraManager
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCameraState
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCaptureCallbacks
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCaptureSequencer
import hka.awp.cgi.temi.app.feature.photobox.filter.PhotoboxPhotoFilter
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxFinalizeOptions
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxOverlayOptions
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxSessionFinalizer
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadOutcomeHandler
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadQueue
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Current phase of the Photobox session, determining which overlay is shown on screen. */
enum class PhotoboxPhase { MODE_SELECT, IDLE, COUNTDOWN, CAPTURE, PREVIEW }

private const val STRIP_SHOT_COUNT = 3
private const val FOUR_SHOT_COUNT = 4

/** Capture mode determining how many shots are taken and how they are composed into the final image. */
enum class PhotoboxMode(val shotCount: Int) {
    STANDARD(1),
    STRIP(STRIP_SHOT_COUNT),
    STRIP_1X4(FOUR_SHOT_COUNT),
    GRID_2X2(FOUR_SHOT_COUNT)
}

/** Upload state of the current session's photo, reflected live in the preview screen. */
enum class PhotoboxUploadState { NONE, UPLOADING, SUCCESS, FAILED, QUEUED }

/** Complete UI state for the Photobox feature, observed by [PhotoboxScreen]. */
data class PhotoboxUiState(
    val phase: PhotoboxPhase = PhotoboxPhase.MODE_SELECT,
    val mode: PhotoboxMode = PhotoboxMode.STANDARD,
    val selectedDuration: Int = DEFAULT_DURATION,
    val countdownRemaining: Int = DEFAULT_DURATION,
    val stripDelaySeconds: Int = DEFAULT_STRIP_DELAY,
    val isBetweenShots: Boolean = false,
    val shotsTaken: Int = 0,
    val capturedBitmap: Bitmap? = null,
    val capturedBanner: PhotoboxBanner? = null,
    val uploadState: PhotoboxUploadState = PhotoboxUploadState.NONE,
    val uploadedPhotoUrl: String? = null,
    val uploadedPhotoExpiresAt: Long? = null,
    val selectedFilter: PhotoboxPhotoFilter = PhotoboxPhotoFilter.NONE,
    val showQrCode: Boolean = false
) {
    val totalShots: Int get() = mode.shotCount
}

private const val DEFAULT_DURATION = 3
private const val DEFAULT_STRIP_DELAY = 10

/**
 * ViewModel for the Photobox feature.
 *
 * Manages the camera lifecycle, the capture sequencer, and the upload pipeline.
 *
 * @property cameraManager Manager for camera hardware interactions.
 * @property photoboxConfigRepository Repository for retrieving photobox settings.
 * @property uploadRepository Repository for photo uploads.
 * @property uploadQueue Queue for handling background photo uploads.
 */
class PhotoboxViewModel(
    private val cameraManager: PhotoboxCameraManager,
    private val photoboxConfigRepository: PhotoboxConfigRepository,
    uploadRepository: PhotoboxUploadRepository,
    uploadQueue: PhotoboxUploadQueue
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoboxUiState())

    /** The current UI state for the Photobox. */
    val uiState: StateFlow<PhotoboxUiState> = _uiState.asStateFlow()

    /** The current state of the camera. */
    val cameraState: StateFlow<PhotoboxCameraState> = cameraManager.cameraState

    internal val overlaySettings = PhotoboxOverlaySettings(photoboxConfigRepository, viewModelScope)

    /** Flow indicating if the overlay is enabled. */
    val overlayEnabled: StateFlow<Boolean> = overlaySettings.enabled

    /** Flow of the current overlay position. */
    val overlayPosition: StateFlow<TemiOverlayPosition> = overlaySettings.position

    internal val bannerSettings = PhotoboxBannerSettings(photoboxConfigRepository, viewModelScope)

    private val captureSequencer = PhotoboxCaptureSequencer(cameraManager, viewModelScope)
    private val sessionFinalizer = PhotoboxSessionFinalizer(uploadRepository, uploadQueue, viewModelScope)
    private val uploadOutcomeHandler = PhotoboxUploadOutcomeHandler(uploadQueue, _uiState, viewModelScope)

    internal val pendingUploadController =
        PhotoboxPendingUploadController(sessionFinalizer, _uiState, uploadOutcomeHandler::handle)

    /** Binds the camera to a lifecycle and surface provider. */
    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider, viewPort: ViewPort?) {
        cameraManager.bindToLifecycle(lifecycleOwner, surfaceProvider, viewPort)
    }

    /** Sets the current capture mode. */
    fun selectMode(mode: PhotoboxMode) {
        _uiState.update { it.copy(mode = mode, phase = PhotoboxPhase.IDLE) }
    }

    /** Returns to the mode selection phase. */
    fun backToModeSelect() {
        _uiState.update { it.copy(phase = PhotoboxPhase.MODE_SELECT) }
    }

    /** Sets the countdown duration. */
    fun setDuration(seconds: Int) {
        _uiState.update { it.copy(selectedDuration = seconds, countdownRemaining = seconds) }
    }

    /** Sets the delay between shots in strip mode. */
    fun setStripDelay(seconds: Int) {
        _uiState.update { it.copy(stripDelaySeconds = seconds) }
    }

    /** Starts the capture session. */
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
                    val bannerAtCapture = bannerSettings.banner.value.takeIf { bannerSettings.enabled.value }
                    val overlayOptionsAtCapture =
                        PhotoboxOverlayOptions(overlayPosition.value, bannerAtCapture, mode = state.mode)
                    sessionFinalizer.finalize(
                        mode = state.mode,
                        shots = shots,
                        options = PhotoboxFinalizeOptions(
                            withOverlay = overlayEnabled.value,
                            overlay = overlayOptionsAtCapture
                        ),
                        onFinalImageReady = { finalImage, needsOverlayBakeAtUpload ->
                            pendingUploadController.begin(
                                finalImage,
                                needsOverlayBakeAtUpload,
                                overlayOptionsAtCapture
                            )
                            _uiState.update {
                                it.copy(
                                    phase = PhotoboxPhase.PREVIEW,
                                    capturedBitmap = finalImage,
                                    capturedBanner = bannerAtCapture,
                                    uploadState = PhotoboxUploadState.NONE,
                                    uploadedPhotoUrl = null,
                                    uploadedPhotoExpiresAt = null,
                                    selectedFilter = PhotoboxPhotoFilter.NONE
                                )
                            }
                        }
                    )
                },
                onFailed = {
                    _uiState.update { it.copy(phase = PhotoboxPhase.IDLE) }
                }
            )
        )
    }

    /** Resets the Photobox session to the default state. */
    fun reset() {
        captureSequencer.cancel()
        uploadOutcomeHandler.cancel()
        pendingUploadController.abandonAndClear()
        _uiState.update { state ->
            PhotoboxUiState(
                mode = state.mode,
                selectedDuration = state.selectedDuration,
                countdownRemaining = state.selectedDuration,
                stripDelaySeconds = state.stripDelaySeconds
            )
        }
    }

    /** Called when the Photobox screen is stopped. */
    fun onScreenStopped() {
        reset()
        cameraManager.unbind()
    }

    /** Toggles the visibility of the QR code for a successfully uploaded photo. */
    fun setQrCodeVisible(visible: Boolean) {
        if (visible && _uiState.value.uploadedPhotoUrl == null) return
        _uiState.update { it.copy(showQrCode = visible) }
    }

    override fun onCleared() {
        captureSequencer.cancel()
        cameraManager.unbind()
    }
}
