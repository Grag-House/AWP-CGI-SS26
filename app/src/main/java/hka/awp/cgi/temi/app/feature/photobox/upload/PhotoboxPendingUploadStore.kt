package hka.awp.cgi.temi.app.feature.photobox.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.util.UUID

private const val PENDING_DIR_NAME = "photobox_pending"
private const val JPEG_QUALITY = 90

/**
 * Disk-backed queue for photos whose upload failed (e.g. flaky WiFi). Each entry is the fully
 * finalized JPEG — overlay already baked in, strip already combined — so retrying it later is
 * just re-running the network call, nothing else.
 *
 * Files are named by their own [PhotoboxUploadWorker] work id, so a file's presence on disk and
 * its WorkManager job are always 1:1 — that's what lets [pendingIds] re-enqueue orphans after a
 * process death without risking a duplicate upload.
 */
class PhotoboxPendingUploadStore(context: Context) {
    private val pendingDir = File(context.filesDir, PENDING_DIR_NAME).apply { mkdirs() }

    /** Saves [photo] to disk and returns the id to use as both the filename and the work name. */
    fun save(photo: Bitmap): String {
        val id = UUID.randomUUID().toString()
        fileFor(id).outputStream().use { out ->
            photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return id
    }

    fun load(id: String): Bitmap? {
        val file = fileFor(id)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun delete(id: String) {
        fileFor(id).delete()
    }

    /** All ids with a cached file on disk right now — used to resume orphaned uploads on app start. */
    fun pendingIds(): List<String> =
        pendingDir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()

    private fun fileFor(id: String) = File(pendingDir, "$id.jpg")
}
