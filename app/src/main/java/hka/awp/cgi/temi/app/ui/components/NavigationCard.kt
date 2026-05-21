package hka.awp.cgi.temi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A composable component that displays a card used for navigation within the application.
 *
 * This card provides a visual entry point for users to trigger specific actions or
 * navigate to different screens, typically featuring an icon and a descriptive label.
 *
 * @param label The text to be displayed on the card, describing the navigation destination.
 * @param icon The icon to be displayed alongside the label.
 */
@Composable
fun NavigationCard(label: String, icon: ImageVector, onClick: () -> Unit) {
    @Suppress("MagicNumber")
    OutlinedCard(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
                             )
                ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
              ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(66.dp),
                tint = MaterialTheme.colorScheme.primary
                )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
                )
        }
    }
}
