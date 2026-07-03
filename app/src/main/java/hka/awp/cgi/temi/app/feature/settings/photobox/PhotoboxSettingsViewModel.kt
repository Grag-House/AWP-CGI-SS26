package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxBannerSettings
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for preparing and modifying application configuration parameters for the photobox subsystem.
 *
 * It bridges the interactive UI layers to the [AppConfigRepository], rendering persistent preference metrics
 * active using lifecycle-aware [StateFlow] streams. Any mutations requested by settings items are processed
 * asynchronously within the view model's coroutine scope.
 *
 * @property appConfigRepository The infrastructure repository handling permanent configuration storage routines.
 */
class PhotoboxSettingsViewModel(
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    /**
     * An observable stream indicating whether picture frame image overlays should be displayed during captures.
     */
    val overlayEnabled: StateFlow<Boolean> = appConfigRepository.photobox.photoboxOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    internal val bannerSettings = PhotoboxBannerSettings(appConfigRepository, viewModelScope)

    /**
     * An observable stream representing the configured Google Drive target folder link.
     */
    val driveFolderLink: StateFlow<String> = appConfigRepository.photobox.driveFolderLink
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /**
     * An observable stream representing the remote script or endpoint URL handling background media uploads.
     */
    val driveUploadUrl: StateFlow<String> = appConfigRepository.photobox.driveUploadUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /**
     * Toggles the photobox camera overlay rendering layer
     * state while preserving the previously configured layout positions.
     *
     * @param enabled Set to `true` to display the overlay layer, or `false` to hide it.
     */
    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val position = appConfigRepository.photobox.photoboxOverlayPosition.first()
            appConfigRepository.photobox.setPhotoboxOverlay(enabled, position)
        }
    }

    /**
     * Updates the persistent storage configuration with a new destination target folder link.
     *
     * @param link The full path or URL identifier of the remote folder storage directory.
     */
    fun setDriveFolderLink(link: String) {
        viewModelScope.launch {
            appConfigRepository.photobox.setDriveSettings(folderLink = link)
        }
    }

    /**
     * Updates the persistent storage configuration with a new endpoint ingestion URL.
     *
     * @param url The server hook or target API route managing automated cloud uploads.
     */
    fun setDriveUploadUrl(url: String) {
        viewModelScope.launch {
            appConfigRepository.photobox.setDriveSettings(uploadUrl = url)
        }
    }
}
