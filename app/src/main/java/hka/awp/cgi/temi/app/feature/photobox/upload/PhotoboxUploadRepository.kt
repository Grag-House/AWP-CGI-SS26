package hka.awp.cgi.temi.app.feature.photobox.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.util.Base64
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.photobox.PHOTOBOX_GRID_BANNER_WIDTH_FRACTION
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxBanner
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxMode
import hka.awp.cgi.temi.app.feature.photobox.TemiOverlayPosition
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

/** [position] and [banner] of the Temi cutout/branding banner plus the capture [mode], grouped
 * since `bakeOverlay` and `bakeBanner` are needed together wherever a final photo gets decorated,
 * and both need [mode] to pick the right banner asset/size. */
internal data class PhotoboxOverlayOptions(
    val position: TemiOverlayPosition,
    val banner: PhotoboxBanner?,
    val mode: PhotoboxMode
)

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

    /**
     * Uploads a photo that's already in its final form (overlay/banner baked in, strip combined,
     * if applicable) — used both by [PhotoboxSessionFinalizer.upload] and by
     * [PhotoboxUploadWorker] when retrying a photo that was cached to disk after a failed first
     * attempt.
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

    /**
     * Burns the Temi cutout into [photo] itself (e.g. so each strip frame carries its own copy).
     * If [PhotoboxOverlayOptions.banner] is set, Temi is shifted up to sit above the banner
     * instead of overlapping it — the banner is assumed to already be baked into [photo] at its
     * default (non-grid) size, which is the only case where Temi and the banner could collide
     * (strip/grid bake Temi per-frame, well before the banner is added to the combined image).
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

    // Keyed by drawable resource rather than by PhotoboxBanner directly since each banner has a
    // separate asset per mode (see PhotoboxBanner.drawableRes) — decoded on first use and reused
    // for the rest of the process, since a session can bake more than one photo.
    private val bannerBitmaps = mutableMapOf<Int, Bitmap>()

    private fun bannerBitmap(drawableRes: Int): Bitmap =
        bannerBitmaps.getOrPut(drawableRes) { BitmapFactory.decodeResource(context.resources, drawableRes) }

    /**
     * Draws [banner] centered against the bottom edge. For strip/grid the banner is stretched to
     * fill the white branding area exactly — height derived from the composite's own dimensions
     * using the same ratios as [combinePhotoStrip]/[combinePhotoGrid], so it covers the area
     * regardless of the banner asset's own aspect ratio. For STANDARD the banner has no fixed
     * white area, so it is scaled by width while keeping its aspect ratio as before.
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

    // Derives the exact white branding area height from the composite height, inverting the
    // layout ratios used by combinePhotoStrip/combinePhotoGrid.
    private fun whiteAreaHeight(compositeHeight: Int, mode: PhotoboxMode): Float = when (mode) {
        PhotoboxMode.STRIP -> compositeHeight * STRIP_BANNER_HEIGHT_FRACTION
        PhotoboxMode.STRIP_1X4 -> compositeHeight * STRIP_1X4_BANNER_HEIGHT_FRACTION
        PhotoboxMode.GRID_2X2 -> compositeHeight * GRID_2X2_BANNER_HEIGHT_FRACTION
        PhotoboxMode.STANDARD -> compositeHeight.toFloat()
    }

    // Only ever called with a non-null banner while baking the Temi overlay for a standalone
    // photo at upload time (see PhotoboxSessionFinalizer) — strip/grid bake Temi per-frame, before
    // the banner exists, so mode here is always STANDARD in practice.
    private fun bannerHeightPx(photoWidth: Int, banner: PhotoboxBanner, mode: PhotoboxMode): Float {
        val bannerBitmap = bannerBitmap(banner.drawableRes(mode))
        return bannerBitmap.height * (photoWidth.toFloat() / bannerBitmap.width)
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
