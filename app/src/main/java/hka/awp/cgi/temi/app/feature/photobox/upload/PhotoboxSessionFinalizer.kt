package hka.awp.cgi.temi.app.feature.photobox.upload

import android.graphics.Bitmap
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxMode
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCaptureSequencer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Outcome of a finalize-and-upload run, distinguishing a genuine failure from one that's been
 * queued for an automatic retry once the network is back. */
internal sealed interface PhotoboxUploadOutcome {
    data class Success(val result: PhotoboxUploadResult) : PhotoboxUploadOutcome
    data class Queued(val pendingId: String) : PhotoboxUploadOutcome
    data object Failed : PhotoboxUploadOutcome
}

/**
 * Turns the raw shots from a [PhotoboxCaptureSequencer] run into the final image (baking in the
 * Temi overlay per-frame for a strip, or combining the strip itself) and uploads it, reporting
 * progress back through callbacks.
 */
internal class PhotoboxSessionFinalizer(
    private val uploadRepository: PhotoboxUploadRepository,
    private val uploadQueue: PhotoboxUploadQueue,
    private val scope: CoroutineScope
) {
    fun finalizeAndUpload(
        mode: PhotoboxMode,
        shots: List<Bitmap>,
        withOverlay: Boolean,
        onFinalImageReady: (Bitmap) -> Unit,
        onUploadResult: (Bitmap, PhotoboxUploadOutcome) -> Unit
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
                onSuccess = { result ->
                    onUploadResult(finalImage, PhotoboxUploadOutcome.Success(result))
                },
                onFailure = { error ->
                    // A misconfigured webhook/folder will fail the exact same way every retry —
                    // queuing it would just hammer the server forever for no benefit. Anything
                    // else (network blip, transient server error) is worth retrying once the
                    // connection is back.
                    if (error is IllegalStateException) {
                        Timber.e(error, "Photobox upload misconfigured, not queuing for retry")
                        onUploadResult(finalImage, PhotoboxUploadOutcome.Failed)
                    } else {
                        Timber.w(error, "Failed to upload photobox photo, queuing for retry")
                        // enqueue() compresses the bitmap to disk — blocking I/O, off the main thread.
                        val pendingId = withContext(Dispatchers.IO) { uploadQueue.enqueue(finalImage) }
                        onUploadResult(finalImage, PhotoboxUploadOutcome.Queued(pendingId))
                    }
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
