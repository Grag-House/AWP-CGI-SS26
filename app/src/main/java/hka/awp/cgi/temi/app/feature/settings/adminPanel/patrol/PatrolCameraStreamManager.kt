package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class PatrolCameraStreamManager(
    private val context: Context,
    private val serverUrl: String
) {
    private val client = OkHttpClient()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var webSocket: WebSocket? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isStreaming = false

    private val _events = MutableSharedFlow<PatrolAnalysisEvent>()
    val events: SharedFlow<PatrolAnalysisEvent> = _events

    fun connect() {
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Timber.d("Patrol WebSocket verbunden: $serverUrl")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Timber.d("Patrol WebSocket Antwort: $text")
                    _events.tryEmit(parseEvent(text))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Timber.e(t, "Patrol WebSocket Fehler")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.d("Patrol WebSocket geschlossen: $code $reason")
                }
            }
        )
    }

    fun startStream() {
        if (isStreaming) return

        isStreaming = true
        Timber.d("Starte Patrol Kamera-Livestream")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            handleFrame(imageProxy)
                        }
                    }

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    ProcessLifecycleOwner.get(),
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageAnalysis
                )
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun stopStream() {
        if (!isStreaming) return

        Timber.d("Stoppe Patrol Kamera-Livestream")
        isStreaming = false

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                cameraProviderFuture.get().unbindAll()
                imageAnalysis = null
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun handleFrame(imageProxy: ImageProxy) {
        try {
            if (!isStreaming) return

            val jpegBytes = imageProxy.toJpegBytes(
                quality = JPEG_QUALITY
            )

            webSocket?.send(jpegBytes.toByteString())
        } catch (e: Exception) {
            Timber.e(e, "Frame konnte nicht gesendet werden")
        } finally {
            imageProxy.close()
        }
    }

    fun sendPatrolPointReached(location: String) {
        val payload = """
            {
              "type": "patrol_point_reached",
              "location": "$location"
            }
        """.trimIndent()

        webSocket?.send(payload)
    }

    fun disconnect() {
        stopStream()
        webSocket?.close(NORMAL_CLOSE_CODE, "Patrol stream closed")
        webSocket = null
        cameraExecutor.shutdown()
    }

    private fun parseEvent(raw: String): PatrolAnalysisEvent {
        return when {
            raw.contains("person_on_floor", ignoreCase = true) -> PatrolAnalysisEvent.PersonOnFloor
            raw.contains("person_ok", ignoreCase = true) -> PatrolAnalysisEvent.PersonOk
            raw.contains("no_person_detected", ignoreCase = true) -> PatrolAnalysisEvent.NoPersonDetected
            else -> PatrolAnalysisEvent.Unknown(raw)
        }
    }

    private companion object {
        private const val NORMAL_CLOSE_CODE = 1000
        private const val JPEG_QUALITY = 60
    }
}

private fun ImageProxy.toJpegBytes(quality: Int): ByteArray {
    val nv21 = yuv420ToNv21(this)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        width,
        height,
        null
    )

    val outputStream = ByteArrayOutputStream()

    yuvImage.compressToJpeg(
        Rect(0, 0, width, height),
        quality,
        outputStream
    )

    return outputStream.toByteArray()
}

private fun yuv420ToNv21(image: ImageProxy): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    return nv21
}

sealed interface PatrolAnalysisEvent {
    data object PersonOk : PatrolAnalysisEvent
    data object PersonOnFloor : PatrolAnalysisEvent
    data object NoPersonDetected : PatrolAnalysisEvent
    data class Unknown(val raw: String) : PatrolAnalysisEvent
}
