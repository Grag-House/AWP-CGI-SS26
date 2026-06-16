package hka.awp.cgi.temi.app.feature.voiceRecognition

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Stores multiple user voice profiles (speaker vectors) in DataStore.
 */
class VoiceProfileRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val VOICE_PROFILES_KEY = stringPreferencesKey("voice_profiles")
    }

    /**
     * Map of profile name to speaker vector.
     */
    val voiceProfiles: Flow<Map<String, List<Float>>> = dataStore.data.map { preferences ->
        preferences[VOICE_PROFILES_KEY]?.let { json ->
            try {
                Json.decodeFromString<Map<String, List<Float>>>(json)
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception
            ) {
                Timber.e(e, "Failed to decode voice profiles from DataStore")
                emptyMap()
            }
        } ?: emptyMap()
    }

    suspend fun saveVoiceProfile(name: String, vector: List<Float>) {
        dataStore.edit { preferences ->
            val currentProfiles = preferences[VOICE_PROFILES_KEY]?.let { json ->
                runCatching { Json.decodeFromString<Map<String, List<Float>>>(json) }.getOrDefault(emptyMap())
            } ?: emptyMap()

            val updatedProfiles = currentProfiles + (name to vector)
            preferences[VOICE_PROFILES_KEY] = Json.encodeToString(updatedProfiles)
        }
    }

    suspend fun deleteVoiceProfile(name: String) {
        dataStore.edit { preferences ->
            val currentProfiles = preferences[VOICE_PROFILES_KEY]?.let { json ->
                runCatching { Json.decodeFromString<Map<String, List<Float>>>(json) }.getOrDefault(emptyMap())
            } ?: emptyMap()

            val updatedProfiles = currentProfiles - name
            preferences[VOICE_PROFILES_KEY] = Json.encodeToString(updatedProfiles)
        }
    }

    suspend fun clearAllProfiles() {
        dataStore.edit { preferences ->
            preferences.remove(VOICE_PROFILES_KEY)
        }
    }
}
