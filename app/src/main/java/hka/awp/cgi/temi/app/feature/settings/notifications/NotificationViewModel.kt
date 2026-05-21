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
            Timber.d("Current temi volume from SDK: $currentVolume")

            val safeVolume = currentVolume.coerceIn(1, 10)
            _volume.value = (safeVolume - 1) / 9f
        }
    }

    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume

        val temiVolume = (1 + (newVolume * 9)).toInt().coerceIn(1, 10)

        robot?.setVolume(temiVolume)
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled

        if (!enabled) {
            previousVolume = (1 + (_volume.value * 9)).toInt().coerceIn(1, 10)

            robot?.setVolume(1)
        } else {
            val restoreVolume = if (previousVolume > 0) previousVolume else 5
            robot?.setVolume(restoreVolume)
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
