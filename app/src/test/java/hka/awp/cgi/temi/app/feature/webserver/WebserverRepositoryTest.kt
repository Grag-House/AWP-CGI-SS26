package hka.awp.cgi.temi.app.feature.webserver

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.utils.AppConfigRepository
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
    private lateinit var repository: AppConfigRepository
    private lateinit var datastore: DataStore<Preferences>

    @BeforeEach
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        datastore = mockk<DataStore<Preferences>>(relaxed = true)
        every { datastore.data } returns flowOf(emptyPreferences())
        repository = AppConfigRepository(context, dataStore = datastore)
    }

    @AfterEach
    fun tearDown() {
        // clear all mocks after each test to avoid interference between tests
        unmockkAll()
    }

    @Test
    fun `when DataStore is empty, it returns the BuildConfig fallback URL`() = runTest {
        val expected = BuildConfig.WEBVIEW_URL
        val result = repository.currentUrl.first()
        assertEquals(expected, result)
    }

    @Test
    fun `when DataStore has a valid URL, it returns the host of that URL`() = runTest {
        val key = stringPreferencesKey("webview_url")
        val value = "https://example.com"

        datastore = mockk<DataStore<Preferences>>(relaxed = true)
        every { datastore.data } returns flowOf(preferencesOf(key to value))
        repository = AppConfigRepository(dataStore = datastore)

        assertEquals(value, repository.currentUrl.first())
    }

    // we use a real  DataStore here, since it is too annoying to mock the edit function
    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `when the URL in DataStore is  updated, it returns the new host`() = runTest {
        // temp file + dir
        val tmpDir = createTempDirectory(prefix = "datastore-test")
        val file = File(tmpDir.toString(), "preferences.preferences_pb")

        // create a real Preferences DataStore that the test controls via the test scope
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { file }
        )
        val context: Context = ApplicationProvider.getApplicationContext()
        val repository = AppConfigRepository(context, dataStore)

        // initial: should read fallback from BuildConfig
        val expectedFallback = BuildConfig.WEBVIEW_URL
        assertEquals(expectedFallback, repository.currentUrl.first())

        val newValue = "https://example.com/path"

        // update the value
        repository.updateUrl(newValue)

        // DataStore will persist and emit the new value
        assertEquals(newValue, repository.currentUrl.first())

        // cleanup
        tmpDir.deleteRecursively()
    }
}
