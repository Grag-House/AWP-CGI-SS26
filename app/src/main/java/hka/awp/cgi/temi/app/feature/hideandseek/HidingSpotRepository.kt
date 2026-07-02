package hka.awp.cgi.temi.app.feature.hideandseek

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "hiding_spot_filter"
private const val KEY_HAS_FILTER = "has_filter"
private const val KEY_ENABLED_SPOTS = "enabled_spots"

/**
 * Persists the user's hiding spot filter selection across app restarts via SharedPreferences.
 * A null result from [loadEnabledSpots] means no filter is active (all locations are allowed).
 */
class HidingSpotRepository(private val context: Context) {

    fun loadEnabledSpots(): Set<String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_FILTER, false)) return null
        return prefs.getStringSet(KEY_ENABLED_SPOTS, emptySet())?.toSet() ?: emptySet()
    }

    fun saveEnabledSpots(spots: Set<String>?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_HAS_FILTER, spots != null)
            putStringSet(KEY_ENABLED_SPOTS, spots ?: emptySet())
        }
    }
}
