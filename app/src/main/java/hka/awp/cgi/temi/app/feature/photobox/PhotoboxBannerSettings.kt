package hka.awp.cgi.temi.app.feature.photobox

import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PhotoboxBanner(val drawableRes: Int) {
    FIFTY_YEARS_CGI(R.drawable.banner_50_jahre_cgi),
    CGI_LAB(R.drawable.banner_cgi_lab)
}

private val DEFAULT_BANNER = PhotoboxBanner.CGI_LAB

// Both banner PNGs share this width:height ratio (4400x327) — used to approximate, in the
// preview screen's live Compose layer, how tall the already-baked banner renders so Temi's live
// overlay can be shifted to sit above it (see PhotoboxPreviewOverlay). The actual bake
// (PhotoboxUploadRepository) reads the real decoded bitmap instead, so this is preview-only.
const val PHOTOBOX_BANNER_ASPECT_RATIO = 4400f / 327f

/** Whether/which branding banner is burned into Photobox photos, backed by [AppConfigRepository]. */
internal class PhotoboxBannerSettings(
    private val appConfigRepository: AppConfigRepository,
    private val scope: CoroutineScope
) {
    val enabled: StateFlow<Boolean> = appConfigRepository.photoboxBannerEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val banner: StateFlow<PhotoboxBanner> = appConfigRepository.photoboxBanner
        .map { raw -> runCatching { PhotoboxBanner.valueOf(raw) }.getOrDefault(DEFAULT_BANNER) }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_BANNER)

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            appConfigRepository.setPhotoboxBanner(enabled, banner.value.name)
        }
    }

    fun setBanner(banner: PhotoboxBanner) {
        scope.launch {
            appConfigRepository.setPhotoboxBanner(enabled.value, banner.name)
        }
    }
}
