package hka.awp.cgi.temi.app.feature.photobox.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.koin.core.context.GlobalContext
import timber.log.Timber

internal const val KEY_PENDING_ID = "pending_id"
internal const val KEY_VIEW_URL = "view_url"
internal const val KEY_EXPIRES_AT = "expires_at"

/**
 * Retries a single cached Photobox upload. Scheduled with a network constraint, so WorkManager
 * won't even start it until connectivity is back; a failed attempt returns [Result.retry] so
 * WorkManager's backoff policy keeps trying rather than dropping the photo.
 *
 * Looks up its dependencies straight from Koin's [GlobalContext] instead of constructor
 * injection — WorkManager's default [androidx.work.WorkerFactory] instantiates workers via a
 * plain (Context, WorkerParameters) constructor, so there's no hook to pass Koin-provided
 * arguments in otherwise.
 */
class PhotoboxUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pendingId = inputData.getString(KEY_PENDING_ID) ?: return Result.failure()

        val koin = GlobalContext.get()
        val pendingUploadStore = koin.get<PhotoboxPendingUploadStore>()
        val uploadRepository = koin.get<PhotoboxUploadRepository>()

        val bitmap = pendingUploadStore.load(pendingId) ?: return Result.failure()

        return uploadRepository.uploadFinalPhoto(bitmap).fold(
            onSuccess = { result ->
                pendingUploadStore.delete(pendingId)
                Result.success(
                    workDataOf(
                        KEY_VIEW_URL to result.viewUrl,
                        KEY_EXPIRES_AT to result.expiresAtMillis
                    )
                )
            },
            onFailure = { error ->
                Timber.w(error, "Queued photobox upload failed, will retry: %s", pendingId)
                Result.retry()
            }
        )
    }
}
