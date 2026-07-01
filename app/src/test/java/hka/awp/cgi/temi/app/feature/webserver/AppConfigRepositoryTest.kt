package hka.awp.cgi.temi.app.utils

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

class AppConfigRepositoryTest {

    @OptIn(ExperimentalPathApi::class)
    private fun createTestRepository(
        scope: kotlinx.coroutines.CoroutineScope
                                    ): Pair<AppConfigRepository, java.nio.file.Path> {
        val tmpDir = createTempDirectory(prefix = "app-config-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        // FakeWebserverCredentialStore avoids EncryptedSharedPreferences — no Context needed
        return AppConfigRepository(dataStore = dataStore, credentialStore = FakeWebserverCredentialStore()) to tmpDir
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `admin panel and webserver passwords are independent`() = runTest {
        val (repository, tmpDir) = createTestRepository(this)

        repository.updateAdminPanelPassword("admin123")
        repository.updateWebserverPassword("web456")

        val adminHash = repository.adminPanelPasswordHash.first()
        val webHash = repository.webserverPasswordHash.first()

        assertEquals(repository.hashPassword("admin123"), adminHash)
        assertEquals(repository.hashPassword("web456"), webHash)
        assertNotEquals(adminHash, webHash)

        tmpDir.deleteRecursively()
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `updateWebserverUser and updateWebserverPassword persist to credential store and flow`() = runTest {
        val fake = FakeWebserverCredentialStore()
        val tmpDir = createTempDirectory(prefix = "app-config-creds-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = this, produceFile = { file })
        val repository = AppConfigRepository(dataStore = dataStore, credentialStore = fake)

        repository.updateWebserverUser("alice")
        repository.updateWebserverPassword("s3cr3t")

        // Credential store holds plaintext for Basic Auth
        assertEquals("alice", fake.getUser())
        assertEquals("s3cr3t", fake.getPassword())

        // Flows emit the new values
        assertEquals("alice", repository.webserverUser.first())
        assertEquals("s3cr3t", repository.webserverPassword.first())

        // DataStore holds only hashes — never plaintext
        val userHash = repository.webserverUserHash.first()
        val passwordHash = repository.webserverPasswordHash.first()
        assertEquals(repository.hashPassword("alice"), userHash)
        assertEquals(repository.hashPassword("s3cr3t"), passwordHash)
        assertNotEquals("alice", userHash)
        assertNotEquals("s3cr3t", passwordHash)

        tmpDir.deleteRecursively()
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `webserverUser and webserverPassword flows are initialised from the credential store on construction`() =
        runTest {
            val fake = FakeWebserverCredentialStore()
            fake.saveUser("bob")
            fake.savePassword("hunter2")

            val tmpDir = createTempDirectory(prefix = "app-config-init-test")
            val file = File(tmpDir.toString(), "preferences.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(scope = this, produceFile = { file })
            val repository = AppConfigRepository(dataStore = dataStore, credentialStore = fake)

            assertEquals("bob", repository.webserverUser.first())
            assertEquals("hunter2", repository.webserverPassword.first())

            tmpDir.deleteRecursively()
        }

    @OptIn(ExperimentalPathApi::class)
    @Test
    @Disabled("Reason: reset-to-defaults logic currently removed from code")
    fun `reset webserver defaults restores default URL and default admin password hash`() = runTest {
        val tmpDir = createTempDirectory(prefix = "app-config-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = this, produceFile = { file })
        val repository = AppConfigRepository(dataStore = dataStore, credentialStore = FakeWebserverCredentialStore())

        repository.updateUrl("https://example.com/custom")
        repository.updateAdminPassword("super-secret")

        assertEquals(BuildConfig.WEBVIEW_URL, repository.currentUrl.first())
        assertEquals(
            repository.hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD),
            repository.adminPasswordHash.first()
                    )

        tmpDir.deleteRecursively()
    }
}
