package hka.awp.cgi.temi.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolSettingsDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.security.MessageDigest

class AppConfigRepository(private val dataStore: DataStore<Preferences>) {
    
    // Keys
    private val webviewUrlKey = stringPreferencesKey("webview_url")
    private val latitudeKey = doublePreferencesKey("latitude")
    private val longitudeKey = doublePreferencesKey("longitude")
    private val adminPanelPasswordHashKey = stringPreferencesKey("admin_panel_password_hash")
    private val webserverPasswordHashKey = stringPreferencesKey("webserver_password_hash")
    private val adminPasswordLegacyKey = stringPreferencesKey("admin_password")
    private val keyIsPatrolEnabled = booleanPreferencesKey("is_patrol_enabled")
    private val keyPatrolMode = stringPreferencesKey("patrol_mode")
    private val keyMinMinutes = intPreferencesKey("min_minutes")
    private val keyMaxMinutes = intPreferencesKey("max_minutes")
    private val keySelectedHours = stringPreferencesKey("selected_hours")
    private val keyPatrolRoute = stringPreferencesKey("patrol_route")

    private companion object {
        const val ROUTE_SEPARATOR = "|"
        const val COMMA_SEPARATOR = ","
        const val DEFAULT_LATITUDE = 49.0138
        const val DEFAULT_LONGITUDE = 8.3573
        const val DEFAULT_PATROL_ENABLED = false
        const val DEFAULT_MIN_MINUTES = 40
        const val DEFAULT_MAX_MINUTES = 60
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

    // --- Webview & Koordinaten ---

    val currentUrl: Flow<String> = dataStore.data.map { it[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL }
    val latitude: Flow<Double> = dataStore.data.map { it[latitudeKey] ?: DEFAULT_LATITUDE }
    val longitude: Flow<Double> = dataStore.data.map { it[longitudeKey] ?: DEFAULT_LONGITUDE }

    suspend fun updateUrl(newUrl: String) {
        dataStore.edit { it[webviewUrlKey] = newUrl }
    }

    suspend fun updateCoordinates(lat: Double, lon: Double) {
        dataStore.edit {
            it[latitudeKey] = lat
            it[longitudeKey] = lon
        }
    }

    // --- Patrol Settings ---

    suspend fun updatePatrolSettings(isEnabled: Boolean, mode: PatrolSettingsDialog, minMin: Int, maxMin: Int, hours: Set<Int>) {
        dataStore.edit {
            it[keyIsPatrolEnabled] = isEnabled
            it[keyPatrolMode] = mode.name
            it[keyMinMinutes] = minMin
            it[keyMaxMinutes] = maxMin
            it[keySelectedHours] = hours.joinToString(COMMA_SEPARATOR)
        }
    }

    val isPatrolEnabled: Flow<Boolean> = dataStore.data.map { it[keyIsPatrolEnabled] ?: DEFAULT_PATROL_ENABLED }
    val patrolMode: Flow<PatrolSettingsDialog> = dataStore.data.map {
        try { PatrolSettingsDialog.valueOf(it[keyPatrolMode] ?: PatrolSettingsDialog.RANDOM.name) }
        catch (e: IllegalArgumentException) { PatrolSettingsDialog.RANDOM }
    }
    val minPatrolMinutes: Flow<Int> = dataStore.data.map { it[keyMinMinutes] ?: DEFAULT_MIN_MINUTES }
    val maxPatrolMinutes: Flow<Int> = dataStore.data.map { it[keyMaxMinutes] ?: DEFAULT_MAX_MINUTES }
    
    val selectedPatrolHours: Flow<Set<Int>> = dataStore.data.map {
        it[keySelectedHours]?.split(COMMA_SEPARATOR)?.mapNotNull { s -> s.toIntOrNull() }?.toSet() ?: emptySet()
    }
    
    val patrolRoute: Flow<List<String>> = dataStore.data.map { 
        it[keyPatrolRoute]?.split(ROUTE_SEPARATOR)?.filter { s -> s.isNotBlank() } ?: emptyList() 
    }

    suspend fun updatePatrolRoute(route: List<String>) {
        dataStore.edit { it[keyPatrolRoute] = route.joinToString(ROUTE_SEPARATOR) }
    }
}