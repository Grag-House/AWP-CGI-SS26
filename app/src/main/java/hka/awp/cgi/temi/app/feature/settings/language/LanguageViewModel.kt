package hka.awp.cgi.temi.app.feature.settings.language

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import hka.awp.cgi.temi.app.utils.LanguageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * ViewModel responsible for managing and updating the application's runtime language configuration.
 *
 * It orchestrates runtime localization updates by exposing the currently active [Locale] as an
 * observable data flow and listing the application's supported translation targets. When a language
 * change is requested, it ensures consistency across preference structures and triggers an activity
 * reconstruction to refresh the underlying framework resources instantly.
 */
class LanguageViewModel : ViewModel() {

    private val _selectedLocale = MutableStateFlow(Locale.getDefault())

    /**
     * An observable stream representing the application's currently configured user interface [Locale].
     */
    val selectedLocale = _selectedLocale.asStateFlow()

    /**
     * A hardcoded collection list representing all localized translation variations supported by the app framework.
     */
    val supportedLocales = listOf(
        Locale.ENGLISH,
        Locale.FRENCH,
        Locale.GERMAN,
        Locale.ITALIAN
    )

    /**
     * Updates the persistent localization configuration of the application and refreshes active layout resources.
     *
     * This method updates the reactive state flow, applies the modifications globally via the [LanguageHelper],
     * and forces an immediate reconstruction of the calling [Activity] layer so that string resource definitions
     * re-evaluate according to the new language target.
     *
     * @param languageCode The ISO 639-1 alpha-2 language identifier (e.g., "en", "de").
     * @param context The interface context from which the update call
     * originates, expected to evaluate to an [Activity].
     */
    fun updateLocale(languageCode: String, context: Context) {
        val newLocale = Locale(languageCode)
        _selectedLocale.value = newLocale
        LanguageHelper.setLocale(context, languageCode)

        (context as? Activity)?.recreate()
    }
}
