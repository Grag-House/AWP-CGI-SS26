package hka.awp.cgi.temi.app.feature.photobox

import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.data.repository.PhotoboxConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Branding banner variant burned into the bottom of Photobox photos.
 */
enum class PhotoboxBanner(
    private val standardDrawableRes: Int,
    private val stripDrawableRes: Int,
    private val grid2x2DrawableRes: Int
) {
    FIFTY_YEARS_CGI(
        R.drawable.banner_50_jahre_cgi,
        R.drawable.banner_50_jahre_grid,
        R.drawable.banner_50_jahre_2x2grid
    ),
    CGI_LAB(
        R.drawable.banner_cgi_lab,
        R.drawable.banner_cgi_lab_grid,
        R.drawable.banner_cgi_lab_2x2grid
    );

    /**
     * Returns the appropriate drawable resource for the given mode.
     */
    fun drawableRes(mode: PhotoboxMode): Int = when (mode) {
        PhotoboxMode.STANDARD -> standardDrawableRes
        PhotoboxMode.GRID_2X2 -> grid2x2DrawableRes
        PhotoboxMode.STRIP, PhotoboxMode.STRIP_1X4 -> stripDrawableRes
    }
}

private val DEFAULT_BANNER = PhotoboxBanner.CGI_LAB

internal const val PHOTOBOX_GRID_BANNER_WIDTH_FRACTION = 1.0f

const val PHOTOBOX_BANNER_ASPECT_RATIO = 4400f / 327f

/**
 * Manages photobox banner settings, backed by [PhotoboxConfigRepository].
 *
 * @property photoboxConfigRepository The repository for storing banner settings.
 * @property scope Coroutine scope for launching updates.
 */
internal class PhotoboxBannerSettings(
    private val photoboxConfigRepository: PhotoboxConfigRepository,
    private val scope: CoroutineScope
) {
    /** Whether the banner is currently enabled. */
    val enabled: StateFlow<Boolean> = photoboxConfigRepository.photoboxBannerEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** The currently selected banner variant. */
    val banner: StateFlow<PhotoboxBanner> = photoboxConfigRepository.photoboxBanner
        .map { raw -> runCatching { PhotoboxBanner.valueOf(raw) }.getOrDefault(DEFAULT_BANNER) }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_BANNER)

    /**
     * Updates the enabled state of the banner.
     *
     * @param enabled True to enable, false to disable.
     */
    fun setEnabled(enabled: Boolean) {
        scope.launch {
            photoboxConfigRepository.setPhotoboxBanner(enabled, banner.value.name)
        }
    }

    /**
     * Updates the selected banner variant.
     *
     * @param banner The new banner variant.
     */
    fun setBanner(banner: PhotoboxBanner) {
        scope.launch {
            photoboxConfigRepository.setPhotoboxBanner(enabled.value, banner.name)
        }
    }
}
