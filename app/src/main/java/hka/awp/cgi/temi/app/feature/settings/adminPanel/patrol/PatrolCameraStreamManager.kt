package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import android.content.Context
import android.graphics.Bitmap
import hka.awp.cgi.temi.app.feature.stream.CameraStreamManager
import kotlinx.coroutines.flow.StateFlow
import hka.awp.cgi.temi.app.feature.stream.CameraStreamManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

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
        baseStreamManager.stopStream()
    }

    fun disconnect() = baseStreamManager.disconnect()

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

sealed interface PatrolAnalysisEvent {
    data object PersonOk : PatrolAnalysisEvent
    data object PersonOnFloor : PatrolAnalysisEvent
    data object NoPersonDetected : PatrolAnalysisEvent
    data class Unknown(val raw: String) : PatrolAnalysisEvent
}
