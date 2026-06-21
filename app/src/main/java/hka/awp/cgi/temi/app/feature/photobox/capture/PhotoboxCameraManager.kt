package hka.awp.cgi.temi.app.feature.photobox.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.LifecycleOwner
import hka.awp.cgi.temi.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.math.roundToInt

enum class PhotoboxCameraState { UNINITIALIZED, BINDING, READY, UNAVAILABLE }

// Caps the captured bitmap's long edge for performance; does not affect field of view/aspect,
// unlike constraining ImageCapture's own resolution selector (which made it crop differently
// from the unconstrained live Preview).
private const val MAX_OUTPUT_LONG_EDGE = 1600

// The device's (legacy) camera HAL defaults to its maximum sensor resolution (12+ MP) for
// ImageCapture, which can take several seconds to encode to JPEG — long enough to blow past
// CameraX's internal "jpeg callback" timeout and leave the request hanging indefinitely. Capping
// the requested resolution keeps capture fast and reliable. Safe to combine with the shared
// ViewPort below: that already keeps Preview/ImageCapture cropped consistently regardless of
// whichever resolution gets picked here.
private const val CAPTURE_TARGET_WIDTH = 1920
private const val CAPTURE_TARGET_HEIGHT = 1080
private val CAPTURE_TARGET_SIZE = Size(CAPTURE_TARGET_WIDTH, CAPTURE_TARGET_HEIGHT)

/**
 * Owns the CameraX lifecycle for the Photobox feature: binds the device camera to a
 * preview surface, takes photos and reports state via [cameraState]. Kept independent of
 * Compose so the UI only has to forward a lifecycle and a surface provider.
 */
class PhotoboxCameraManager(private val context: Context) {

    private val _cameraState = MutableStateFlow(PhotoboxCameraState.UNINITIALIZED)
    val cameraState: StateFlow<PhotoboxCameraState> = _cameraState.asStateFlow()

    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val imageCapture = ImageCapture.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(CAPTURE_TARGET_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                )
                .build()
        )
        .build()
    private var cameraProvider: ProcessCameraProvider? = null

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val shutterSoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val shutterSoundId = shutterSoundPool.load(context, R.raw.camera_shutter, 1)

    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        viewPort: ViewPort?
    ) {
        _cameraState.value = PhotoboxCameraState.BINDING
        // Without this, ImageCapture derives rotation from the device's natural orientation,
        // which can disagree with the locked display rotation and yield a portrait photo.
        imageCapture.targetRotation = currentDisplayRotation()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
            val useFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            try {
                provider.unbindAll()
                // Sharing a ViewPort makes ImageCapture crop to exactly what Preview shows —
                // without it, the two use cases can independently pick different aspect ratios,
                // so the captured photo ends up cropped differently than what was previewed live.
                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
                    .apply { viewPort?.let(::setViewPort) }
                    .build()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
                cameraProvider = provider
                _isFrontCamera.value = useFrontCamera
                _cameraState.value = PhotoboxCameraState.READY
            } catch (e: IllegalStateException) {
                Timber.e(e, "Camera binding failed")
                _cameraState.value = PhotoboxCameraState.UNAVAILABLE
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Camera binding failed")
                _cameraState.value = PhotoboxCameraState.UNAVAILABLE
            }
        }, mainExecutor)
    }

    fun capturePhoto(onResult: (Result<Bitmap>) -> Unit) {
        if (_cameraState.value != PhotoboxCameraState.READY) {
            onResult(Result.failure(IllegalStateException("Camera is not ready")))
            return
        }
        shutterSoundPool.play(shutterSoundId, 1f, 1f, 1, 0, 1f)
        imageCapture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rawBitmap = image.toBitmap()
                    val cropRect = image.cropRect
                    // The ViewPort crop is metadata on the ImageProxy, not baked into the raw
                    // buffer — toBitmap() returns the full, uncropped sensor frame, so we have to
                    // crop to cropRect ourselves or the uncropped margin shows up as a black band.
                    val bitmap = if (cropRect.width() != rawBitmap.width || cropRect.height() != rawBitmap.height) {
                        Bitmap.createBitmap(rawBitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
                    } else {
                        rawBitmap
                    }
                    val rotation = image.imageInfo.rotationDegrees
                    image.close()
                    var result = rotatedBitmap(bitmap, rotation)
                    if (_isFrontCamera.value) result = mirroredBitmap(result)
                    result = downscaledIfNeeded(result)
                    onResult(Result.success(result))
                }

                override fun onError(exc: ImageCaptureException) {
                    Timber.e(exc, "Photo capture failed")
                    onResult(Result.failure(exc))
                }
            }
        )
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        _cameraState.value = PhotoboxCameraState.UNINITIALIZED
    }

    private fun rotatedBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun mirroredBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postScale(-1f, 1f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun downscaledIfNeeded(bitmap: Bitmap): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= MAX_OUTPUT_LONG_EDGE) return bitmap
        val scale = MAX_OUTPUT_LONG_EDGE.toFloat() / longEdge
        val newWidth = (bitmap.width * scale).roundToInt()
        val newHeight = (bitmap.height * scale).roundToInt()
        return bitmap.scale(newWidth, newHeight)
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        return windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
    }
}
