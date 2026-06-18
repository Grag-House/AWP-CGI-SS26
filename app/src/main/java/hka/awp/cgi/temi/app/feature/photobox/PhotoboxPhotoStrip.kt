package hka.awp.cgi.temi.app.feature.photobox

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.roundToInt

// Proportions derived from a reference strip layout (528x396 photos, 36px side margin,
// 100px top margin, 60px gaps between photos, 392px bottom branding area), expressed as
// ratios so spacing scales sensibly regardless of the camera's actual photo resolution.
private const val SIDE_MARGIN_RATIO = 36f / 528f
private const val TOP_MARGIN_RATIO = 100f / 396f
private const val GAP_RATIO = 60f / 396f
private const val BOTTOM_MARGIN_RATIO = 392f / 396f

/**
 * Stacks the given shots vertically into a classic photo booth strip: a side margin around
 * each photo, a top margin, gaps between photos, and a larger blank area below the last shot
 * for branding/text.
 */
internal fun combinePhotoStrip(shots: List<Bitmap>): Bitmap {
    val width = shots.maxOf { it.width }
    val scaledShots = shots.map { shot ->
        if (shot.width == width) {
            shot
        } else {
            val scaledHeight = (shot.height.toFloat() * width / shot.width).roundToInt()
            shot.scale(width, scaledHeight)
        }
    }

    val photoHeight = scaledShots.first().height
    val sideMargin = (width * SIDE_MARGIN_RATIO).roundToInt()
    val topMargin = (photoHeight * TOP_MARGIN_RATIO).roundToInt()
    val gap = (photoHeight * GAP_RATIO).roundToInt()
    val bottomMargin = (photoHeight * BOTTOM_MARGIN_RATIO).roundToInt()

    val totalHeight = topMargin +
        scaledShots.sumOf { it.height } +
        gap * (scaledShots.size - 1) +
        bottomMargin
    val stripWidth = width + sideMargin * 2
    val strip = createBitmap(stripWidth, totalHeight)

    val canvas = Canvas(strip)
    canvas.drawColor(Color.WHITE)

    var y = topMargin
    scaledShots.forEachIndexed { index, shot ->
        canvas.drawBitmap(shot, sideMargin.toFloat(), y.toFloat(), null)
        y += shot.height
        if (index < scaledShots.lastIndex) y += gap
    }
    // The bottomMargin below the last shot stays blank/white — the canvas was already filled
    // white above.

    return strip
}
