package hka.awp.temi_cgi_app.feature.settings

import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {
    fun onSettingsItemClick(item: SettingsItem) {
        when (item) {
            SettingsItem.Notifications -> TODO()
            SettingsItem.Display -> TODO()
            SettingsItem.Battery -> TODO()
            SettingsItem.Location -> TODO()
            SettingsItem.About -> TODO()
        }
    }
}