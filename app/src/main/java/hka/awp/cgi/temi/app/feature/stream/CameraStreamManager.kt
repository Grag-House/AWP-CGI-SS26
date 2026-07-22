package hka.awp.cgi.temi.app.feature.stream

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
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
import okio.ByteString
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraStreamManager(
    private val context: Context,
    private val serverUrl: String,
) {
    private val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()

    // This executor remains alive across normal start/stop cycles.
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var cameraProvider: ProcessCameraProvider? = null

    @Volatile
    private var imageAnalysis: ImageAnalysis? = null

    @Volatile
    private var isStreaming = false

    @Volatile
    private var isSocketOpen = false

    private var lastFrameSentAt = 0L

    // Prevents an unlimited backlog: only one frame is waiting for a server response.
    private val frameInFlight = AtomicBoolean(false)

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()

    private val _textMessages = MutableSharedFlow<String>(extraBufferCapacity = 20)
    val textMessages: SharedFlow<String> = _textMessages.asSharedFlow()

    @Synchronized
    private fun connect() {
        if (!isStreaming || webSocket != null) return

        val request = Request.Builder().url(serverUrl).build()

        val newSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (this@CameraStreamManager.webSocket !== webSocket || !isStreaming) {
                        webSocket.close(WEBSOCKET_CODE, "Obsolete stream session")
                        return
                    }

                    isSocketOpen = true
                    frameInFlight.set(false)
                    Timber.d("WebSocket connected: %s", serverUrl)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (this@CameraStreamManager.webSocket !== webSocket) return

                    // A response means the server has handled the previous frame.
                    frameInFlight.set(false)
                    Timber.d("Analysis: %s", text)
                    _textMessages.tryEmit(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    if (this@CameraStreamManager.webSocket !== webSocket) return

                    frameInFlight.set(false)

                    val data = bytes.toByteArray()
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    if (bitmap != null) {
                        _processedBitmap.value = bitmap
                    } else {
                        Timber.w("Received binary message could not be decoded as bitmap.")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Timber.e(t, "WebSocket error")
                    clearSocketIfCurrent(webSocket)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.d("WebSocket closed: %d %s", code, reason)
                    clearSocketIfCurrent(webSocket)
                }
            }
        )

        webSocket = newSocket
    }

    @Synchronized
    private fun clearSocketIfCurrent(socket: WebSocket) {
        // An old socket callback must never clear a newly opened socket.
        if (webSocket === socket) {
            webSocket = null
            isSocketOpen = false
            frameInFlight.set(false)

            if (isStreaming) {
                connect()
            }
        }
    }

    @Synchronized
    fun startStream() {
        if (isStreaming) return

        isStreaming = true
        isSocketOpen = false
        frameInFlight.set(false)
        lastFrameSentAt = 0L
        connect()

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (!isStreaming) return@addListener

            val provider = providerFuture.get()
            cameraProvider = provider

            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(STREAM_WIDTH, STREAM_HEIGHT))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

            analysis.setAnalyzer(cameraExecutor, ::handleFrame)
            imageAnalysis = analysis

            // Only replace this manager's analysis use case, not every camera use case in the app.
            provider.unbind(analysis)
            provider.bindToLifecycle(
                ProcessLifecycleOwner.get(),
                CameraSelector.DEFAULT_FRONT_CAMERA,
                analysis
            )
        }, mainExecutor)
    }

    @Suppress("ReturnCount")
    private fun handleFrame(imageProxy: ImageProxy) {
        try {
            if (!isStreaming || !isSocketOpen) return

            val now = System.currentTimeMillis()
            if (now - lastFrameSentAt < FRAME_INTERVAL_MS) return

            val socket = webSocket ?: return

            // Local network queue protection in addition to one-frame-in-flight control.
            if (socket.queueSize() > MAX_SOCKET_QUEUE_BYTES) {
                Timber.w("Dropping frame because WebSocket queue is too large: %d bytes", socket.queueSize())
                return
            }

            if (!frameInFlight.compareAndSet(false, true)) return

            lastFrameSentAt = now
            val jpegBytes = imageProxy.toJpegBytes(JPEG_QUALITY)

            if (!socket.send(jpegBytes.toByteString())) {
                frameInFlight.set(false)
                Timber.w("WebSocket rejected camera frame.")
            }
        } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
            frameInFlight.set(false)
            Timber.e(exception, "Error processing camera frame")
        } finally {
            imageProxy.close()
        }
    }

    fun sendText(text: String): Boolean = webSocket?.send(text) == true

    @Synchronized
    fun stopStream() {
        if (!isStreaming && webSocket == null && imageAnalysis == null) return

        isStreaming = false
        isSocketOpen = false
        frameInFlight.set(false)
        lastFrameSentAt = 0L
        _processedBitmap.value = null

        imageAnalysis?.clearAnalyzer()
        imageAnalysis?.let { analysis -> cameraProvider?.unbind(analysis) }
        imageAnalysis = null

        val socketToClose = webSocket
        webSocket = null
        socketToClose?.close(WEBSOCKET_CODE, "Stream stopped")
    }

    /** Call only when the manager itself is permanently destroyed. */
    fun release() {
        stopStream()
        cameraExecutor.shutdown()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private companion object {
        private const val STREAM_WIDTH = 640
        private const val STREAM_HEIGHT = 480
        private const val JPEG_QUALITY = 45
        private const val FRAME_INTERVAL_MS = 250L
        private const val MAX_SOCKET_QUEUE_BYTES = 1_000_000L
        private const val WEBSOCKET_CODE = 1000
    }
}

private fun ImageProxy.toJpegBytes(quality: Int): ByteArray {
    val nv21 = yuv420ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    return ByteArrayOutputStream().use { outputStream ->
        yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, outputStream)
        outputStream.toByteArray()
    }
}

private fun yuv420ToNv21(image: ImageProxy): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    return ByteArray(ySize + uSize + vSize).also { nv21 ->
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
    }
}
