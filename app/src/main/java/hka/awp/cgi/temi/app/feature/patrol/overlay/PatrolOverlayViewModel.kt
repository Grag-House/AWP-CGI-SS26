package hka.awp.cgi.temi.app.feature.patrol.overlay

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import hka.awp.cgi.temi.app.feature.patrol.PatrolCameraStreamManager
import hka.awp.cgi.temi.app.feature.patrol.PatrolManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the patrol overlay UI, providing reactive state for the robot's status,
 * camera stream, and countdown timers.
 *
 * @param patrolManager The central manager handling patrol states and logic.
 * @param patrolCameraStreamManager Manager providing the live [Bitmap] video frame.
 */
class PatrolOverlayViewModel(
    private val patrolManager: PatrolManager,
    private val patrolCameraStreamManager: PatrolCameraStreamManager
) : ViewModel() {

    /** Current running state of the patrol. */
    val isRunning: StateFlow<Boolean> = patrolManager.isRunning

    /** Live camera feed frame from the robot. */
    val videoFrame: StateFlow<Bitmap?> = patrolCameraStreamManager.videoFrame

    private val _isOverlayVisible = MutableStateFlow(true)

    /** Indicates if the patrol control overlay should be rendered. */
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    /** The current countdown value (if a patrol is scheduled to start). */
    val countdownSeconds: StateFlow<Int?> = patrolManager.countdownSeconds

    /** Terminates the active patrol and resets the overlay visibility. */
    fun stopPatrol() {
        patrolManager.stopPatrol()
        _isOverlayVisible.value = true
    }

    /** Hides the overlay UI. */
    fun hideOverlay() {
        _isOverlayVisible.value = false
    }

    /** Makes the overlay UI visible. */
    fun showOverlay() {
        _isOverlayVisible.value = true
    }
}
