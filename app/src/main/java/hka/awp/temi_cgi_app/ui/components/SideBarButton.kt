package hka.awp.temi_cgi_app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.temi_cgi_app.ui.shell.Screen

@Composable
fun SidebarButton(isExpanded: Boolean, screen: Screen, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color(0xFF7B7B7B)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp)
            .height(48.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (isExpanded) {
                if (!screen.isCustomIcon) {
                    Icon(
                        painter = if (screen.icon != null) {
                            rememberVectorPainter(screen.icon)
                        } else if (screen.iconRes != null) {
                            painterResource(id = screen.iconRes)
                        } else {
                            rememberVectorPainter(Icons.Rounded.ImageNotSupported)
                        }, contentDescription = screen.contentDescription
                    )
                } else {
                    ModeIcon()
                }
                Spacer(modifier = Modifier.width(12.dp))

                Text(text = stringResource(id = screen.title), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
