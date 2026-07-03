package hka.awp.cgi.temi.app.feature.photobox

import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Branding banner variant burned into the bottom of Photobox photos. Each variant carries
 * separate assets for standalone, strip, and grid layouts because the branding area's aspect
 * ratio differs across modes.
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

    /** The standalone-photo banner is wide and thin; strip/grid composites get a banner cropped
     * for their narrower branding area instead, since the wide one looked out of place there. The
     * 2x2 grid uses its own asset rather than the strip one since its branding area has a
     * different aspect ratio. */
    fun drawableRes(mode: PhotoboxMode): Int = when (mode) {
        PhotoboxMode.STANDARD -> standardDrawableRes
        PhotoboxMode.GRID_2X2 -> grid2x2DrawableRes
        PhotoboxMode.STRIP, PhotoboxMode.STRIP_1X4 -> stripDrawableRes
    }
}

private val DEFAULT_BANNER = PhotoboxBanner.CGI_LAB

internal const val PHOTOBOX_GRID_BANNER_WIDTH_FRACTION = 1.0f

const val PHOTOBOX_BANNER_ASPECT_RATIO = 4400f / 327f

/** Whether/which branding banner is burned into Photobox photos, backed by [AppConfigRepository]. */
internal class PhotoboxBannerSettings(
    private val appConfigRepository: AppConfigRepository,
    private val scope: CoroutineScope
) {
    val enabled: StateFlow<Boolean> = appConfigRepository.photobox.photoboxBannerEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val banner: StateFlow<PhotoboxBanner> = appConfigRepository.photobox.photoboxBanner
        .map { raw -> runCatching { PhotoboxBanner.valueOf(raw) }.getOrDefault(DEFAULT_BANNER) }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_BANNER)

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            appConfigRepository.photobox.setPhotoboxBanner(enabled, banner.value.name)
        }
    }

    fun setBanner(banner: PhotoboxBanner) {
        scope.launch {
            appConfigRepository.photobox.setPhotoboxBanner(enabled.value, banner.name)
        }
    }
}
