package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.data.repository.PhotoboxConfigRepository
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxBannerSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing configuration parameters for the Photobox feature.
 *
 * It bridges the UI to the [PhotoboxConfigRepository], exposing settings as lifecycle-aware [StateFlow]s.
 *
 * @property photoboxConfigRepository Repository for persisting photobox configuration.
 */
class PhotoboxSettingsViewModel(
    private val photoboxConfigRepository: PhotoboxConfigRepository
) : ViewModel() {

    /** Indicates whether photobox overlays are enabled. */
    val overlayEnabled: StateFlow<Boolean> = photoboxConfigRepository.photoboxOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Helper for managing branding banner settings. */
    internal val bannerSettings = PhotoboxBannerSettings(photoboxConfigRepository, viewModelScope)

    /** The current Google Drive folder link for photo access. */
    val driveFolderLink: StateFlow<String> = photoboxConfigRepository.driveFolderLink
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /** The endpoint URL for cloud photo uploads. */
    val driveUploadUrl: StateFlow<String> = photoboxConfigRepository.driveUploadUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /**
     * Toggles the photobox overlay.
     *
     * @param enabled Set to `true` to enable, or `false` to disable.
     */
    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val position = photoboxConfigRepository.photoboxOverlayPosition.first()
            photoboxConfigRepository.setPhotoboxOverlay(enabled, position)
        }
    }

    /**
     * Updates the Google Drive folder link.
     *
     * @param link The new folder URL.
     */
    fun setDriveFolderLink(link: String) {
        viewModelScope.launch {
            photoboxConfigRepository.setDriveSettings(folderLink = link)
        }
    }

    /**
     * Updates the Google Drive upload endpoint.
     *
     * @param url The new upload URL.
     */
    fun setDriveUploadUrl(url: String) {
        viewModelScope.launch {
            photoboxConfigRepository.setDriveSettings(uploadUrl = url)
        }
    }
}
