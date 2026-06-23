package hka.awp.cgi.temi.app.feature.photobox

import android.graphics.Bitmap
import hka.awp.cgi.temi.app.feature.photobox.filter.PhotoboxPhotoFilter
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxSessionFinalizer
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the photo finalized by [PhotoboxSessionFinalizer.finalize] until the user picks a filter
 * and confirms (see [confirmUpload]), so nothing is uploaded before they've seen the preview.
 */
internal class PhotoboxPendingUploadController(
    private val sessionFinalizer: PhotoboxSessionFinalizer,
    private val uiState: MutableStateFlow<PhotoboxUiState>,
    private val onUploadResult: (Bitmap, PhotoboxUploadOutcome) -> Unit
) {
    private var pending: PendingUpload? = null

    private data class PendingUpload(
        val finalImage: Bitmap,
        val needsOverlayBake: Boolean,
        val overlayPosition: TemiOverlayPosition
    )

    fun begin(finalImage: Bitmap, needsOverlayBake: Boolean, overlayPosition: TemiOverlayPosition) {
        pending = PendingUpload(finalImage, needsOverlayBake, overlayPosition)
    }

    fun selectFilter(filter: PhotoboxPhotoFilter) {
        uiState.update { it.copy(selectedFilter = filter) }
    }

    fun confirmUpload() {
        val current = pending ?: return
        uiState.update { it.copy(uploadState = PhotoboxUploadState.UPLOADING) }
        sessionFinalizer.upload(
            finalImage = current.finalImage,
            filter = uiState.value.selectedFilter,
            needsOverlayBake = current.needsOverlayBake,
            overlayPosition = current.overlayPosition,
            onUploadResult = onUploadResult
        )
    }

    // If the user moves on (new photo / dashboard / leaving the tab) without ever generating a
    // QR code, the photo would otherwise never reach Drive at all — upload it anyway, unfiltered
    // and fire-and-forget, since there's no one left looking at a QR code for it.
    fun abandonAndClear() {
        val current = pending
        if (current != null && uiState.value.uploadState == PhotoboxUploadState.NONE) {
            sessionFinalizer.upload(
                finalImage = current.finalImage,
                filter = PhotoboxPhotoFilter.NONE,
                needsOverlayBake = current.needsOverlayBake,
                overlayPosition = current.overlayPosition,
                onUploadResult = { _, _ -> }
            )
        }
        pending = null
    }
}
