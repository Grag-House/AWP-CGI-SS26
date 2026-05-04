package hka.awp.temi_cgi_app.feature.settings.notifications

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private var tts: TextToSpeech? = null

    private val _volume = MutableStateFlow(0.5f)
    val volume = _volume.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    // Neue States für die Sprache
    private val _availableLocales = MutableStateFlow<List<Locale>>(emptyList())
    val availableLocales = _availableLocales.asStateFlow()

    private val _selectedLocale = MutableStateFlow(Locale.getDefault())
    val selectedLocale = _selectedLocale.asStateFlow()

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

    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun setLocale(locale: Locale) {
        _selectedLocale.value = locale
        tts?.language = locale
        if (_notificationsEnabled.value) {
            tts?.speak("Stimme geändert", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
}