package hka.awp.cgi.temi.app.feature.voiceRecognition

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class VoiceProfileRepositoryTest {

    private val voiceProfilesKey = stringPreferencesKey("voice_profiles")

    @Test
    fun `save and load voice profile`() = runTest {
        val tmpDir = Files.createTempDirectory("datastore-test").toFile()
        val file = File(tmpDir, "test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = VoiceProfileRepository(dataStore)

        val name = "TestUser"
        val vector = FloatArray(SpeakerVector.VECTOR_SIZE) { it.toFloat() }

        repository.saveVoiceProfile(name, vector)

        val profiles = repository.voiceProfiles.first()
        assertTrue(profiles.containsKey(name))
        assertEquals(SpeakerVector(vector), profiles[name])

        tmpDir.deleteRecursively()
    }

    @Test
    fun `delete voice profile`() = runTest {
        val tmpDir = Files.createTempDirectory("datastore-test").toFile()
        val file = File(tmpDir, "test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = VoiceProfileRepository(dataStore)

        val name = "TestUser"
        val vector = FloatArray(SpeakerVector.VECTOR_SIZE) { it.toFloat() }

        repository.saveVoiceProfile(name, vector)
        repository.deleteVoiceProfile(name)

        val profiles = repository.voiceProfiles.first()
        assertTrue(profiles.isEmpty())

        tmpDir.deleteRecursively()
    }

    @Test
    fun `clear all profiles`() = runTest {
        val tmpDir = Files.createTempDirectory("datastore-test").toFile()
        val file = File(tmpDir, "test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = VoiceProfileRepository(dataStore)

        repository.saveVoiceProfile("U1", FloatArray(SpeakerVector.VECTOR_SIZE))
        repository.saveVoiceProfile("U2", FloatArray(SpeakerVector.VECTOR_SIZE))
        repository.clearAllProfiles()

        val profiles = repository.voiceProfiles.first()
        assertTrue(profiles.isEmpty())

        tmpDir.deleteRecursively()
    }

    @Test
    fun `save profile aborts on corrupted data`() = runTest {
        val tmpDir = Files.createTempDirectory("datastore-test").toFile()
        val file = File(tmpDir, "test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = VoiceProfileRepository(dataStore)

        // Inject corrupted JSON
        dataStore.edit { it[voiceProfilesKey] = "{ invalid json" }

        repository.saveVoiceProfile("NewUser", FloatArray(SpeakerVector.VECTOR_SIZE))

        // DataStore should still contain the corrupted string (not emptyMap() or the new profile)
        dataStore.data.first().let { prefs ->
            assertEquals("{ invalid json", prefs[voiceProfilesKey])
        }

        tmpDir.deleteRecursively()
    }
}
