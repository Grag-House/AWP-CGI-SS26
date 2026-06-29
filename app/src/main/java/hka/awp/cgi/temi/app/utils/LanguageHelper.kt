package hka.awp.cgi.temi.app.utils

import android.content.Context
import androidx.core.content.edit

/**
 * A utility object responsible for persisting and retrieving the application's language configuration.
 *
 * It acts as a lightweight wrapper around Android's [android.content.SharedPreferences] to ensure
 * that the user's localized language selection is saved across application restarts.
 */
object LanguageHelper {

    private const val PREFS_NAME = "Settings"
    private const val KEY_LANGUAGE = "lang"
    private const val DEFAULT_LANGUAGE = "de"

    /**
     * Persists the selected language code asynchronously into the private preferences storage.
     *
     * @param context The context required to access the shared preferences system.
     * @param languageCode The ISO 639-1 alpha-2 language identifier code (e.g., "en", "de").
     */
    fun setLocale(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LANGUAGE, languageCode) }
    }

    /**
     * Retrieves the currently persisted language code from storage.
     * Returns "de" as a fallback default if no preference has been configured yet.
     *
     * @param context The context required to access the shared preferences system.
     * @return The saved language identifier string.
     */
    fun getLocale(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
}
