package hka.awp.cgi.temi.app.feature.photobox

import hka.awp.cgi.temi.app.data.repository.PhotoboxConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Position of the Temi cutout on Photobox photos.
 */
enum class TemiOverlayPosition { LEFT, CENTER, RIGHT }

private val DEFAULT_OVERLAY_POSITION = TemiOverlayPosition.RIGHT

/**
 * Manages whether and where the Temi cutout is shown on Photobox photos.
 *
 * @property photoboxConfigRepository Repository for persisting overlay settings.
 * @property scope Coroutine scope for updates.
 */
internal class PhotoboxOverlaySettings(
    private val photoboxConfigRepository: PhotoboxConfigRepository,
    private val scope: CoroutineScope
) {
    /** Whether the overlay is enabled. */
    val enabled: StateFlow<Boolean> = photoboxConfigRepository.photoboxOverlayEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** The current horizontal position of the overlay. */
    val position: StateFlow<TemiOverlayPosition> = photoboxConfigRepository.photoboxOverlayPosition
        .map { raw -> runCatching { TemiOverlayPosition.valueOf(raw) }.getOrDefault(DEFAULT_OVERLAY_POSITION) }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_OVERLAY_POSITION)

    /**
     * Updates the horizontal position of the overlay.
     *
     * @param position The new position.
     */
    fun setPosition(position: TemiOverlayPosition) {
        scope.launch {
            photoboxConfigRepository.setPhotoboxOverlay(enabled.value, position.name)
        }
    }
}
