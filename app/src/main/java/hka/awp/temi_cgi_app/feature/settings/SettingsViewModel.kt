package hka.awp.temi_cgi_app.feature.settings

import androidx.lifecycle.ViewModel
import hka.awp.temi_cgi_app.data.repository.RobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing settings state and logic.
 */
class SettingsViewModel(private val repository: RobotRepository) : ViewModel() {

    private val _aboutInfo = MutableStateFlow<Map<String, String>?>(null)
    val aboutInfo: StateFlow<Map<String, String>?> = _aboutInfo.asStateFlow()

    /**
     * Handles clicks on setting items.
     *
     * @param item The selected setting.
     */
    fun onSettingsItemClick(item: SettingsItem) {
        when (item) {
            SettingsItem.Notifications -> { /* TODO */ }
            SettingsItem.Display -> { /* TODO */ }
            SettingsItem.Battery -> { /* TODO */ }
            SettingsItem.Location -> { /* TODO */ }
            SettingsItem.About -> {
                _aboutInfo.value = mapOf(
                    "Roboter Name" to "Temi-CGI-App-Dev",
                    "Seriennummer" to "1234-5678-90AB",
                    "Version" to "1.0.0",
                    "IP-Adresse" to repository.getIpAddress(),
                    "Modell" to "Temi V1"
                )
            }
        }
    }

    fun dismissAbout() {
        _aboutInfo.value = null
    }
}