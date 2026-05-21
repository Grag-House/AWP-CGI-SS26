package hka.awp.cgi.temi.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_BATTERY_LEVEL = 100
private const val FULL_BATTERY_THRESHOLD = 95

/**
 * Compose component that renders the vertical battery icon.
 *
 * The indicator dynamically updates its fill level based on the [level] percentage
 * and displays a charging bolt icon when [isCharging] is true.
 *
 * @param level The battery percentage (0-100). If null, an empty outline with "--" is shown.
 * @param isCharging Boolean flag to determine if the charging bolt overlay should be displayed.
 * @param modifier [Modifier] to be applied to the battery indicator.
 */
@Composable
fun BatteryIndicator(level: Int?, isCharging: Boolean, modifier: Modifier = Modifier) {
    val safeLevel = level?.coerceIn(0, MAX_BATTERY_LEVEL)

    val batteryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 15.dp, height = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val boltPath = remember { Path() }

            Canvas(modifier = Modifier.matchParentSize()) {
                val nubHeight = 4.dp.toPx()
                val nubWidth = 9.dp.toPx()
                val nubRadius = 1.5.dp.toPx()

                val bodyTop = nubHeight - 0.5.dp.toPx()
                val bodyWidth = 15.dp.toPx()
                val bodyHeight = size.height - bodyTop
                val bodyLeft = (size.width - bodyWidth) / 2f

                val radius = 4.dp.toPx()
                val strokeWidth = 2.dp.toPx()

                val fillPercent = safeLevel?.div(MAX_BATTERY_LEVEL.toFloat()) ?: 0f
                val fillHeight = bodyHeight * fillPercent
                val fillTop = bodyTop + bodyHeight - fillHeight

                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset((size.width - nubWidth) / 2f, 0f),
                    size = Size(nubWidth, nubHeight),
                    cornerRadius = CornerRadius(nubRadius, nubRadius)
                )

                if (safeLevel == null) {
                    drawRoundRect(
                        color = batteryColor,
                        topLeft = Offset(bodyLeft, bodyTop),
                        size = Size(bodyWidth, bodyHeight),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = strokeWidth)
                    )
                } else if (safeLevel >= FULL_BATTERY_THRESHOLD) {
                    drawRoundRect(
                        color = batteryColor,
                        topLeft = Offset(bodyLeft, bodyTop),
                        size = Size(bodyWidth, bodyHeight),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                } else {
                    drawRoundRect(
                        color = batteryColor,
                        topLeft = Offset(bodyLeft, bodyTop),
                        size = Size(bodyWidth, bodyHeight),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = strokeWidth)
                    )

                    if (fillHeight > 0f) {
                        drawRoundRect(
                            color = batteryColor,
                            topLeft = Offset(bodyLeft, fillTop),
                            size = Size(bodyWidth, fillHeight),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                }

                if (isCharging) {
                    boltPath.reset()
                    boltPath.buildChargingBolt(
                        size = size,
                        bodyTop = bodyTop,
                        bodyHeight = bodyHeight
                    )

                    drawPath(
                        path = boltPath,
                        color = batteryColor
                    )
                }
            }
        }

        if (!isCharging) {
            Text(
                text = safeLevel?.let { "$it%" } ?: "--%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

// extension function to build the charging bolt
@Suppress("MagicNumber")
private fun Path.buildChargingBolt(size: Size, bodyTop: Float, bodyHeight: Float) {
    moveTo(size.width * 0.60f, bodyTop + bodyHeight * 0.22f)
    lineTo(size.width * 0.40f, bodyTop + bodyHeight * 0.52f)
    lineTo(size.width * 0.53f, bodyTop + bodyHeight * 0.52f)
    lineTo(size.width * 0.42f, bodyTop + bodyHeight * 0.82f)
    lineTo(size.width * 0.66f, bodyTop + bodyHeight * 0.43f)
    lineTo(size.width * 0.52f, bodyTop + bodyHeight * 0.43f)
    close()
}
