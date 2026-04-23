package hka.awp.temi_cgi_app.ui.components

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
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clickable { onClick() }, shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (customIcon == null) {
                    Icon(
                        icon ?: Icons.Rounded.ImageNotSupported,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    customIcon()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)

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