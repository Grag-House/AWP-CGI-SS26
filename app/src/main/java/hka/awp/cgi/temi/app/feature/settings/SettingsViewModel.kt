package hka.awp.cgi.temi.app.feature.settings

import androidx.lifecycle.ViewModel

/**
 * ViewModel for managing settings state and logic.
 */
class SettingsViewModel : ViewModel() {
    /**
     * Handles clicks on setting items.
     *
     * @param item The selected setting.
     */
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
