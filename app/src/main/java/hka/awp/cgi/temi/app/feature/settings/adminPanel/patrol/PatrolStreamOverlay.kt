package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun PatrolStreamOverlay(
    videoFrame: Bitmap?,
    onBackClick: () -> Unit,
    onStopPatrol: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        videoFrame?.let { frame ->
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "Patrol Stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBackClick) {
                Text("Zurück")
            }

            Button(onClick = onStopPatrol) {
                Text("Patruille abbrechen")
            }
        }
    }
}
