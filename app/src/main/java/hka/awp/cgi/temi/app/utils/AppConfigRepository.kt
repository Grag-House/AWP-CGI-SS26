package hka.awp.cgi.temi.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.DialogPatrolMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest

class AppConfigRepository(private val dataStore: DataStore<Preferences>) {
    private val webviewUrlKey = stringPreferencesKey("webview_url")
    private val latitudeKey = doublePreferencesKey("latitude")
    private val longitudeKey = doublePreferencesKey("longitude")

    // Neue Keys
    private val adminPanelPasswordHashKey = stringPreferencesKey("admin_panel_password_hash")
    private val webserverPasswordHashKey = stringPreferencesKey("webserver_password_hash")
    private val adminPasswordLegacyKey = stringPreferencesKey("admin_password")

    private val KEY_IS_PATROL_ENABLED = booleanPreferencesKey("is_patrol_enabled")
    private val KEY_PATROL_MODE = stringPreferencesKey("patrol_mode")
    private val KEY_MIN_MINUTES = intPreferencesKey("min_minutes")
    private val KEY_MAX_MINUTES = intPreferencesKey("max_minutes")
    private val KEY_SELECTED_HOURS = stringPreferencesKey("selected_hours")
    private val PATROL_ROUTE = stringPreferencesKey("patrol_route")

    private companion object {
        const val ROUTE_SEPARATOR = "|"
    }

    // --- Webview URL ---
    val currentUrl: Flow<String> = dataStore.data.map { it[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL }

    suspend fun updateUrl(newUrl: String) {
        dataStore.edit { it[webviewUrlKey] = newUrl }
    }

    // --- Migration & Passwort Management ---

    suspend fun performMigrationIfNeeded() {
        dataStore.edit { prefs ->
            val legacy = prefs[adminPasswordLegacyKey]
            if (legacy != null) {
                val hash = hashPassword(legacy)
                // Migriere Legacy zu BEIDEN neuen Keys
                prefs[adminPanelPasswordHashKey] = hash
                prefs[webserverPasswordHashKey] = hash
                prefs.remove(adminPasswordLegacyKey)
                Timber.d("Migration auf neue Passwort-Struktur abgeschlossen.")
            }
        }
    }

    val adminPanelPasswordHash: Flow<String> = dataStore.data.map {
        it[adminPanelPasswordHashKey] ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    val webserverPasswordHash: Flow<String> = dataStore.data.map {
        it[webserverPasswordHashKey] ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    suspend fun updateAdminPanelPassword(password: String) {
        dataStore.edit { it[adminPanelPasswordHashKey] = hashPassword(password) }
    }

    suspend fun updateWebserverPassword(password: String) {
        dataStore.edit { it[webserverPasswordHashKey] = hashPassword(password) }
    }

    fun isValidPassword(plainPassword: String, currentHash: String): Boolean {
        return hashPassword(plainPassword) == currentHash
    }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    val latitude: Flow<Double> = dataStore.data.map { it[latitudeKey] ?: 49.0138 }
    val longitude: Flow<Double> = dataStore.data.map { it[longitudeKey] ?: 8.3573 }

    suspend fun updateCoordinates(latitude: Double, longitude: Double) {
        dataStore.edit {
            it[latitudeKey] = latitude
            it[longitudeKey] = longitude
        }
    }

    suspend fun updatePatrolSettings(isEnabled: Boolean, mode: DialogPatrolMode, minMin: Int, maxMin: Int, hours: Set<Int>) {
        dataStore.edit {
            it[KEY_IS_PATROL_ENABLED] = isEnabled
            it[KEY_PATROL_MODE] = mode.name
            it[KEY_MIN_MINUTES] = minMin
            it[KEY_MAX_MINUTES] = maxMin
            it[KEY_SELECTED_HOURS] = hours.joinToString(",")
        }
    }

    val isPatrolEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_IS_PATROL_ENABLED] ?: false }
    val patrolMode: Flow<DialogPatrolMode> = dataStore.data.map {
        try { DialogPatrolMode.valueOf(it[KEY_PATROL_MODE] ?: DialogPatrolMode.RANDOM.name) }
        catch (e: Exception) { DialogPatrolMode.RANDOM }
    }
    val minPatrolMinutes: Flow<Int> = dataStore.data.map { it[KEY_MIN_MINUTES] ?: 40 }
    val maxPatrolMinutes: Flow<Int> = dataStore.data.map { it[KEY_MAX_MINUTES] ?: 60 }
    val selectedPatrolHours: Flow<Set<Int>> = dataStore.data.map {
        it[KEY_SELECTED_HOURS]?.split(",")?.filter { s -> s.isNotEmpty() }?.map { s -> s.toInt() }?.toSet() ?: emptySet()
    }
    val patrolRoute: Flow<List<String>> = dataStore.data.map { it[PATROL_ROUTE]?.split(ROUTE_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList() }

    suspend fun updatePatrolRoute(route: List<String>) {
        dataStore.edit { it[PATROL_ROUTE] = route.joinToString(ROUTE_SEPARATOR) }
    }
}
