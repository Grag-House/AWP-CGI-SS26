package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import android.content.Context
import hka.awp.cgi.temi.app.core.camera.CameraStreamManager
import hka.awp.cgi.temi.app.feature.stream.PatrolAnalysisEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class PatrolCameraStreamManager(context: Context, serverUrl: String) {

    private val _events = MutableSharedFlow<PatrolAnalysisEvent>()
    val events: SharedFlow<PatrolAnalysisEvent> = _events

    // Instanziierung des generischen Managers mit Übergabe des Nachrichten-Parsers
    private val baseStreamManager = CameraStreamManager(context, serverUrl) { text ->
        _events.tryEmit(parseEvent(text))
    }

    fun connect() = baseStreamManager.connect()
    fun startStream() = baseStreamManager.startStream()
    fun stopStream() = baseStreamManager.stopStream()
    fun disconnect() = baseStreamManager.disconnect()

    // Das spezifische Patrol-Event wird hier gekapselt
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
