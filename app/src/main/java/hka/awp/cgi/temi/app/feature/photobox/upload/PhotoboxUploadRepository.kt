package hka.awp.cgi.temi.app.feature.photobox.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.util.Base64
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.data.repository.PhotoboxConfigRepository
import hka.awp.cgi.temi.app.feature.photobox.PHOTOBOX_GRID_BANNER_WIDTH_FRACTION
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxBanner
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxMode
import hka.awp.cgi.temi.app.feature.photobox.TemiOverlayPosition
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

// Kept in sync with PhotoboxScreen's TemiOverlayImage
internal const val PHOTOBOX_OVERLAY_HEIGHT_FRACTION = 0.68f

/**
 * Result of a successful upload.
 *
 * @property viewUrl The URL to view the uploaded photo.
 * @property expiresAtMillis The timestamp when the view URL expires.
 */
data class PhotoboxUploadResult(val viewUrl: String, val expiresAtMillis: Long)

/**
 * Groups overlay options like position, banner, and capture mode.
 */
internal data class PhotoboxOverlayOptions(
    val position: TemiOverlayPosition,
    val banner: PhotoboxBanner?,
    val mode: PhotoboxMode
)

/**
 * Uploads Photobox photos to a Google Drive folder.
 * Fresh configuration is read from [PhotoboxConfigRepository] for every upload.
 *
 * @property context Application context for resource access.
 * @property photoboxConfigRepository Repository for retrieving upload URL and folder link.
 */
class PhotoboxUploadRepository(
    private val context: Context,
    client: OkHttpClient,
    private val photoboxConfigRepository: PhotoboxConfigRepository
) {

    private val uploadClient = client.newBuilder()
        .connectTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Uploads a final decorated photo to Google Drive.
     *
     * @param finalBitmap The bitmap to upload.
     * @return A [Result] containing the upload result or an error.
     */
    suspend fun uploadFinalPhoto(finalBitmap: Bitmap): Result<PhotoboxUploadResult> = withContext(Dispatchers.IO) {
        try {
            val webhookUrl = photoboxConfigRepository.driveUploadUrl.first()
            val folderLink = photoboxConfigRepository.driveFolderLink.first()
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

    private val overlayBitmap by lazy { BitmapFactory.decodeResource(context.resources, R.drawable.temi_photo) }

    /**
     * Burns the Temi cutout into [photo].
     */
    internal fun bakeOverlay(photo: Bitmap, overlayOptions: PhotoboxOverlayOptions): Bitmap {
        val overlay = overlayBitmap
        val result = photo.copy(photo.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val bannerHeight = overlayOptions.banner?.let {
            bannerHeightPx(result.width, it, overlayOptions.mode)
        } ?: 0f
        val bottom = result.height - bannerHeight
        val targetHeight = result.height * PHOTOBOX_OVERLAY_HEIGHT_FRACTION
        val scale = targetHeight / overlay.height
        val targetWidth = overlay.width * scale
        val left = when (overlayOptions.position) {
            TemiOverlayPosition.LEFT -> 0f
            TemiOverlayPosition.CENTER -> (result.width - targetWidth) / 2f
            TemiOverlayPosition.RIGHT -> result.width - targetWidth
        }
        val destRect = RectF(left, bottom - targetHeight, left + targetWidth, bottom)
        canvas.drawBitmap(overlay, null, destRect, null)
        return result
    }

    private val bannerBitmaps = mutableMapOf<Int, Bitmap>()

    private fun bannerBitmap(drawableRes: Int): Bitmap =
        bannerBitmaps.getOrPut(drawableRes) { BitmapFactory.decodeResource(context.resources, drawableRes) }

    /**
     * Draws [banner] centered against the bottom edge.
     */
    internal fun bakeBanner(photo: Bitmap, banner: PhotoboxBanner, mode: PhotoboxMode): Bitmap {
        val bannerBitmap = bannerBitmap(banner.drawableRes(mode))
        val result = photo.copy(photo.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val widthFraction = if (mode == PhotoboxMode.GRID_2X2) PHOTOBOX_GRID_BANNER_WIDTH_FRACTION else 1f
        val targetWidth = result.width * widthFraction
        val bannerHeight = if (mode == PhotoboxMode.STANDARD) {
            bannerBitmap.height * (targetWidth / bannerBitmap.width)
        } else {
            whiteAreaHeight(result.height, mode)
        }
        val left = (result.width - targetWidth) / 2f
        val destRect = RectF(left, result.height - bannerHeight, left + targetWidth, result.height.toFloat())
        canvas.drawBitmap(bannerBitmap, null, destRect, null)
        return result
    }

    private fun whiteAreaHeight(compositeHeight: Int, mode: PhotoboxMode): Float = when (mode) {
        PhotoboxMode.STRIP -> compositeHeight * STRIP_BANNER_HEIGHT_FRACTION
        PhotoboxMode.STRIP_1X4 -> compositeHeight * STRIP_1X4_BANNER_HEIGHT_FRACTION
        PhotoboxMode.GRID_2X2 -> compositeHeight * GRID_2X2_BANNER_HEIGHT_FRACTION
        PhotoboxMode.STANDARD -> compositeHeight.toFloat()
    }

    private fun bannerHeightPx(photoWidth: Int, banner: PhotoboxBanner, mode: PhotoboxMode): Float {
        val bannerBitmap = bannerBitmap(banner.drawableRes(mode))
        return bannerBitmap.height * (photoWidth.toFloat() / bannerBitmap.width)
    }
}

/**
 * Extracts the Google Drive folder ID from a link.
 *
 * @param link The full Google Drive folder link.
 * @return The extracted folder ID, or null if invalid.
 */
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
