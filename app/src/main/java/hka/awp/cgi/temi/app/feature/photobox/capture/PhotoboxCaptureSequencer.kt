package hka.awp.cgi.temi.app.feature.photobox.capture

import android.graphics.Bitmap
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxMode
import hka.awp.cgi.temi.app.feature.photobox.STRIP_SHOT_COUNT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private val TICK = 1.seconds

// Some camera HALs (notably legacy/compatibility ones) can occasionally fail to deliver a
// takePicture() result at all — neither success nor error. Without a timeout on our side, that
// leaves the session stuck forever waiting for a callback that never comes.
private val CAPTURE_TIMEOUT = 8.seconds

/** The callbacks a [PhotoboxCaptureSequencer] reports session progress through. */
internal data class PhotoboxCaptureCallbacks(
    val onTick: (countdownRemaining: Int, isBetweenShots: Boolean, shotsTaken: Int) -> Unit,
    val onCapturing: () -> Unit,
    val onShotsReady: (List<Bitmap>) -> Unit,
    val onFailed: () -> Unit
)

/**
 * Runs the countdown(s) and camera capture(s) for one Photobox session: a single shot for
 * [PhotoboxMode.STANDARD], or [STRIP_SHOT_COUNT] shots with a delay between each for
 * [PhotoboxMode.STRIP]. Combining/uploading the result is the caller's responsibility.
 */
internal class PhotoboxCaptureSequencer(
    private val cameraManager: PhotoboxCameraManager,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun start(
        mode: PhotoboxMode,
        firstShotDelaySeconds: Int,
        betweenShotsDelaySeconds: Int,
        callbacks: PhotoboxCaptureCallbacks
    ) {
        job?.cancel()
        val totalShots = if (mode == PhotoboxMode.STRIP) STRIP_SHOT_COUNT else 1
        val shots = mutableListOf<Bitmap>()

        job = scope.launch {
            var delaySeconds = firstShotDelaySeconds
            while (shots.size < totalShots && isActive) {
                var remaining = delaySeconds
                while (remaining > 0 && isActive) {
                    callbacks.onTick(remaining, shots.isNotEmpty(), shots.size)
                    delay(TICK)
                    remaining--
                }
                if (!isActive) return@launch

                callbacks.onCapturing()
                val bitmap = captureOnce()
                if (bitmap == null) {
                    callbacks.onFailed()
                    return@launch
                }
                shots.add(bitmap)
                delaySeconds = betweenShotsDelaySeconds
            }
            if (isActive) callbacks.onShotsReady(shots)
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private suspend fun captureOnce(): Bitmap? = withTimeoutOrNull(CAPTURE_TIMEOUT) {
        suspendCancellableCoroutine { continuation ->
            cameraManager.capturePhoto { result -> continuation.resumeWith(Result.success(result.getOrNull())) }
        }
    }
}
