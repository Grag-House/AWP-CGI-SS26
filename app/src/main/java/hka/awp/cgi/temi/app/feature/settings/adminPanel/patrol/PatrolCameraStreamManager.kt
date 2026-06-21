package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import android.content.Context
import android.graphics.Bitmap
import hka.awp.cgi.temi.app.core.camera.CameraStreamManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class PatrolCameraStreamManager(context: Context, serverUrl: String) {

    private val _events = MutableSharedFlow<PatrolAnalysisEvent>()
    val events: SharedFlow<PatrolAnalysisEvent> = _events

    private val _videoFrame = MutableStateFlow<Bitmap?>(null)
    val videoFrame: StateFlow<Bitmap?> = _videoFrame

    private val baseStreamManager = CameraStreamManager(
        context = context,
        serverUrl = serverUrl,
        onStringMessageReceived = { text ->
            _events.tryEmit(parseEvent(text))
        },
        onByteMessageReceived = { bitmap ->
            if (bitmap != null) {
                _videoFrame.value = bitmap
            }
        }
    )

    fun startStream() = baseStreamManager.startStream()
    fun stopStream() {
        baseStreamManager.stopStream()
        _videoFrame.value = null
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

    private fun parseEvent(raw: String): PatrolAnalysisEvent {
        return when {
            raw.contains("person_on_floor", ignoreCase = true) -> PatrolAnalysisEvent.PersonOnFloor
            raw.contains("person_ok", ignoreCase = true) -> PatrolAnalysisEvent.PersonOk
            raw.contains("no_person_detected", ignoreCase = true) -> PatrolAnalysisEvent.NoPersonDetected
            else -> PatrolAnalysisEvent.Unknown(raw)
        }
    }
}

sealed interface PatrolAnalysisEvent {
    data object PersonOk : PatrolAnalysisEvent
    data object PersonOnFloor : PatrolAnalysisEvent
    data object NoPersonDetected : PatrolAnalysisEvent
    data class Unknown(val raw: String) : PatrolAnalysisEvent
}
