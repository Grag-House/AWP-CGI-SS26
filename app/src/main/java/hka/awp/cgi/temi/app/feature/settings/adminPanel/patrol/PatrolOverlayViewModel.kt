package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PatrolOverlayViewModel(
    private val patrolManager: PatrolManager,
    private val patrolCameraStreamManager: PatrolCameraStreamManager
                            ) : ViewModel() {

    val isRunning: StateFlow<Boolean> = patrolManager.isRunning
    val videoFrame: StateFlow<Bitmap?> = patrolCameraStreamManager.videoFrame

    private val _isOverlayVisible = MutableStateFlow(true)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible

    fun stopPatrol() {
        patrolManager.stopPatrol()
        _isOverlayVisible.value = true
    }

    fun hideOverlay() {
        _isOverlayVisible.value = false
    }

    fun showOverlay() {
        _isOverlayVisible.value = true
    }
}
