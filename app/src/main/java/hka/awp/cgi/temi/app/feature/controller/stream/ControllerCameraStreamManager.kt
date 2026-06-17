package hka.awp.cgi.temi.app.feature.controller.stream

import android.content.Context
import hka.awp.cgi.temi.app.core.camera.CameraStreamManager
import kotlinx.coroutines.delay

class ControllerCameraStreamManager(context: Context, serverUrl: String) {

    private val baseStreamManager = CameraStreamManager(context, serverUrl) { _ ->
    }

    fun startLiveView() {
        baseStreamManager.startStream()
    }

    fun stopLiveView() {
        baseStreamManager.stopStream()
        baseStreamManager.disconnect()
    }
}
