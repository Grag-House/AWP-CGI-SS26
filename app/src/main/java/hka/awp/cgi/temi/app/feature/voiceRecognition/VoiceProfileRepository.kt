package hka.awp.cgi.temi.app.feature.voiceRecognition

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.feature.settings.adminPanel.SpeakerVector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Stores multiple user voice profiles (speaker i-Vectors) in DataStore.
 * Each profile is a 128-dimensional speaker embedding vector from Vosk.
 */
class VoiceProfileRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val VOICE_PROFILES_KEY = stringPreferencesKey("voice_profiles")
    }

    /**
     * Map of profile name to speaker vector.
     * Flows updated whenever profiles change.
     */
    val voiceProfiles: Flow<Map<String, SpeakerVector>> = dataStore.data.map { preferences ->
        preferences[VOICE_PROFILES_KEY]?.let { json ->
            try {
                Json.decodeFromString<Map<String, SpeakerVector>>(json)
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception
            ) {
                Timber.e(e, "Failed to decode voice profiles from DataStore")
                emptyMap()
            }
        } ?: emptyMap()
    }

    suspend fun saveVoiceProfile(name: String, vector: FloatArray) {
        dataStore.edit { preferences ->
            // Validate vector format (will throw if size != 128)
            val speakerVector = SpeakerVector(vector)

            val currentProfiles = preferences[VOICE_PROFILES_KEY]?.let { json ->
                runCatching { Json.decodeFromString<Map<String, SpeakerVector>>(json) }
                    .getOrDefault(emptyMap())
            } ?: emptyMap()

            val updatedProfiles = currentProfiles + (name to speakerVector)
            preferences[VOICE_PROFILES_KEY] = Json.encodeToString(updatedProfiles)
            Timber.i("Voice profile '$name' saved successfully")
        }
    }

    suspend fun deleteVoiceProfile(name: String) {
        dataStore.edit { preferences ->
            val currentProfiles = preferences[VOICE_PROFILES_KEY]?.let { json ->
                runCatching { Json.decodeFromString<Map<String, SpeakerVector>>(json) }
                    .getOrDefault(emptyMap())
            } ?: emptyMap()

            val updatedProfiles = currentProfiles - name
            preferences[VOICE_PROFILES_KEY] = Json.encodeToString(updatedProfiles)
            Timber.i("Voice profile '$name' deleted")
        }
    }

    suspend fun clearAllProfiles() {
        dataStore.edit { preferences ->
            preferences.remove(VOICE_PROFILES_KEY)
            Timber.i("All voice profiles cleared")
        }
    }
}
