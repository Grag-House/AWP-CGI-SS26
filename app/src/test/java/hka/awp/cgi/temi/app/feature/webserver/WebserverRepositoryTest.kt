package hka.awp.cgi.temi.app.feature.webserver

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

class WebserverRepositoryTest {
    private lateinit var repository: WebserverConfigRepository
    private lateinit var datastore: DataStore<Preferences>

    @BeforeEach
    fun setup() {
        datastore = mockk<DataStore<Preferences>>(relaxed = true)
        every { datastore.data } returns flowOf(emptyPreferences())
        repository =
            WebserverConfigRepository.Companion(dataStore = datastore, credentialStore = FakeWebserverCredentialStore())
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when DataStore is empty, it returns the BuildConfig fallback URL`() = runTest {
        assertEquals(BuildConfig.WEBVIEW_URL, repository.currentUrl.first())
    }

    @Test
    fun `when DataStore has a stored URL, it returns that URL`() = runTest {
        val key = stringPreferencesKey("webview_url")
        val value = "https://example.com"
        every { datastore.data } returns flowOf(preferencesOf(key to value))
        repository =
            WebserverConfigRepository.Companion(dataStore = datastore, credentialStore = FakeWebserverCredentialStore())

        assertEquals(value, repository.currentUrl.first())
    }

    // Real DataStore used here — mocking DataStore.edit() is too fragile
    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `when the URL in DataStore is updated, the flow emits the new value`() = runTest {
        val tmpDir = createTempDirectory(prefix = "datastore-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = this, produceFile = { file })
        val repository =
            WebserverConfigRepository.Companion(dataStore = dataStore, credentialStore = FakeWebserverCredentialStore())

        repository.updateUrl("https://example.com/path")

        assertEquals("https://example.com/path", repository.currentUrl.first())

        tmpDir.deleteRecursively()
    }

    @Test
    fun `webserverVerificationEnabled defaults to false when DataStore is empty`() = runTest {
        assertEquals(false, repository.isWebserverVerificationEnabled.first())
    }
}

/**
 * In-memory fake for unit tests — no Android runtime or encryption needed.
 */
class FakeWebserverCredentialStore : WebserverCredentialStore {
    private var user: String = ""
    private var password: String = ""

    override fun getUser(): String = user
    override fun getPassword(): String = password
    override fun saveUser(user: String) {
        this.user = user
    }

    override fun savePassword(password: String) {
        this.password = password
    }
}
