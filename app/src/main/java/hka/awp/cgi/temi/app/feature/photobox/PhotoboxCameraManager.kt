package hka.awp.cgi.temi.app.feature.photobox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import hka.awp.cgi.temi.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executors

enum class PhotoboxCameraState { UNINITIALIZED, BINDING, READY, UNAVAILABLE }

private const val CAPTURE_TARGET_WIDTH = 1280
private const val CAPTURE_TARGET_HEIGHT = 720
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
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        )
        .build()
    private var cameraProvider: ProcessCameraProvider? = null
    private var isFrontCamera = true

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

    fun bindToLifecycle(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        _cameraState.value = PhotoboxCameraState.BINDING
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
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                cameraProvider = provider
                isFrontCamera = useFrontCamera
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
                    val bitmap = image.toBitmap()
                    val rotation = image.imageInfo.rotationDegrees
                    image.close()
                    var result = rotatedBitmap(bitmap, rotation)
                    if (isFrontCamera) result = mirroredBitmap(result)
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
}
