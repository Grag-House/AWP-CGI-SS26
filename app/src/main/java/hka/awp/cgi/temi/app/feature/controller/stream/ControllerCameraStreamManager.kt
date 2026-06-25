package hka.awp.cgi.temi.app.feature.controller.stream

import android.content.Context
import hka.awp.cgi.temi.app.feature.stream.CameraStreamManager

class ControllerCameraStreamManager(context: Context, serverUrl: String) {
    private val baseStreamManager = CameraStreamManager(
        context = context,
        serverUrl = serverUrl
    )

    fun startLiveView() {
        baseStreamManager.startStream()
    }

    fun stopLiveView() {
        baseStreamManager.stopStream()
        baseStreamManager.disconnect()
    }
}
