package hka.awp.cgi.temi.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.DialogPatrolMode
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
    private val KEY_IS_PATROL_ENABLED = booleanPreferencesKey("is_patrol_enabled")
    private val KEY_PATROL_MODE = stringPreferencesKey("patrol_mode") // Enums speichert man am besten als String
    private val KEY_MIN_MINUTES = intPreferencesKey("min_minutes")
    private val KEY_MAX_MINUTES = intPreferencesKey("max_minutes")
    private val KEY_SELECTED_HOURS = stringPreferencesKey("selected_hours")

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

    @Suppress("MagicNumber")
    val latitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[latitudeKey] ?: 49.0138 // Default Karlsruhe
    }

    @Suppress("MagicNumber")
    val longitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[longitudeKey] ?: 8.3573 // Default Karlsruhe
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
        mode: DialogPatrolMode,
        minMin: Int,
        maxMin: Int,
        hours: Set<Int>
    ) {
        Timber.d("Speichere Patrol Settings...")
        dataStore.edit { preferences ->
            preferences[KEY_IS_PATROL_ENABLED] = isEnabled
            preferences[KEY_PATROL_MODE] = mode.name
            preferences[KEY_MIN_MINUTES] = minMin
            preferences[KEY_MAX_MINUTES] = maxMin
            preferences[KEY_SELECTED_HOURS] = hours.joinToString(",")
        }
    }

    val isPatrolEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_IS_PATROL_ENABLED] ?: false }
    val patrolMode: Flow<DialogPatrolMode> = dataStore.data.map {
        val name = it[KEY_PATROL_MODE] ?: DialogPatrolMode.RANDOM.name
        try { DialogPatrolMode.valueOf(name) } catch (e: Exception) { DialogPatrolMode.RANDOM }
    }
    val minPatrolMinutes: Flow<Int> = dataStore.data.map { it[KEY_MIN_MINUTES] ?: 40 }
    val maxPatrolMinutes: Flow<Int> = dataStore.data.map { it[KEY_MAX_MINUTES] ?: 60 }
    val selectedPatrolHours: Flow<Set<Int>> = dataStore.data.map {
        it[KEY_SELECTED_HOURS]?.split(",")?.filter { s -> s.isNotEmpty() }?.map { s -> s.toInt() }?.toSet() ?: emptySet()
    }
}
