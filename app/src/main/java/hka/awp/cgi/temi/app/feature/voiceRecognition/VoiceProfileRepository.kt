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
 * Stores multiple user voice profiles (speaker i-Vectors) in DataStore.
 * Each profile is a 128-dimensional speaker embedding vector from Vosk.
 */
class VoiceProfileRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val VOICE_PROFILES_KEY = stringPreferencesKey("voice_profiles")
        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * Map of profile name to speaker vector.
     * Flows updated whenever profiles change.
     */
    val voiceProfiles: Flow<Map<String, SpeakerVector>> = dataStore.data.map { preferences ->
        loadProfiles(preferences)
    }

    private fun loadProfiles(preferences: Preferences): Map<String, SpeakerVector> {
        val jsonString = preferences[VOICE_PROFILES_KEY] ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, SpeakerVector>>(jsonString)
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception
        ) {
            Timber.e(e, "Failed to decode voice profiles from DataStore")
            emptyMap()
        }
    }

    /**
     * Saves a new voice profile.
     * Aborts if current DataStore content is corrupted to prevent accidental data loss.
     */
    suspend fun saveVoiceProfile(name: String, vector: FloatArray) {
        dataStore.edit { preferences ->
            val speakerVector = SpeakerVector(vector)
            val jsonString = preferences[VOICE_PROFILES_KEY]

            val currentProfiles = if (jsonString != null) {
                try {
                    json.decodeFromString<Map<String, SpeakerVector>>(jsonString)
                } catch (
                    @Suppress("TooGenericExceptionCaught")
                    e: Exception
                ) {
                    Timber.e(e, "Corrupted DataStore detected during save. Aborting to prevent data loss.")
                    return@edit
                }
            } else {
                emptyMap()
            }

            val updatedProfiles = currentProfiles + (name to speakerVector)
            preferences[VOICE_PROFILES_KEY] = json.encodeToString(updatedProfiles)
            Timber.i("Voice profile '%s' saved successfully", name)
        }
    }

    /**
     * Deletes a voice profile by name.
     * Aborts if current DataStore content is corrupted.
     */
    suspend fun deleteVoiceProfile(name: String) {
        dataStore.edit { preferences ->
            val jsonString = preferences[VOICE_PROFILES_KEY] ?: return@edit

            val currentProfiles = try {
                json.decodeFromString<Map<String, SpeakerVector>>(jsonString)
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception
            ) {
                Timber.e(e, "Corrupted DataStore detected during delete. Aborting.")
                return@edit
            }

            val updatedProfiles = currentProfiles - name
            preferences[VOICE_PROFILES_KEY] = json.encodeToString(updatedProfiles)
            Timber.i("Voice profile '%s' deleted", name)
        }
    }

    /**
     * Clears all stored voice profiles.
     */
    suspend fun clearAllProfiles() {
        dataStore.edit { preferences ->
            preferences.remove(VOICE_PROFILES_KEY)
            Timber.i("All voice profiles cleared")
        }
    }
}
