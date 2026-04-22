package hka.awp.temi_cgi_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Sidebar(
    selectedMenu: String,
    onMenuSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Logo / Header
        Text(
            text = "CGI",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Funktionen",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Menü-Buttons
        SidebarButton("Hauptmenü", Icons.Default.Home, selectedMenu, onMenuSelected) // [cite: 4]
        SidebarButton("Webserver", Icons.Default.Storage, selectedMenu, onMenuSelected) // [cite: 6]
        SidebarButton("Wetter", Icons.Default.Cloud, selectedMenu, onMenuSelected) // [cite: 7]
        SidebarButton(
            "Navigation",
            Icons.Default.Navigation,
            selectedMenu,
            onMenuSelected
        ) // [cite: 8]
        SidebarButton("Modus", Icons.Default.ToggleOn, selectedMenu, onMenuSelected) // [cite: 15]
        SidebarButton(
            "Einstellungen",
            Icons.Default.Settings,
            selectedMenu,
            onMenuSelected
        ) // [cite: 16]

        // Drückt den Hilfe-Button an das untere Ende
        Spacer(modifier = Modifier.weight(1f))

        // Hilfe Button am Boden
        Button(
            onClick = { /* Hilfe Aktion */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = Color(0xFF7B7B7B)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("Hilfe", modifier = Modifier.weight(1f), color = Color(0xFF7B7B7B))
        }
    }
}

@Composable
fun SidebarButton(
    title: String,
    icon: ImageVector,
    selectedMenu: String,
    onMenuSelected: (String) -> Unit
) {
    val isSelected = title == selectedMenu
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color(0xFF7B7B7B)

    Button(
        onClick = { onMenuSelected(title) },
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}