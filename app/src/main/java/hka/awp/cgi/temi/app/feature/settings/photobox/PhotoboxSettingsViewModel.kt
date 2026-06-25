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

class PhotoboxSettingsViewModel(
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    val overlayEnabled: StateFlow<Boolean> = appConfigRepository.photoboxOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    internal val bannerSettings = PhotoboxBannerSettings(appConfigRepository, viewModelScope)

    val driveFolderLink: StateFlow<String> = appConfigRepository.driveFolderLink
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val driveUploadUrl: StateFlow<String> = appConfigRepository.driveUploadUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val position = appConfigRepository.photoboxOverlayPosition.first()
            appConfigRepository.setPhotoboxOverlay(enabled, position)
        }
    }

    fun setDriveFolderLink(link: String) {
        viewModelScope.launch {
            appConfigRepository.setDriveFolderLink(link)
        }
    }

    fun setDriveUploadUrl(url: String) {
        viewModelScope.launch {
            appConfigRepository.setDriveUploadUrl(url)
        }
    }
}
