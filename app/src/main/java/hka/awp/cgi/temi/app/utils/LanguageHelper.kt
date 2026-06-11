package hka.awp.cgi.temi.app.utils

import android.content.Context

object LanguageHelper {

    private const val PREFS_NAME = "Settings"
    private const val KEY_LANGUAGE = "lang"
    private const val DEFAULT_LANGUAGE = "de"

    fun setLocale(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getLocale(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
}
