package hka.awp.cgi.temi.app.feature.photobox.upload

import android.graphics.Bitmap
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxMode
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCaptureSequencer
import hka.awp.cgi.temi.app.feature.photobox.filter.PhotoboxPhotoFilter
import hka.awp.cgi.temi.app.feature.photobox.filter.bake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Outcome of an upload run, distinguishing a genuine failure from one that's been queued for an
 * automatic retry once the network is back. */
internal sealed interface PhotoboxUploadOutcome {
    data class Success(val result: PhotoboxUploadResult) : PhotoboxUploadOutcome
    data class Queued(val pendingId: String) : PhotoboxUploadOutcome
    data object Failed : PhotoboxUploadOutcome
}

private const val GRID_2X2_COLUMNS = 2

/**
 * Turns the raw shots from a [PhotoboxCaptureSequencer] run into the final image (baking in the
 * Temi overlay per-frame for a multi-shot mode, or combining the shots into a strip/grid),
 * separately from uploading it — the caller decides when (and with which [PhotoboxPhotoFilter])
 * the upload actually starts, so the user can preview/pick a filter on the finished photo before
 * anything leaves the device.
 */
internal class PhotoboxSessionFinalizer(
    private val uploadRepository: PhotoboxUploadRepository,
    private val uploadQueue: PhotoboxUploadQueue,
    private val scope: CoroutineScope
) {
    fun finalize(
        mode: PhotoboxMode,
        shots: List<Bitmap>,
        withOverlay: Boolean,
        onFinalImageReady: (finalImage: Bitmap, needsOverlayBakeAtUpload: Boolean) -> Unit
    ) {
        scope.launch {
            // Combining the strip and baking the overlay are pure CPU/bitmap work with no need
            // for the main thread — running them inline on viewModelScope (Dispatchers.Main)
            // would freeze the UI for the duration, which can be long enough for a stray touch
            // (e.g. on the sidebar) to queue up and fire once the main thread frees up again.
            val (finalImage, needsOverlayBakeAtUpload) = withContext(Dispatchers.Default) {
                buildFinalImage(mode, shots, withOverlay)
            }
            onFinalImageReady(finalImage, needsOverlayBakeAtUpload)
        }
    }

    /**
     * Bakes [filter] into [finalImage] and uploads the result. [finalImage] itself is passed
     * back unchanged via [onUploadResult] (not the filtered copy) so callers can keep comparing
     * it by identity against the bitmap they're already showing in the UI.
     */
    fun upload(
        finalImage: Bitmap,
        filter: PhotoboxPhotoFilter,
        needsOverlayBake: Boolean,
        onUploadResult: (Bitmap, PhotoboxUploadOutcome) -> Unit
    ) {
        scope.launch {
            val filteredImage = withContext(Dispatchers.Default) { filter.bake(finalImage) }
            uploadRepository.uploadPhoto(filteredImage, needsOverlayBake).fold(
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
                        val pendingId = withContext(Dispatchers.IO) { uploadQueue.enqueue(filteredImage) }
                        onUploadResult(finalImage, PhotoboxUploadOutcome.Queued(pendingId))
                    }
                }
            )
        }
    }

    // For a strip/grid, Temi is baked into each individual frame before combining — overlaying
    // it once on the whole composite would put a single oversized Temi next to it instead of on
    // each photo. Standard mode keeps the existing behavior: shown live via a separate Compose
    // layer, baked in only at upload time (needsOverlayBakeAtUpload = withOverlay).
    private fun buildFinalImage(
        mode: PhotoboxMode,
        shots: List<Bitmap>,
        withOverlay: Boolean
    ): Pair<Bitmap, Boolean> {
        if (mode == PhotoboxMode.STANDARD) return shots.first() to withOverlay

        val frames = if (withOverlay) shots.map(uploadRepository::bakeOverlay) else shots
        val combined = if (mode == PhotoboxMode.GRID_2X2) {
            combinePhotoGrid(frames, columns = GRID_2X2_COLUMNS)
        } else {
            combinePhotoStrip(frames)
        }
        return combined to false
    }
}
