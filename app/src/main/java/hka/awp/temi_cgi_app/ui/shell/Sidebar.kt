package hka.awp.temi_cgi_app.ui.shell

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.temi_cgi_app.ui.components.SidebarButton
import hka.awp.temi_cgi_app.ui.shell.Screen.Companion.navScreens

@Composable
fun Sidebar(
    isExpanded: Boolean,
    selectedRoute: String,
    onRouteSelected: (Screen) -> Unit,
    onSidebarToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 250.dp else 80.dp, label = "sidebar_width_animation"
    )

    if (isExpanded) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .width(sidebarWidth)
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CGI",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = { onSidebarToggle() }) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        "button to toggle or expand the side panel",
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Funktionen",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // initialize each of the screens in the side panel
            navScreens.forEach {
                SidebarButton(
                    isExpanded = isExpanded,
                    screen = it,
                    isSelected = selectedRoute == it.route,
                    onClick = { onRouteSelected(it) })
            }

            Spacer(modifier = Modifier.weight(1f))

            //Help button
            Button(
                onClick = {/* //TODO add navigation later on */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        12.dp
                    )
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
    // if the side panel is toggled off, we have to show the menu button so it can still be toggled
    else {
        IconButton(onClick = { onSidebarToggle() }) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                "button to toggle or expand the side panel",
            )
        }
    }
}