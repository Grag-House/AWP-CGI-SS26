package hka.awp.cgi.temi.app.core.camera // Jetzt im Core-Paket!

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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class CameraStreamManager(
    private val context: Context,
    private val serverUrl: String,
    private val onMessageReceived: (String) -> Unit // Callback für eingehende Server-Nachrichten
                         ) {
    private val client = OkHttpClient()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var webSocket: WebSocket? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isStreaming = false

    // Verhindert mehrfache Verbindungsaufbaue während der Stream läuft
    @Volatile
    private var isWebSocketConnectingOrConnected = false

    // HINWEIS: Wird jetzt intern aufgerufen, wenn der erste Frame bereit ist!
    private fun connect() {
        if (webSocket != null) return

        val request = Request.Builder().url(serverUrl).build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Timber.d("WebSocket verbunden: $serverUrl")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onMessageReceived(text) // Weiterleiten nach außen
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Timber.e(t, "WebSocket Fehler - Setze Verbindungsstatus zurück")
                    // Bei Fehler zurücksetzen, damit beim nächsten Frame ein Reconnect versucht werden kann
                    isWebSocketConnectingOrConnected = false
                    this@CameraStreamManager.webSocket = null
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.d("WebSocket geschlossen: $code $reason")
                    isWebSocketConnectingOrConnected = false
                    this@CameraStreamManager.webSocket = null
                }
            }
                                       )
    }

    fun startStream() {
        if (isStreaming) return
        isStreaming = true
        isWebSocketConnectingOrConnected = false // Status zurücksetzen

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
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
                                         }, ContextCompat.getMainExecutor(context))
    }

    private fun handleFrame(imageProxy: ImageProxy) {
        try {
            if (!isStreaming) return

            // 1. Erst wenn wirklich Frames ankommen, starten wir den WebSocket
            if (!isWebSocketConnectingOrConnected) {
                isWebSocketConnectingOrConnected = true
                Timber.i("Erster Kamera-Frame empfangen! Starte WebSocket-Verbindung...")
                connect()
            }

            // 2. Nur senden, wenn der Socket auch tatsächlich bereit ist
            val currentWebSocket = webSocket
            if (currentWebSocket != null) {
                val jpegBytes = imageProxy.toJpegBytes(JPEG_QUALITY)
                currentWebSocket.send(jpegBytes.toByteString())
            }

        } catch (e: Exception) {
            Timber.e(e, "Frame konnte nicht gesendet werden")
        } finally {
            imageProxy.close()
        }
    }

    fun sendText(text: String) {
        webSocket?.send(text)
    }

    fun sendBytes(bytes: ByteArray) {
        webSocket?.send(bytes.toByteString())
    }

    fun stopStream() {
        if (!isStreaming) return
        isStreaming = false
        isWebSocketConnectingOrConnected = false

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
                                             cameraProviderFuture.get().unbindAll()
                                             imageAnalysis = null

                                             // Socket schließen, wenn der Stream stoppt
                                             webSocket?.close(1000, "Stream stopped")
                                             webSocket = null
                                         }, ContextCompat.getMainExecutor(context))
    }

    fun disconnect() {
        stopStream()
        cameraExecutor.shutdown()
    }

    private companion object {
        private const val JPEG_QUALITY = 60
    }
}

// Die Extension-Funktionen bleiben hier als private Hilfsfunktionen für die Konvertierung
private fun ImageProxy.toJpegBytes(quality: Int): ByteArray {
    val nv21 = yuv420ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val outputStream = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, outputStream)
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
