package hka.awp.cgi.temi.app.feature.patrol

import android.content.Context
import android.graphics.Bitmap
import hka.awp.cgi.temi.app.feature.stream.CameraStreamManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the camera video stream and real-time messaging specifically for patrol operations.
 *
 * This class acts as a specialized wrapper around the generic [CameraStreamManager],
 * tailoring its streaming and communication capabilities to fit patrol-specific workflows,
 * such as reporting when a patrol point has been successfully reached.
 *
 * @property context The Android context required for system and camera services.
 * @property serverUrl The backend or WebSocket server URL where the stream and messages are sent.
 */
class PatrolCameraStreamManager(
    context: Context,
    serverUrl: String
) {

    private val baseStreamManager = CameraStreamManager(
        context = context,
        serverUrl = serverUrl
    )

    val videoFrame: StateFlow<Bitmap?> = baseStreamManager.processedBitmap

    val textMessages = baseStreamManager.textMessages

    fun startStream() = baseStreamManager.startStream()

    fun stopStream() {
        baseStreamManager.disconnect()
    }

    fun sendPatrolPointReached(location: String) {
        val payload = """
            {
              "type": "patrol_point_reached",
              "location": "$location"
            }
        """.trimIndent()

        baseStreamManager.sendText(payload)
    }
}
