package hka.awp.cgi.temi.app.feature.photobox

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TICK_MS = 1000L

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
                    delay(TICK_MS)
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

    private suspend fun captureOnce(): Bitmap? = suspendCancellableCoroutine { continuation ->
        cameraManager.capturePhoto { result -> continuation.resumeWith(Result.success(result.getOrNull())) }
    }
}
