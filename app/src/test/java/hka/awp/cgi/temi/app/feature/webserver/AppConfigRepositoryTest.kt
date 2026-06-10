package hka.awp.cgi.temi.app.feature.webserver

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

class AppConfigRepositoryTest {

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `when datastore is empty, admin password falls back to default hash`() = runTest {
        val tmpDir = createTempDirectory(prefix = "app-config-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = AppConfigRepository(dataStore)

        val expectedHash = repository.hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
        assertEquals(expectedHash, repository.adminPasswordHash.first())

        tmpDir.deleteRecursively()
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `when password is updated, repository stores and returns hashed value`() = runTest {
        val tmpDir = createTempDirectory(prefix = "app-config-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = AppConfigRepository(dataStore)

        val newPassword = "newPassword42"
        repository.updateAdminPassword(newPassword)

        val expectedHash = repository.hashPassword(newPassword)
        assertEquals(expectedHash, repository.adminPasswordHash.first())
        assertTrue(repository.isValidAdminPassword(newPassword, expectedHash))

        tmpDir.deleteRecursively()
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `when legacy plain password exists, repository hashes it as fallback`() = runTest {
        val tmpDir = createTempDirectory(prefix = "app-config-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = AppConfigRepository(dataStore)

        val legacyPassword = "legacy-admin"
        val legacyKey = stringPreferencesKey("admin_password")
        dataStore.edit { prefs ->
            prefs[legacyKey] = legacyPassword
        }

        val expectedHash = repository.hashPassword(legacyPassword)
        assertEquals(expectedHash, repository.adminPasswordHash.first())

        tmpDir.deleteRecursively()
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `reset webserver defaults restores default url and default admin password hash`() = runTest {
        val tmpDir = createTempDirectory(prefix = "app-config-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = AppConfigRepository(dataStore)

        repository.updateUrl("https://example.com/custom")
        repository.updateAdminPassword("super-secret")

        repository.resetWebserverDefaults()

        assertEquals(BuildConfig.WEBVIEW_URL, repository.currentUrl.first())
        val expectedDefaultHash = repository.hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
        assertEquals(expectedDefaultHash, repository.adminPasswordHash.first())

        tmpDir.deleteRecursively()
    }
}
