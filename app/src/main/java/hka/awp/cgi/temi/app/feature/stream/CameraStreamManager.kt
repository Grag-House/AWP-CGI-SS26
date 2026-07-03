package hka.awp.cgi.temi.app.feature.stream

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

/**
 * A core manager responsible for capturing real-time camera frames and streaming them via WebSockets.
 *
 * This class binds into the Jetpack CameraX lifecycle to capture frames from the default front camera,
 * processes them into optimized JPEG payloads, and sends them to a targeted WebSocket server.
 * It also handles bi-directional messaging, exposing incoming processed video frames as [Bitmap]s
 * and incoming server string data as reactive [SharedFlow] messages.
 *
 * All operations regarding connection throttling, backpressure handling, and hardware unbinding
 * are managed internally.
 */
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

    /**
     * Emits the latest processed frame returned by the server as a display-ready [Bitmap].
     * Emits `null` if the stream is stopped or uninitialized.
     */
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()

    private val _textMessages = MutableSharedFlow<String>(
        extraBufferCapacity = 20
    )

    /**
     * Hot stream emitting textual messages or event responses received from the server.
     */
    val textMessages: SharedFlow<String> = _textMessages.asSharedFlow()

    @Volatile
    private var isWebSocketConnectingOrConnected = false

    private fun connect() {
        if (webSocket != null || isWebSocketConnectingOrConnected) return

        isWebSocketConnectingOrConnected = true

        val request = Request.Builder().url(serverUrl).build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Timber.d("WebSocket connected: $serverUrl")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Timber.d("Analysis: $text")
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
                        Timber.w("Failed to decode received binary data into a Bitmap.")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Timber.e(t, "WebSocket error - resetting connection status")

                    isWebSocketConnectingOrConnected = false
                    this@CameraStreamManager.webSocket = null
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.d("WebSocket closed: $code $reason")
                    isWebSocketConnectingOrConnected = false
                    this@CameraStreamManager.webSocket = null
                }
            }
        )
    }

    /**
     * Initializes and binds CameraX to the application's process lifecycle and begins capturing frames.
     * The underlying WebSocket connection will automatically trigger upon receiving the initial camera frames.
     * If streaming is already active, this call is safely ignored.
     */
    fun startStream() {
        if (isStreaming) return
        isStreaming = true
        isWebSocketConnectingOrConnected = false

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
                Timber.i("First camera frame received! Initiating WebSocket connection...")
                connect()
            }

            val currentWebSocket = webSocket
            if (currentWebSocket != null) {
                val jpegBytes = imageProxy.toJpegBytes(JPEG_QUALITY)
                currentWebSocket.send(jpegBytes.toByteString())
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "Error processing frame: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Sends an arbitrary string message or JSON payload over the active WebSocket channel.
     *
     * @param text The data payload to transmit.
     */
    fun sendText(text: String) {
        webSocket?.send(text)
    }

    /**
     * Stops the active stream, unbinds all CameraX pipeline components, and gracefully closes
     * the current WebSocket session.
     */
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

    /**
     * Completely shuts down the streaming manager infrastructure, terminates background execution threads,
     * and releases camera and socket holds.
     */
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
