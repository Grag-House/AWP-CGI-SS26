package hka.awp.cgi.temi.app.feature.stream // Jetzt im Core-Paket!

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
) {
    private val client = OkHttpClient()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var webSocket: WebSocket? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isStreaming = false
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var lastFrameSentAt = 0L
    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()
    private val _textMessages = MutableSharedFlow<String>(
        extraBufferCapacity = 20
    )
    val textMessages: SharedFlow<String> = _textMessages.asSharedFlow()

    // Verhindert mehrfache Verbindungsaufbaue während der Stream läuft
    @Volatile
    private var isWebSocketConnectingOrConnected = false

    // HINWEIS: Wird jetzt intern aufgerufen, wenn der erste Frame bereit ist!
    private fun connect() {
        if (webSocket != null || isWebSocketConnectingOrConnected) return

        isWebSocketConnectingOrConnected = true

        val request = Request.Builder().url(serverUrl).build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Timber.d("WebSocket verbunden: $serverUrl")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Timber.d("Analyse: $text")
                    _textMessages.tryEmit(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                    val kotlinBytes = bytes.toByteArray()
                    val bitmap = BitmapFactory.decodeByteArray(kotlinBytes, 0, kotlinBytes.size)

                    if (bitmap != null) {
                        mainExecutor.execute {
                            _processedBitmap.value = bitmap
                        }
                    } else {
                        Timber.w("Empfangenes Binary konnte nicht als Bitmap decodiert werden.")
                    }
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

            val now = System.currentTimeMillis()
            if (now - lastFrameSentAt < FRAME_INTERVAL_MS) {
                return
            }

            lastFrameSentAt = now

            if (!isWebSocketConnectingOrConnected) {
                Timber.i("Erster Kamera-Frame empfangen! Starte WebSocket-Verbindung...")
                connect()
            }

            val currentWebSocket = webSocket
            if (currentWebSocket != null) {
                val jpegBytes = imageProxy.toJpegBytes(JPEG_QUALITY)
                currentWebSocket.send(jpegBytes.toByteString())
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "Fehler beim Verarbeiten des Frames: ${e.message}")
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
        _processedBitmap.value = null
        isWebSocketConnectingOrConnected = false

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProviderFuture.get().unbindAll()
            imageAnalysis = null

            webSocket?.close(WEBSOCKET_CODE, "Stream stopped")
            webSocket = null
        }, ContextCompat.getMainExecutor(context))
    }

    fun disconnect() {
        stopStream()
        cameraExecutor.shutdown()
    }

    private companion object {
        private const val JPEG_QUALITY = 60
        private const val FRAME_INTERVAL_MS = 200L

        private const val WEBSOCKET_CODE = 1000
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
