package hka.awp.temi_cgi_app

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
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        // Statusleiste (WLAN, Batterie, Uhrzeit)
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
            Text(text = "16:29", color = MaterialTheme.colorScheme.primary) // [cite: 27]
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Benachrichtigungen",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Begrüßungstext mit formatiertem Namen
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "Roboter",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = buildAnnotatedString {
                    append("Hey, Ich bin ")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("Temi!")
                    }
                    append(" Wie kann ich\ndir helfen?")
                }, //
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Grid für die Karten
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // 3 Spalten nebeneinander
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                DashboardCard(
                    "Webserver",
                    "Rufe den Webserver auf",
                    Icons.Default.Storage,
                    "Active 10.0.0.1"
                ) // [cite: 9, 10, 18]
            }
            item {
                DashboardCard(
                    "Wetter",
                    "Wettervorhersagen für den\naktuellen Standort",
                    Icons.Default.Cloud,
                    "21°C",
                    isTemp = true
                ) // [cite: 11, 12, 19]
            }
            item {
                DashboardCard(
                    "Navigation",
                    "Sag mir wo ich dich\nhinbringen soll.",
                    Icons.Default.Navigation,
                    "FASTEST ROUTE"
                ) // [cite: 13, 14, 26]
            }
            item {
                DashboardCard(
                    "Modus",
                    "Ändere den Modus",
                    Icons.Default.ToggleOn,
                    "SHOWROOM MODE",
                    overline = "Aktueller Modus"
                ) // [cite: 20, 21, 24, 25]
            }
            item {
                DashboardCard(
                    "Einstellungen",
                    "Routen, Patroullien etc.",
                    Icons.Default.Settings,
                    onClick = onOpenSettings
                )
            }
        }
    }
}