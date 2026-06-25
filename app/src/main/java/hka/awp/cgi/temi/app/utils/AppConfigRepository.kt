package hka.awp.cgi.temi.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolSettingsDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.security.MessageDigest

/**
 * Central repository for application configurations stored in Jetpack DataStore.
 * Handles URL settings, coordinates for weather, and admin credentials.
 */
class AppConfigRepository(private val dataStore: DataStore<Preferences>) {
    private val webviewUrlKey = stringPreferencesKey("webview_url")
    private val latitudeKey = doublePreferencesKey("latitude")
    private val longitudeKey = doublePreferencesKey("longitude")
    private val adminPasswordHashKey = stringPreferencesKey("admin_password_hash")
    private val adminPasswordLegacyKey = stringPreferencesKey("admin_password")
    private val keyIsPatrolEnabled = booleanPreferencesKey("is_patrol_enabled")
    private val keyPatrolMode = stringPreferencesKey("patrol_mode") // Enums speichert man am besten als String
    private val keyMinMinutes = intPreferencesKey("min_minutes")
    private val keyMaxMinutes = intPreferencesKey("max_minutes")
    private val keySelectedHours = stringPreferencesKey("selected_hours")
    private val keyPatrolRoute = stringPreferencesKey("patrol_route")
    private companion object {
        const val ROUTE_SEPARATOR = "|"
        const val COMMA_SEPARATOR = ","

        // Standardwerte
        const val DEFAULT_LATITUDE = 49.0138
        const val DEFAULT_LONGITUDE = 8.3573
        const val DEFAULT_PATROL_ENABLED = false
        const val DEFAULT_MIN_MINUTES = 40
        const val DEFAULT_MAX_MINUTES = 60
    }
    // --- Webview URL ---

    val currentUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL
    }

    suspend fun updateUrl(newUrl: String) {
        dataStore.edit { preferences ->
            preferences[webviewUrlKey] = newUrl
        }
    }

    // --- Coordinates ---

    val latitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[latitudeKey] ?: DEFAULT_LATITUDE // Default Karlsruhe
    }

    val longitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[longitudeKey] ?: DEFAULT_LONGITUDE // Default Karlsruhe
    }

    suspend fun updateCoordinates(latitude: Double, longitude: Double) {
        dataStore.edit { preferences ->
            preferences[latitudeKey] = latitude
            preferences[longitudeKey] = longitude
        }
    }

    // --- Admin Password ---

    val adminPasswordHash: Flow<String> = dataStore.data.map { preferences ->
        preferences[adminPasswordHashKey]
            ?: preferences[adminPasswordLegacyKey]?.let(::hashPassword)
            ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    suspend fun updateAdminPassword(password: String) {
        val hash = hashPassword(password)
        dataStore.edit { preferences ->
            preferences[adminPasswordHashKey] = hash
            preferences.remove(adminPasswordLegacyKey)
        }
    }

    suspend fun resetAdminPassword(password: String) {
        val hash = hashPassword(password)
        dataStore.edit { preferences ->
            preferences[adminPasswordHashKey] = hash
            preferences.remove(adminPasswordLegacyKey)
        }
    }

    suspend fun resetWebserverDefaults() {
        dataStore.edit { preferences ->
            preferences[webviewUrlKey] = BuildConfig.WEBVIEW_URL
            preferences[adminPasswordHashKey] = hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
            preferences.remove(adminPasswordLegacyKey)
        }
    }

    fun isValidAdminPassword(plainPassword: String, currentHash: String): Boolean {
        return hashPassword(plainPassword) == currentHash
    }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    suspend fun updatePatrolSettings(
        isEnabled: Boolean,
        mode: PatrolSettingsDialog,
        minMin: Int,
        maxMin: Int,
        hours: Set<Int>
    ) {
        Timber.d("Speichere Patrol Settings...")
        dataStore.edit { preferences ->
            preferences[keyIsPatrolEnabled] = isEnabled
            preferences[keyPatrolMode] = mode.name
            preferences[keyMinMinutes] = minMin
            preferences[keyMaxMinutes] = maxMin
            preferences[keySelectedHours] = hours.joinToString(COMMA_SEPARATOR)
        }
    }

    val isPatrolEnabled: Flow<Boolean> = dataStore.data.map { it[keyIsPatrolEnabled] ?: DEFAULT_PATROL_ENABLED }
    val patrolMode: Flow<PatrolSettingsDialog> = dataStore.data.map {
        val name = it[keyPatrolMode] ?: PatrolSettingsDialog.RANDOM.name
        try {
            PatrolSettingsDialog.valueOf(name)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Ungültiger Patrol-Modus im DataStore gefunden: $name. Setze auf RANDOM.")
            PatrolSettingsDialog.RANDOM
        }
    }
    val minPatrolMinutes: Flow<Int> = dataStore.data.map { it[keyMinMinutes] ?: DEFAULT_MIN_MINUTES }
    val maxPatrolMinutes: Flow<Int> = dataStore.data.map { it[keyMaxMinutes] ?: DEFAULT_MAX_MINUTES }
    val selectedPatrolHours: Flow<Set<Int>> = dataStore.data.map {
        it[keySelectedHours]?.split(COMMA_SEPARATOR)?.filter { s -> s.isNotEmpty() }?.map {
                s ->
            s.toInt()
        }?.toSet() ?: emptySet()
    }
    val patrolRoute: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[keyPatrolRoute]
            ?.split(ROUTE_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun updatePatrolRoute(route: List<String>) {
        dataStore.edit { preferences ->
            preferences[keyPatrolRoute] = route.joinToString(ROUTE_SEPARATOR)
        }
    }
}
