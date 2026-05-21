package hka.awp.cgi.temi.app.feature.settings.notifications

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import com.robotemi.sdk.Robot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

@Suppress("MagicNumber")
class NotificationViewModel(
    application: Application,
    private val robot: Robot?
) : AndroidViewModel(application) {

    private var tts: TextToSpeech? = null

    private val _volume = MutableStateFlow(0.5f)
    val volume = _volume.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _availableLocales = MutableStateFlow<List<Locale>>(emptyList())
    val availableLocales = _availableLocales.asStateFlow()

    private val _selectedLocale = MutableStateFlow(Locale.getDefault())
    val selectedLocale = _selectedLocale.asStateFlow()
    private var previousVolume = 5

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locales = Locale.getAvailableLocales().filter { locale ->
                    val availability = tts?.isLanguageAvailable(locale) ?: -1
                    availability >= TextToSpeech.LANG_AVAILABLE
                }.sortedBy { it.displayName }
                _availableLocales.value = locales
                _selectedLocale.value = tts?.voice?.locale ?: Locale.getDefault()
            }
        }
    }

    init {
        loadCurrentVolume()
    }

    private fun loadCurrentVolume() {
        robot?.let {
            val currentVolume = it.volume
            Timber.d("Current temi volume: $currentVolume")
            _volume.value = currentVolume / 10f
        }
    }

    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume

        val temiVolume = (newVolume * 10).toInt()

        robot?.volume = temiVolume
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled

        if (!enabled) {
            previousVolume = (_volume.value * 10).toInt()
            robot?.volume = 0
        } else {
            robot?.volume = previousVolume
        }
    }

    fun setLocale(locale: Locale) {
        _selectedLocale.value = locale
        tts?.language = locale
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
}
