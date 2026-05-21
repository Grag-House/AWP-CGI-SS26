package hka.awp.cgi.temi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A customizable dashboard card component used to display information summaries or navigation triggers.
 *
 * @param title The primary heading text displayed on the card.
 * @param subtitle A secondary description or supporting text.
 * @param icon An optional [ImageVector] to be displayed in the icon box. Defaults to a placeholder
 * if [customIcon] is also null.
 * @param bottomText Optional text displayed at the bottom of the card.
 * @param overline Optional small label text displayed directly above the [bottomText].
 * @param isTemp If true, applies a larger headline style to the [bottomText], typically used for
 * numerical readings like temperature.
 * @param customIcon An optional composable slot to provide a custom icon or graphic, overriding the [icon] parameter.
 * @param onClick Callback to be executed when the card is clicked.
 */
@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    bottomText: String? = null,
    overline: String? = null,
    isTemp: Boolean = false,
    customIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    @Suppress("MagicNumber")
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (customIcon == null) {
                    Icon(
                        icon ?: Icons.Rounded.ImageNotSupported,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    customIcon()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.weight(1f))

            if (overline != null) {
                Text(text = overline, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
            if (bottomText != null) {
                if (isTemp) {
                    Text(
                        text = bottomText,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                } else {
                    Text(
                        text = bottomText,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
