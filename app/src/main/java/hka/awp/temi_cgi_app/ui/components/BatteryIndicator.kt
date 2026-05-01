package hka.awp.temi_cgi_app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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

@Composable
fun BatteryIndicator(
    level: Int?, isCharging: Boolean, modifier: Modifier = Modifier
) {
    val safeLevel = level?.coerceIn(0, 100)
    val batteryColor = MaterialTheme.colorScheme.primary
    val textOnFill = MaterialTheme.colorScheme.onPrimary
    val textOnEmpty = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(width = 15.dp, height = 24.dp), contentAlignment = Alignment.Center
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

            val fillPercent = safeLevel?.div(100f) ?: 0f
            val fillHeight = bodyHeight * fillPercent
            val fillTop = bodyTop + bodyHeight - fillHeight

            // Nub top
            drawRoundRect(
                color = batteryColor,
                topLeft = Offset((size.width - nubWidth) / 2f, 0f),
                size = Size(nubWidth, nubHeight),
                cornerRadius = CornerRadius(nubRadius, nubRadius)
            )

            if (safeLevel == null) {
                // outline in case of no data
                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = strokeWidth)
                )
            } else if (safeLevel >= 95) {
                // just like the material icon, completely filled
                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
            } else {
                // outline
                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = strokeWidth)
                )

                // filling (bottom-up)
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
                // reset the path to clear the old one from the heap
                boltPath.reset()
                // bolt icon
                boltPath.buildChargingBolt(size, bodyTop, bodyHeight)
                drawPath(
                    path = boltPath, color = batteryColor
                )
            }
        }

        if (!isCharging) {
            Text(
                text = when (safeLevel) {
                    null -> "--"
                    100 -> ""
                    else -> safeLevel.toString()
                }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = when {
                    safeLevel == null -> textOnEmpty
                    safeLevel >= 45 -> textOnFill
                    else -> textOnEmpty
                }
            )
        }
    }
}

// extension function to build the charging bolt
private fun Path.buildChargingBolt(size: Size, bodyTop: Float, bodyHeight: Float) {
    moveTo(size.width * 0.60f, bodyTop + bodyHeight * 0.22f)
    lineTo(size.width * 0.40f, bodyTop + bodyHeight * 0.52f)
    lineTo(size.width * 0.53f, bodyTop + bodyHeight * 0.52f)
    lineTo(size.width * 0.42f, bodyTop + bodyHeight * 0.82f)
    lineTo(size.width * 0.66f, bodyTop + bodyHeight * 0.43f)
    lineTo(size.width * 0.52f, bodyTop + bodyHeight * 0.43f)
    close()
}