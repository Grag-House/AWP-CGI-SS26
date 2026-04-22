package hka.awp.temi_cgi_app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TemiDashboardScreen() {
    // Zustand für das ausgewählte Menüelement, um die Sidebar interaktiv zu machen
    var selectedMenu by remember { mutableStateOf("Hauptmenü") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Linke Seitenleiste
            Sidebar(
                selectedMenu = selectedMenu,
                onMenuSelected = { selectedMenu = it },
                modifier = Modifier.width(260.dp)
            )
            // Einstellungen öffnen
            when (selectedMenu) {
                "Einstellungen" -> {
                    SettingsContent(
                        modifier = Modifier.weight(1f)
                    )
                }

            // Rechter Hauptbereich
                else -> {
                    MainContent(
                        modifier = Modifier.weight(1f),
                        onOpenSettings = {
                            selectedMenu = "Einstellungen"
                        }
                    )
                }
            }
        }
    }
}