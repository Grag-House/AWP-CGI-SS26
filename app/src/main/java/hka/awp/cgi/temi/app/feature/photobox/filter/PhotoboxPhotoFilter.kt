package hka.awp.cgi.temi.app.feature.photobox.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

// Each matrix is a 4x5 row-major color matrix with no translation column (kept at 0), so the
// same FloatArray works unchanged for both the live Compose preview (Color components in 0..1)
// and android.graphics.ColorMatrixColorFilter on the final Bitmap (components in 0..255) — pure
// channel-mixing is scale-invariant, only a non-zero offset would need separate units.
// These are standard color matrix coefficients, not arbitrary numbers that need named constants.
@Suppress("MagicNumber")
private val GRAYSCALE_MATRIX = floatArrayOf(
    0.33f, 0.59f, 0.11f, 0f, 0f,
    0.33f, 0.59f, 0.11f, 0f, 0f,
    0.33f, 0.59f, 0.11f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

@Suppress("MagicNumber")
private val SEPIA_MATRIX = floatArrayOf(
    0.393f, 0.769f, 0.189f, 0f, 0f,
    0.349f, 0.686f, 0.168f, 0f, 0f,
    0.272f, 0.534f, 0.131f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

@Suppress("MagicNumber")
private val VINTAGE_MATRIX = floatArrayOf(
    0.9f, 0.5f, 0.1f, 0f, 0f,
    0.3f, 0.8f, 0.1f, 0f, 0f,
    0.2f, 0.3f, 0.5f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

/** A post-capture color filter for Photobox photos. [matrix] is null for [NONE] so callers can skip work entirely. */
enum class PhotoboxPhotoFilter(internal val matrix: FloatArray?) {
    NONE(null),
    GRAYSCALE(GRAYSCALE_MATRIX),
    SEPIA(SEPIA_MATRIX),
    VINTAGE(VINTAGE_MATRIX)
}

/** Cheap, GPU-composited preview of [this] filter — used so the live preview updates instantly
 * as the user taps through filters. */
fun PhotoboxPhotoFilter.toComposeColorFilter(): ColorFilter? =
    matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }

/** Burns [this] filter into [bitmap] itself — only called once, right before upload. */
fun PhotoboxPhotoFilter.bake(bitmap: Bitmap): Bitmap {
    val colorMatrix = matrix ?: return bitmap
    val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
    Canvas(result).drawBitmap(bitmap, 0f, 0f, paint)
    return result
}
