package hka.awp.cgi.temi.app.feature.webserver

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val speakerVerificationEnabledKey = booleanPreferencesKey("speaker_verification_enabled")

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

    // --- Speaker Verification ---

    val isSpeakerVerificationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[speakerVerificationEnabledKey] ?: false // Default off
    }

    suspend fun updateSpeakerVerificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[speakerVerificationEnabledKey] = enabled
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

    // --- General ---

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
