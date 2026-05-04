package hka.awp.temi_cgi_app.feature.settings.notifications

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationViewModel : ViewModel() {
    private val _volume = MutableStateFlow(0.5f) // 0.0 bis 1.0
    val volume = _volume.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume
        // TODO Audio Manager
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }
}