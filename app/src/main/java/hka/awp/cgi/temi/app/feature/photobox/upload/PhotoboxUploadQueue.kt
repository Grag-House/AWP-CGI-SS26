package hka.awp.cgi.temi.app.feature.photobox.upload

import android.content.Context
import android.graphics.Bitmap
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow

private val UPLOAD_CONSTRAINTS = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

/**
 * Queues a Photobox photo for upload retry when the first attempt fails (e.g. flaky WiFi).
 * Backed by [PhotoboxPendingUploadStore] for the bytes and WorkManager for the retry scheduling —
 * WorkManager won't even start the job until a network is back, then retries with backoff if the
 * server itself errors.
 *
 * Each job is named after its [PhotoboxPendingUploadStore] id, so the file on disk and its
 * WorkManager job are always 1:1 — [reconcileOrphans] relies on that to safely re-enqueue without
 * risking a duplicate upload (KEEP policy skips a job that's already pending/running).
 */
class PhotoboxUploadQueue(
    private val context: Context,
    private val pendingUploadStore: PhotoboxPendingUploadStore
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Caches [photo] to disk and schedules its upload; returns the id to observe its progress with. */
    fun enqueue(photo: Bitmap): String {
        val pendingId = pendingUploadStore.save(photo)
        enqueueWorker(pendingId)
        return pendingId
    }

    /** Re-enqueues photos left on disk from a previous process death (app killed before a retry
     * could finish) — call once on app start. */
    fun reconcileOrphans() {
        pendingUploadStore.pendingIds().forEach(::enqueueWorker)
    }

    fun workInfoFlow(pendingId: String): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(pendingId)

    private fun enqueueWorker(pendingId: String) {
        val request = OneTimeWorkRequestBuilder<PhotoboxUploadWorker>()
            .setConstraints(UPLOAD_CONSTRAINTS)
            .setInputData(workDataOf(KEY_PENDING_ID to pendingId))
            .build()
        workManager.enqueueUniqueWork(pendingId, ExistingWorkPolicy.KEEP, request)
    }
}
