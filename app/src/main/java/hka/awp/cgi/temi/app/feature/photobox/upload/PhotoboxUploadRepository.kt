package hka.awp.cgi.temi.app.feature.photobox.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.util.Base64
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val JPEG_QUALITY = 90
private const val UPLOAD_TIMEOUT_SECONDS = 30L

// Kept in sync with PhotoboxScreen's TemiOverlayImage so the uploaded file matches what was
// shown on screen.
internal const val PHOTOBOX_OVERLAY_HEIGHT_FRACTION = 0.68f

/**
 * Result of a successful upload. [expiresAtMillis] is the backend's own enforcement of how long
 * [viewUrl] stays reachable — today that's a signed token checked by the Apps Script webhook, but
 * the contract (a URL good only until a point in time) is written so it still holds if the
 * backend is ever swapped for something with native expiring links (e.g. S3 presigned URLs).
 */
data class PhotoboxUploadResult(val viewUrl: String, val expiresAtMillis: Long)

/**
 * Uploads Photobox photos to a Google Drive folder via a Google Apps Script web app acting as a
 * webhook — the app never needs Google credentials, it just POSTs the image as base64 JSON.
 * Both the target folder and the webhook URL are read fresh from [AppConfigRepository] on every
 * upload, so they can be changed in settings without a rebuild.
 */
class PhotoboxUploadRepository(
    private val context: Context,
    client: OkHttpClient,
    private val appConfigRepository: AppConfigRepository
) {
    // Uploading a JPEG over a possibly slow/flaky WiFi connection can take longer than
    // OkHttp's default timeouts.
    private val uploadClient = client.newBuilder()
        .connectTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** Returns the time-limited, shareable view URL for the uploaded photo on success. */
    suspend fun uploadPhoto(photo: Bitmap, withOverlay: Boolean): Result<PhotoboxUploadResult> {
        val finalBitmap = if (withOverlay) bakeOverlay(photo) else photo
        return uploadFinalPhoto(finalBitmap)
    }

    /**
     * Uploads a photo that's already in its final form (overlay baked in, strip combined, if
     * applicable) — used both by [uploadPhoto] and by [PhotoboxUploadWorker] when retrying a
     * photo that was cached to disk after a failed first attempt.
     */
    suspend fun uploadFinalPhoto(finalBitmap: Bitmap): Result<PhotoboxUploadResult> = withContext(Dispatchers.IO) {
        try {
            val webhookUrl = appConfigRepository.driveUploadUrl.first()
            val folderLink = appConfigRepository.driveFolderLink.first()
            val folderId = extractDriveFolderId(folderLink)

            if (webhookUrl.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No Photobox upload URL configured"))
            }
            if (folderId == null) {
                return@withContext Result.failure(IllegalStateException("Invalid Drive folder link: $folderLink"))
            }

            val requestJson = JSONObject().apply {
                put("folderId", folderId)
                put("fileName", "photobox_${System.currentTimeMillis()}.jpg")
                put("imageBase64", encodeToBase64Jpeg(finalBitmap))
            }
            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(webhookUrl).post(requestBody).build()

            uploadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Upload failed with code ${response.code}"))
                }
                // The Apps Script web app always answers with HTTP 200, even on failure — the
                // actual outcome is in the JSON body.
                val responseJson = JSONObject(response.body.string())
                if (!responseJson.optBoolean("success", false)) {
                    val error = responseJson.optString("error", "Unknown upload error")
                    return@withContext Result.failure(IOException(error))
                }
                val viewUrl = responseJson.optString("viewUrl")
                if (viewUrl.isBlank()) {
                    return@withContext Result.failure(IOException("Upload response did not include a viewUrl"))
                }
                val expiresAt = responseJson.optLong("expiresAt", -1L)
                if (expiresAt < 0L) {
                    return@withContext Result.failure(IOException("Upload response did not include an expiresAt"))
                }
                return@withContext Result.success(PhotoboxUploadResult(viewUrl, expiresAt))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.e(e, "Failed to upload photobox photo")
            Result.failure(e)
        } catch (e: JSONException) {
            Timber.e(e, "Unexpected response from Photobox upload webhook")
            Result.failure(e)
        }
    }

    private fun encodeToBase64Jpeg(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // Decoded once and reused — a strip bakes this into 3 frames per session.
    private val overlayBitmap by lazy { BitmapFactory.decodeResource(context.resources, R.drawable.temi_photo) }

    /** Burns the Temi cutout into [photo] itself (e.g. so each strip frame carries its own copy). */
    internal fun bakeOverlay(photo: Bitmap): Bitmap {
        val overlay = overlayBitmap
        val result = photo.copy(photo.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val targetHeight = result.height * PHOTOBOX_OVERLAY_HEIGHT_FRACTION
        val scale = targetHeight / overlay.height
        val targetWidth = overlay.width * scale
        val destRect = RectF(
            result.width - targetWidth,
            result.height - targetHeight,
            result.width.toFloat(),
            result.height.toFloat()
        )
        canvas.drawBitmap(overlay, null, destRect, null)
        return result
    }
}

internal fun extractDriveFolderId(link: String): String? {
    val trimmed = link.trim()
    if (trimmed.isEmpty()) return null

    val folderMatch = Regex("/folders/([a-zA-Z0-9_-]+)").find(trimmed)
    val idParamMatch = Regex("[?&]id=([a-zA-Z0-9_-]+)").find(trimmed)
    val looksLikeBareId = !trimmed.contains("/") && !trimmed.contains(" ")

    return folderMatch?.groupValues?.get(1)
        ?: idParamMatch?.groupValues?.get(1)
        ?: trimmed.takeIf { looksLikeBareId }
}
