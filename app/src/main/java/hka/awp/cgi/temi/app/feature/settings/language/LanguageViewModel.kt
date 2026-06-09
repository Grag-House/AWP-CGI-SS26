package hka.awp.cgi.temi.app.feature.settings.language

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import hka.awp.cgi.temi.app.utils.LanguageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LanguageViewModel : ViewModel() {

    private val _selectedLocale = MutableStateFlow(Locale.getDefault())
    val selectedLocale = _selectedLocale.asStateFlow()

    val supportedLocales = listOf(
        Locale.ENGLISH,
        Locale.FRENCH,
        Locale.GERMAN,
        Locale.ITALIAN
    )

    fun updateLocale(languageCode: String, context: Context) {
        LanguageHelper.setLocale(context, languageCode)

        (context as? Activity)?.recreate()
    }
}
