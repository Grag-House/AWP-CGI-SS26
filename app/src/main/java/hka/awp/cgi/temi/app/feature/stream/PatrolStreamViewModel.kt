package hka.awp.cgi.temi.app.feature.stream

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class PatrolStreamViewModel(
    cameraStreamManager: CameraStreamManager,
) : ViewModel() {

    val processedBitmap: StateFlow<Bitmap?> = cameraStreamManager.processedBitmap
}
