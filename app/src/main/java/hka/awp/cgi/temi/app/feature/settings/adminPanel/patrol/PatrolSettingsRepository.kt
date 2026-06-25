package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PatrolSettingsRepository(
    private val prefs: SharedPreferences
) {
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PatrolSettings> = _settings

    fun saveSettings(settings: PatrolSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.isEnabled)
            .putString(KEY_MODE, settings.mode.name)
            .putInt(KEY_MIN_MINUTES, settings.minMinutes)
            .putInt(KEY_MAX_MINUTES, settings.maxMinutes)
            .putStringSet(KEY_HOURS, settings.hours.map { it.toString() }.toSet())
            .putStringSet(KEY_ROUTE, settings.route.toSet())
            .apply()

        _settings.value = settings
    }

    private fun loadSettings(): PatrolSettings {
        val mode = prefs.getString(KEY_MODE, DEFAULT_MODE.name)
            ?.let { PatrolMode.valueOf(it) }
            ?: DEFAULT_MODE

        return PatrolSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED),
            mode = mode,
            minMinutes = prefs.getInt(KEY_MIN_MINUTES, DEFAULT_MIN_MINUTES),
            maxMinutes = prefs.getInt(KEY_MAX_MINUTES, DEFAULT_MAX_MINUTES),
            hours = prefs.getStringSet(KEY_HOURS, emptySet())
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet(),
            route = prefs.getStringSet(KEY_ROUTE, emptySet())
                ?.toList()
                ?: emptyList()
        )
    }

    private companion object {
        const val KEY_ENABLED = "patrol_enabled"
        const val KEY_MODE = "patrol_mode"
        const val KEY_MIN_MINUTES = "patrol_min_minutes"
        const val KEY_MAX_MINUTES = "patrol_max_minutes"
        const val KEY_HOURS = "patrol_hours"
        const val KEY_ROUTE = "patrol_route"
        private const val DEFAULT_ENABLED = false
        private const val DEFAULT_MIN_MINUTES = 40
        private const val DEFAULT_MAX_MINUTES = 60
        private val DEFAULT_MODE = PatrolMode.RANDOM
    }
}
