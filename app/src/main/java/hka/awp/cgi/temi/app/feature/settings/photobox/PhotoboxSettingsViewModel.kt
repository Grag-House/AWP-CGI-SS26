package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoboxSettingsViewModel(
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    val overlayEnabled: StateFlow<Boolean> = appConfigRepository.photoboxOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appConfigRepository.setPhotoboxOverlayEnabled(enabled)
        }
    }
}
