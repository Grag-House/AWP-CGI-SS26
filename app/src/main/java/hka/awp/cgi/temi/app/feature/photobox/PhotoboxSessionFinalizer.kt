package hka.awp.cgi.temi.app.feature.photobox

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Turns the raw shots from a [PhotoboxCaptureSequencer] run into the final image (baking in the
 * Temi overlay per-frame for a strip, or combining the strip itself) and uploads it, reporting
 * progress back through callbacks.
 */
internal class PhotoboxSessionFinalizer(
    private val uploadRepository: PhotoboxUploadRepository,
    private val scope: CoroutineScope
) {
    fun finalizeAndUpload(
        mode: PhotoboxMode,
        shots: List<Bitmap>,
        withOverlay: Boolean,
        onFinalImageReady: (Bitmap) -> Unit,
        onUploadResult: (Bitmap, PhotoboxUploadState, String?) -> Unit
    ) {
        scope.launch {
            // Combining the strip and baking the overlay are pure CPU/bitmap work with no need
            // for the main thread — running them inline on viewModelScope (Dispatchers.Main)
            // would freeze the UI for the duration, which can be long enough for a stray touch
            // (e.g. on the sidebar) to queue up and fire once the main thread frees up again.
            val (finalImage, overlayAlreadyHandled) = withContext(Dispatchers.Default) {
                buildFinalImage(mode, shots, withOverlay)
            }
            onFinalImageReady(finalImage)

            uploadRepository.uploadPhoto(finalImage, withOverlay && !overlayAlreadyHandled).fold(
                onSuccess = { url -> onUploadResult(finalImage, PhotoboxUploadState.SUCCESS, url) },
                onFailure = { error ->
                    Timber.e(error, "Failed to upload photobox photo")
                    onUploadResult(finalImage, PhotoboxUploadState.FAILED, null)
                }
            )
        }
    }

    // For a strip, Temi is baked into each individual frame before combining — overlaying it
    // once on the whole tall strip would put a single oversized Temi next to it instead of on
    // each photo. Standard mode keeps the existing behavior: shown live via a separate Compose
    // layer, baked in only at upload time (overlayAlreadyHandled = false).
    private fun buildFinalImage(
        mode: PhotoboxMode,
        shots: List<Bitmap>,
        withOverlay: Boolean
    ): Pair<Bitmap, Boolean> {
        if (mode != PhotoboxMode.STRIP) return shots.first() to false

        val frames = if (withOverlay) shots.map(uploadRepository::bakeOverlay) else shots
        return combinePhotoStrip(frames) to true
    }
}
