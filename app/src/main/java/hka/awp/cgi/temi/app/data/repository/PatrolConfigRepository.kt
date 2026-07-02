package hka.awp.cgi.temi.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.data.model.PatrolMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Repository responsible for managing patrol-related configurations.
 * Handles patrol state, timing, schedule, and route information.
 *
 * @property dataStore The [DataStore] instance used for persisting patrol settings.
 */
class PatrolConfigRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val keyIsPatrolEnabled = booleanPreferencesKey("is_patrol_enabled")
    private val keyPatrolMode = stringPreferencesKey("patrol_mode")
    private val keyMinMinutes = intPreferencesKey("min_minutes")
    private val keyMaxMinutes = intPreferencesKey("max_minutes")
    private val keySelectedHours = stringPreferencesKey("selected_hours")
    private val keyPatrolRoute = stringPreferencesKey("patrol_route")

    /**
     * Flow of the patrol enabled status.
     */
    val isPatrolEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[keyIsPatrolEnabled] ?: DEFAULT_PATROL_ENABLED
    }

    /**
     * Flow of the current patrol mode (e.g., RANDOM or ROUTE).
     */
    val patrolMode: Flow<PatrolMode> = dataStore.data.map { preferences ->
        val name = preferences[keyPatrolMode] ?: PatrolMode.RANDOM.name
        try {
            PatrolMode.valueOf(name)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid Patrol Mode: $name. Defaulting to RANDOM.")
            PatrolMode.RANDOM
        }
    }

    /**
     * Flow of the minimum interval between patrols in minutes.
     */
    val minPatrolMinutes: Flow<Int> = dataStore.data.map { preferences ->
        preferences[keyMinMinutes] ?: DEFAULT_MIN_MINUTES
    }

    /**
     * Flow of the maximum interval between patrols in minutes.
     */
    val maxPatrolMinutes: Flow<Int> = dataStore.data.map { preferences ->
        preferences[keyMaxMinutes] ?: DEFAULT_MAX_MINUTES
    }

    /**
     * Flow of the set of hours (0-23) when patrol is active.
     */
    val selectedPatrolHours: Flow<Set<Int>> = dataStore.data.map { preferences ->
        preferences[keySelectedHours]
            ?.split(COMMA_SEPARATOR)
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    /**
     * Flow of the list of location names defining the patrol route.
     */
    val patrolRoute: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[keyPatrolRoute]
            ?.split(ROUTE_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    /**
     * Updates common patrol settings in a single transaction.
     *
     * @param isEnabled Whether patrol should be enabled.
     * @param mode The patrol mode.
     * @param minMin Minimum interval in minutes.
     * @param maxMin Maximum interval in minutes.
     * @param hours Set of active patrol hours.
     */
    suspend fun updatePatrolSettings(
        isEnabled: Boolean,
        mode: PatrolMode,
        minMin: Int,
        maxMin: Int,
        hours: Set<Int>
    ) {
        dataStore.edit { preferences ->
            preferences[keyIsPatrolEnabled] = isEnabled
            preferences[keyPatrolMode] = mode.name
            preferences[keyMinMinutes] = minMin
            preferences[keyMaxMinutes] = maxMin
            preferences[keySelectedHours] = hours.joinToString(COMMA_SEPARATOR)
        }
    }

    /**
     * Updates the patrol route.
     *
     * @param route List of location names for the route.
     */
    suspend fun updatePatrolRoute(route: List<String>) {
        dataStore.edit { preferences ->
            preferences[keyPatrolRoute] = route.joinToString(ROUTE_SEPARATOR)
        }
    }

    companion object {
        private const val ROUTE_SEPARATOR = "|"
        private const val COMMA_SEPARATOR = ","
        private const val DEFAULT_PATROL_ENABLED = false
        private const val DEFAULT_MIN_MINUTES = 40
        private const val DEFAULT_MAX_MINUTES = 60
    }
}
