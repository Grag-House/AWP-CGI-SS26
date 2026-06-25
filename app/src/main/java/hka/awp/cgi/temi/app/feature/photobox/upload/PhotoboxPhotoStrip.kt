package hka.awp.cgi.temi.app.feature.photobox.upload

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

// Proportions for the 2x2 grid, derived from a reference layout (520x390 photos, 40px side
// margins, 40px top margin, 40px gaps between photos, 260px bottom branding area). Horizontal
// and vertical spacing are expressed separately since they scale against different photo
// dimensions (width vs. height).
private const val GRID_SIDE_MARGIN_RATIO = 40f / 520f
private const val GRID_HORIZONTAL_GAP_RATIO = 40f / 520f
private const val GRID_TOP_MARGIN_RATIO = 40f / 390f
private const val GRID_VERTICAL_GAP_RATIO = 40f / 390f
private const val GRID_BOTTOM_MARGIN_RATIO = 260f / 390f

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

/**
 * Arranges the given shots into a grid with [columns] columns (rows follow from the shot
 * count): side margins and a top margin around the grid, gaps between cells, and a blank area
 * below the grid for branding/text. See [GRID_SIDE_MARGIN_RATIO] and friends for the proportions.
 */
internal fun combinePhotoGrid(shots: List<Bitmap>, columns: Int): Bitmap {
    val width = shots.maxOf { it.width }
    val scaledShots = shots.map { shot ->
        if (shot.width == width) {
            shot
        } else {
            val scaledHeight = (shot.height.toFloat() * width / shot.width).roundToInt()
            shot.scale(width, scaledHeight)
        }
    }

    val cellHeight = scaledShots.maxOf { it.height }
    val rows = (scaledShots.size + columns - 1) / columns
    val sideMargin = (width * GRID_SIDE_MARGIN_RATIO).roundToInt()
    val horizontalGap = (width * GRID_HORIZONTAL_GAP_RATIO).roundToInt()
    val topMargin = (cellHeight * GRID_TOP_MARGIN_RATIO).roundToInt()
    val verticalGap = (cellHeight * GRID_VERTICAL_GAP_RATIO).roundToInt()
    val bottomMargin = (cellHeight * GRID_BOTTOM_MARGIN_RATIO).roundToInt()

    val gridWidth = columns * width + (columns - 1) * horizontalGap
    val gridHeight = rows * cellHeight + (rows - 1) * verticalGap
    val canvasWidth = gridWidth + sideMargin * 2
    val canvasHeight = topMargin + gridHeight + bottomMargin
    val grid = createBitmap(canvasWidth, canvasHeight)

    val canvas = Canvas(grid)
    canvas.drawColor(Color.WHITE)

    scaledShots.forEachIndexed { index, shot ->
        val x = sideMargin + (index % columns) * (width + horizontalGap)
        val y = topMargin + (index / columns) * (cellHeight + verticalGap)
        canvas.drawBitmap(shot, x.toFloat(), y.toFloat(), null)
    }
    // The bottomMargin below the last row stays blank/white — the canvas was already filled
    // white above.

    return grid
}
