package hka.awp.cgi.temi.app.feature.webserver

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Contract for storing and retrieving plaintext webserver credentials.
 * Abstracted so WebserverConfigReopsitory can be unit-tested without Android instrumentation —
 * tests inject FakeWebserverCredentialStore (from the test classes); production wires [EncryptedWebserverCredentialStore].
 */
interface WebserverCredentialStore {
    fun getUser(): String
    fun getPassword(): String
    fun saveUser(user: String)
    fun savePassword(password: String)
}

/**
 * Production implementation backed by [EncryptedSharedPreferences].
 * Keys and values are encrypted at rest using AES256.
 */

// We use EncryptedSharedPreferences for simplicity, even though it's deprecated in favor of Jetpack Security Crypto.
@Suppress("DEPRECATION")
class EncryptedWebserverCredentialStore(context: Context) : WebserverCredentialStore {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "webserver_credentials",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                                                         )

    override fun getUser(): String = prefs.getString(KEY_USER, "") ?: ""
    override fun getPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""
    override fun saveUser(user: String) {
        CoroutineScope(Dispatchers.IO).launch {
            prefs.edit().putString(KEY_USER, user).apply()
        }
    }

    override fun savePassword(password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            prefs.edit().putString(KEY_PASSWORD, password).apply()
        }
    }

    companion object {
        private const val KEY_USER = "user"
        private const val KEY_PASSWORD = "password"
    }
}

class WebserverConfigRepository private constructor(
    private val dataStore: DataStore<Preferences>,
    private val credentialStore: WebserverCredentialStore
                                                   ) {

    companion object {
        /**
         * Creates a production instance of the repository using an [EncryptedWebserverCredentialStore].
         *
         * @param context The Android context required to initialize encrypted shared preferences.
         * @param dataStore The [DataStore] instance for application configurations.
         */
        operator fun invoke(context: Context, dataStore: DataStore<Preferences>) =
            WebserverConfigRepository(dataStore, EncryptedWebserverCredentialStore(context))

        /**
         * Creates a test instance of the repository.
         * Allows passing any [WebserverCredentialStore] implementation (e.g., test fakes).
         *
         * @param dataStore The [DataStore] instance for application configurations.
         * @param credentialStore The credential storage implementation to use (e.g., [FakeWebserverCredentialStore]).
         */
        operator fun invoke(dataStore: DataStore<Preferences>, credentialStore: WebserverCredentialStore) =
            WebserverConfigRepository(dataStore, credentialStore)
    }

    private val webviewUrlKey = stringPreferencesKey("webview_url")

    private val webserverVerificationEnabledKey = booleanPreferencesKey("webserver_verification_enabled")
    private val webserverPasswordHashKey = stringPreferencesKey("webserver_password_hash")

    private val webserverUserHashKey = stringPreferencesKey("webserver_user_hash")

    /** Stream of the currently configured Webview URL. Falls back to the build default if not set. */
    val currentUrl: Flow<String> = dataStore.data.map {
        it[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL
    }

    private val _webserverUser = MutableStateFlow(credentialStore.getUser())

    /** Stream of the unencrypted webserver username. */
    val webserverUser: Flow<String> = _webserverUser.asStateFlow()

    private val _webserverPassword = MutableStateFlow(credentialStore.getPassword())

    /** Stream of the unencrypted webserver password. */
    val webserverPassword: Flow<String> = _webserverPassword.asStateFlow()

    /** Stream of the SHA-256 hash of the webserver password. */
    val webserverPasswordHash: Flow<String> = dataStore.data.map {
        it[webserverPasswordHashKey] ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    /**
     * Updates the Webview URL in the [DataStore].
     * @param newUrl The new target URL for the webview.
     */
    suspend fun updateUrl(newUrl: String) {
        dataStore.edit {
            it[webviewUrlKey] = newUrl
        }
    }

    /**
     * Updates the webserver password. Stores the hash in the [DataStore] and the plaintext password in the
     * [credentialStore].
     * @param password The new plaintext webserver password.
     */
    suspend fun updateWebserverPassword(password: String) {
        dataStore.edit {
            it[webserverPasswordHashKey] = hashPassword(password)
        }
        credentialStore.savePassword(password)
        _webserverPassword.value = password
    }

    /**
     * Updates the webserver username. Stores the hash in the [DataStore] and the plaintext username in the
     * [credentialStore].
     * @param user The new plaintext webserver username.
     */
    suspend fun updateWebserverUser(user: String) {
        dataStore.edit {
            it[webserverUserHashKey] = hashPassword(user)
        }
        credentialStore.saveUser(user)
        _webserverUser.value = user
    }

    /**
     * Enables or disables verification for the webserver.
     * @param enabled `true` to enable, `false` to disable, `null` to ignore the change.
     */
    suspend fun updateWebserverVerification(
        enabled: Boolean? = null
                                           ) {
        dataStore.edit {
            enabled?.let { value -> it[webserverVerificationEnabledKey] = value }
        }
    }

    /** Stream indicating whether webserver verification is enabled. */
    val isWebserverVerificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[webserverVerificationEnabledKey] ?: false
    }

    /**
     * Hashes a given string using **SHA-256**.
     * @param password The text string (e.g., a password) to hash.
     * @return The generated hex string representation of the hash.
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }
}
