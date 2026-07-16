package hka.awp.cgi.temi.app.feature.settings.language

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.data.repository.GeneralConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel for managing the application's language settings.
 *
 * This ViewModel handles retrieving the current language and updating it across app restarts.
 *
 * @property generalConfigRepository Repository for persisting general application settings, including language.
 */
class LanguageViewModel(
    private val generalConfigRepository: GeneralConfigRepository
) : ViewModel() {

    private val _selectedLocale = MutableStateFlow(Locale.getDefault())

    /** Current active [Locale] for the application. */
    val selectedLocale = _selectedLocale.asStateFlow()

    /** Supported locales for the application. */
    val supportedLocales = listOf(
        Locale.ENGLISH,
        Locale.FRENCH,
        Locale.GERMAN,
        Locale.ITALIAN
    )

    init {
        viewModelScope.launch {
            val langCode = generalConfigRepository.language.first()
            _selectedLocale.value = Locale.Builder().setLanguage(langCode).build()
        }
    }

    /**
     * Updates the application language and refreshes the current activity.
     *
     * @param languageCode ISO 639-1 language identifier.
     * @param context The current context, expected to be an [Activity].
     */
    fun updateLocale(languageCode: String, context: Context) {
        val newLocale = Locale.Builder().setLanguage(languageCode).build()
        _selectedLocale.value = newLocale
        viewModelScope.launch {
            generalConfigRepository.updateLanguage(languageCode)
            (context as? Activity)?.recreate()
        }
    }
}
