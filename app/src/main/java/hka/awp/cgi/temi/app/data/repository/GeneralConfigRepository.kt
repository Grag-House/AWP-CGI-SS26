package hka.awp.cgi.temi.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository responsible for general application configurations.
 * Handles WebView URLs, location coordinates, and application language.
 *
 * @property dataStore The [DataStore] instance used for persisting general settings.
 */
class GeneralConfigRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val webviewUrlKey = stringPreferencesKey("webview_url")
    private val latitudeKey = doublePreferencesKey("latitude")
    private val longitudeKey = doublePreferencesKey("longitude")
    private val languageKey = stringPreferencesKey("app_language")
    private val speakerVerificationEnabledKey = booleanPreferencesKey("speaker_verification_enabled")
    private val speakerVerificationThresholdKey = doublePreferencesKey("speaker_verification_threshold")

    /**
     * Flow indicating whether speaker verification (voice AI) is enabled.
     */
    val isSpeakerVerificationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[speakerVerificationEnabledKey] ?: false
    }

    /**
     * Flow of the threshold value for speaker verification (0.0 to 1.0).
     */
    val speakerVerificationThreshold: Flow<Double> = dataStore.data.map { preferences ->
        preferences[speakerVerificationThresholdKey] ?: DEFAULT_SPEAKER_VERIFICATION_THRESHOLD
    }

    /**
     * Updates speaker verification settings.
     *
     * @param enabled Whether to enable speaker verification.
     * @param threshold Optional new threshold value.
     */
    suspend fun updateSpeakerVerification(
        enabled: Boolean? = null,
        threshold: Double? = null
    ) {
        dataStore.edit { preferences ->
            enabled?.let { preferences[speakerVerificationEnabledKey] = it }
            threshold?.let { preferences[speakerVerificationThresholdKey] = it.coerceIn(0.0, 1.0) }
        }
    }

    /**
     * Flow of the current URL for the main WebView.
     */
    val currentUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL
    }

    /**
     * Updates the WebView URL.
     *
     * @param newUrl The new URL to display.
     */
    suspend fun updateUrl(newUrl: String) {
        dataStore.edit { it[webviewUrlKey] = newUrl }
    }

    /**
     * Flow of the stored latitude coordinate.
     */
    val latitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[latitudeKey] ?: DEFAULT_LATITUDE
    }

    /**
     * Flow of the stored longitude coordinate.
     */
    val longitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[longitudeKey] ?: DEFAULT_LONGITUDE
    }

    /**
     * Updates the stored geographic coordinates.
     *
     * @param latitude The new latitude.
     * @param longitude The new longitude.
     */
    suspend fun updateCoordinates(latitude: Double, longitude: Double) {
        dataStore.edit { preferences ->
            preferences[latitudeKey] = latitude
            preferences[longitudeKey] = longitude
        }
    }

    /**
     * Flow of the current application language code (e.g., "de", "en").
     */
    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[languageKey] ?: DEFAULT_LANGUAGE
    }

    /**
     * Updates the application language.
     *
     * @param languageCode ISO 639-1 language code.
     */
    suspend fun updateLanguage(languageCode: String) {
        dataStore.edit { it[languageKey] = languageCode }
    }

    companion object {
        const val DEFAULT_LATITUDE = 49.0138
        const val DEFAULT_LONGITUDE = 8.3573
        const val DEFAULT_LANGUAGE = "de"
        const val DEFAULT_SPEAKER_VERIFICATION_THRESHOLD = 0.82
    }
}
