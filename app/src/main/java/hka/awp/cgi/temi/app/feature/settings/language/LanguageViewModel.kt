package hka.awp.cgi.temi.app.feature.settings.language

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LanguageViewModel : ViewModel() {

    private val _selectedLocale = MutableStateFlow(Locale.getDefault())
    val selectedLocale = _selectedLocale.asStateFlow()

    val supportedLocales = listOf(
        Locale.GERMAN,
        Locale.ENGLISH
    )

    fun updateLocale(newLocale: Locale) {
        if (_selectedLocale.value != newLocale) {
            _selectedLocale.value = newLocale
        }
    }
}
