package hka.awp.cgi.temi.app.feature.settings.notifications

import android.app.Application
import androidx.lifecycle.ViewModel
import com.robotemi.sdk.Robot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

@Suppress("MagicNumber")
class NotificationViewModel(
    private val robot: Robot?
                           ) : ViewModel() {

    private val _volume = MutableStateFlow(0)
    val volume = _volume.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _availableLocales = MutableStateFlow<List<Locale>>(emptyList())
    val availableLocales = _availableLocales.asStateFlow()

    private val _selectedLocale = MutableStateFlow(Locale.getDefault())
    val selectedLocale = _selectedLocale.asStateFlow()

    private var previousVolume = 5

    init {
        loadCurrentVolume()
    }

    private fun loadCurrentVolume() {
        val current = robot?.volume ?: return
        _volume.value = current.coerceIn(0, 10)
    }

    fun updateVolume(newVolume: Int) {
        val clamped = newVolume.coerceIn(0, 10)

        _volume.value = clamped
        robot?.setVolume(clamped)
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled

        if (!enabled) {
            previousVolume = _volume.value.coerceIn(0, 10)

            robot?.setVolume(0)
            _volume.value = 0
        } else {
            val restore = previousVolume.coerceIn(0, 10).takeIf { it > 0 } ?: 5

            robot?.setVolume(restore)
            _volume.value = restore
        }
    }

    fun setLocale(locale: Locale) {
        _selectedLocale.value = locale
    }
}
