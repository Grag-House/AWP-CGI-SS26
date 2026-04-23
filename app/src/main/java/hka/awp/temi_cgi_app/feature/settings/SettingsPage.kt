package hka.awp.temi_cgi_app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onItemClick: (SettingsItem) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 32.dp, top = 32.dp, end = 32.dp)
    ) {
        TopStatusBar()

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Einstellungen",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = buildAnnotatedString {
                    append("Temi ")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("Einstellungen")
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            settingsItems.forEach {
                SettingsOptionCard(
                    title = it.title,
                    subtitle = it.subtitle,
                    icon = it.icon,
                    onClick = { onItemClick(it) }
                )
            }
        }
    }
}

@Composable
private fun TopStatusBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Wifi,
            contentDescription = "WLAN",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Default.BatteryFull,
            contentDescription = "Batterie",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "16:29",
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            Icons.Default.Notifications,
            contentDescription = "Benachrichtigungen",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}