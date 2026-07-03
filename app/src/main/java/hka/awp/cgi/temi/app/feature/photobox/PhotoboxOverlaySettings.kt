package hka.awp.cgi.temi.app.feature.photobox

import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Position of the Temi cutout on Photobox photos: left, center, or right of the frame. */
enum class TemiOverlayPosition { LEFT, CENTER, RIGHT }

private val DEFAULT_OVERLAY_POSITION = TemiOverlayPosition.RIGHT

/** Whether/where the Temi cutout is shown on Photobox photos, backed by [AppConfigRepository]. */
internal class PhotoboxOverlaySettings(
    private val appConfigRepository: AppConfigRepository,
    private val scope: CoroutineScope
) {
    val enabled: StateFlow<Boolean> = appConfigRepository.photobox.photoboxOverlayEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val position: StateFlow<TemiOverlayPosition> = appConfigRepository.photobox.photoboxOverlayPosition
        .map { raw -> runCatching { TemiOverlayPosition.valueOf(raw) }.getOrDefault(DEFAULT_OVERLAY_POSITION) }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_OVERLAY_POSITION)

    fun setPosition(position: TemiOverlayPosition) {
        scope.launch {
            appConfigRepository.photobox.setPhotoboxOverlay(enabled.value, position.name)
        }
    }
}
