package hka.awp.cgi.temi.app.feature.photobox.upload

import android.graphics.Bitmap
import androidx.work.WorkInfo
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxUiState
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxUploadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Applies a [PhotoboxUploadOutcome] to the shared ui state. For a queued upload, also keeps
 * watching its WorkManager job so a retry that succeeds later — while the user is still looking
 * at that same preview — is reflected live instead of leaving the UI stuck on "queued".
 */
internal class PhotoboxUploadOutcomeHandler(
    private val uploadQueue: PhotoboxUploadQueue,
    private val uiState: MutableStateFlow<PhotoboxUiState>,
    private val scope: CoroutineScope
) {
    private var queuedUploadObserverJob: Job? = null

    fun handle(finalImage: Bitmap, outcome: PhotoboxUploadOutcome) {
        when (outcome) {
            is PhotoboxUploadOutcome.Success -> updateIfCurrent(finalImage) {
                it.copy(
                    uploadState = PhotoboxUploadState.SUCCESS,
                    uploadedPhotoUrl = outcome.result.viewUrl,
                    uploadedPhotoExpiresAt = outcome.result.expiresAtMillis
                )
            }
            is PhotoboxUploadOutcome.Queued -> {
                updateIfCurrent(finalImage) { it.copy(uploadState = PhotoboxUploadState.QUEUED) }
                observeQueuedUpload(outcome.pendingId, finalImage)
            }
            PhotoboxUploadOutcome.Failed -> updateIfCurrent(finalImage) {
                it.copy(uploadState = PhotoboxUploadState.FAILED)
            }
        }
    }

    /** Stops watching a queued upload — called when the user moves on to another photo. */
    fun cancel() {
        queuedUploadObserverJob?.cancel()
    }

    // Guards against a slow/queued upload finishing after the user already moved on to another
    // photo — only applies the update if [finalImage] is still the one currently shown.
    private fun updateIfCurrent(finalImage: Bitmap, transform: (PhotoboxUiState) -> PhotoboxUiState) {
        uiState.update { if (it.capturedBitmap === finalImage) transform(it) else it }
    }

    private fun observeQueuedUpload(pendingId: String, finalImage: Bitmap) {
        queuedUploadObserverJob?.cancel()
        queuedUploadObserverJob = uploadQueue.workInfoFlow(pendingId)
            .onEach { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@onEach
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val viewUrl = workInfo.outputData.getString(KEY_VIEW_URL)
                        val expiresAt = workInfo.outputData.getLong(KEY_EXPIRES_AT, -1L)
                        updateIfCurrent(finalImage) {
                            if (viewUrl != null && expiresAt >= 0L) {
                                it.copy(
                                    uploadState = PhotoboxUploadState.SUCCESS,
                                    uploadedPhotoUrl = viewUrl,
                                    uploadedPhotoExpiresAt = expiresAt
                                )
                            } else {
                                it.copy(uploadState = PhotoboxUploadState.FAILED)
                            }
                        }
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED ->
                        updateIfCurrent(finalImage) { it.copy(uploadState = PhotoboxUploadState.FAILED) }
                    else -> Unit
                }
            }
            .launchIn(scope)
    }
}
