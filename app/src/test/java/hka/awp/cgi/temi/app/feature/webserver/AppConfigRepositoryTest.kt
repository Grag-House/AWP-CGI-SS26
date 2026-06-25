package hka.awp.cgi.temi.app.feature.webserver

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

class AppConfigRepositoryTest {

    // Helper für DataStore Setup um Redundanz zu vermeiden
    @OptIn(ExperimentalPathApi::class)
    private fun createTestRepository(
        scope: kotlinx.coroutines.CoroutineScope
    ): Pair<AppConfigRepository, java.nio.file.Path> {
        val tmpDir = createTempDirectory(prefix = "app-config-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        return AppConfigRepository(dataStore) to tmpDir
    }

    @Test
    fun `migration transfers legacy password to both new keys`() = runTest {
        val (repository, tmpDir) = createTestRepository(this)

        // 1. Legacy Passwort setzen
        // val legacyKey = stringPreferencesKey("admin_password")
        // Wir müssen hier direkt auf das DataStore zugreifen, da der Key private ist
        // Tipp: Überlege, den Key im Repository internal statt private zu machen
        // oder eine Methode für den Test zu ergänzen.
        // Alternativ hier der manuelle Edit:
        // (Für Testzwecke: nutze reflection oder mache den Key internal)

        // Simuliere Migration
        repository.performMigrationIfNeeded()

        // Assertions für beide neuen Hashes...

        tmpDir.deleteRecursively()
    }

    @Test
    fun `admin panel and webserver passwords are independent`() = runTest {
        val (repository, tmpDir) = createTestRepository(this)

        val adminPass = "admin123"
        val webPass = "web456"

        repository.updateAdminPanelPassword(adminPass)
        repository.updateWebserverPassword(webPass)

    @OptIn(ExperimentalPathApi::class)
    @Test
    @Disabled("Reason: logic currently removed from code")
    fun `reset webserver defaults restores default url and default admin password hash`() = runTest {
        val tmpDir = createTempDirectory(prefix = "app-config-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val repository = AppConfigRepository(dataStore)
        val adminHash = repository.adminPanelPasswordHash.first()
        val webHash = repository.webserverPasswordHash.first()

        assertEquals(repository.hashPassword(adminPass), adminHash)
        assertEquals(repository.hashPassword(webPass), webHash)
        assertTrue(adminHash != webHash)
        repository.updateUrl("https://example.com/custom")
        repository.updateAdminPassword("super-secret")

        assertEquals(BuildConfig.WEBVIEW_URL, repository.currentUrl.first())
        val expectedDefaultHash = repository.hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
        assertEquals(expectedDefaultHash, repository.adminPasswordHash.first())

        tmpDir.deleteRecursively()
    }
}
