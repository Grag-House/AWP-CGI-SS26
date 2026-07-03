package hka.awp.cgi.temi.app.feature.patrol.overlay

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R

/**
 * An overlay displaying the live camera stream from the robot during a patrol.
 * * It includes navigation controls to exit the view or terminate the patrol session.
 *
 * @param videoFrame The latest [Bitmap] frame from the camera stream, or null if unavailable.
 * @param onBackClick Callback invoked when the back button is clicked.
 * @param onStopPatrol Callback invoked when the stop patrol button is clicked.
 */
@Composable
fun PatrolStreamOverlay(
    videoFrame: Bitmap?,
    onBackClick: () -> Unit,
    onStopPatrol: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Display camera feed
        videoFrame?.let { frame ->
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = stringResource(R.string.patrol_stream_content_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Control buttons overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBackClick) {
                Text(stringResource(R.string.patrol_stream_back_button))
            }

            Button(onClick = onStopPatrol) {
                Text(stringResource(R.string.patrol_stream_stop_button))
            }
        }
    }
}
