package hka.awp.cgi.temi.app.feature.settings.language

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import hka.awp.cgi.temi.app.utils.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

class LanguageViewModel : ViewModel() {

    private val _selectedLocale = MutableStateFlow(Locale.getDefault())
    val selectedLocale = _selectedLocale.asStateFlow()

    val supportedLocales = listOf(
        Locale.GERMAN,
        Locale.ENGLISH
    )

    fun updateLocale(languageCode: String, context: Context) {
        LocaleHelper.setLocale(context, languageCode)

        (context as? Activity)?.recreate()
    }
}
