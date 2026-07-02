package hka.awp.cgi.temi.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.AdminPanelPatrolSettingsDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.security.MessageDigest

private const val DEFAULT_DRIVE_FOLDER_LINK = BuildConfig.DEFAULT_DRIVE_FOLDER_LINK
private const val DEFAULT_DRIVE_UPLOAD_URL = BuildConfig.DEFAULT_DRIVE_UPLOAD_URL

/**
 * Contract for storing and retrieving plaintext webserver credentials.
 * Abstracted so [AppConfigRepository] can be unit-tested without Android instrumentation —
 * tests inject [FakeWebserverCredentialStore]; production wires [EncryptedWebserverCredentialStore].
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
        prefs.edit().putString(KEY_USER, user).apply()
    }

    override fun savePassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    companion object {
        private const val KEY_USER = "user"
        private const val KEY_PASSWORD = "password"
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

/**
 * Central repository for managing and persisting application configurations[cite: 1].
 *
 * Uses Jetpack [DataStore] for general settings (e.g., routes, password hashes, webviews)[cite: 1]
 * and a [WebserverCredentialStore] for sensitive plaintext data (e.g., server login credentials)[cite: 1].
 *
 * @property dataStore The [DataStore] instance used for persistent key-value pairs[cite: 1].
 * @property credentialStore The encrypted or fake storage backend for sensitive credentials[cite: 1].
 */
@Suppress("TooManyFunctions")
class AppConfigRepository private constructor(
    private val dataStore: DataStore<Preferences>,
    private val credentialStore: WebserverCredentialStore
) {
    companion object {
        const val ROUTE_SEPARATOR = "|"
        const val COMMA_SEPARATOR = ","

        const val DEFAULT_LATITUDE = 49.0138
        const val DEFAULT_LONGITUDE = 8.3573

        const val DEFAULT_PATROL_ENABLED = false
        const val DEFAULT_MIN_MINUTES = 40
        const val DEFAULT_MAX_MINUTES = 60

        const val DEFAULT_SPEAKER_VERIFICATION_THRESHOLD = 0.82

        /**
         * Creates a production instance of the repository using an [EncryptedWebserverCredentialStore][cite: 1].
         *
         * @param context The Android context required to initialize encrypted shared preferences[cite: 1].
         * @param dataStore The [DataStore] instance for application configurations[cite: 1].
         */
        operator fun invoke(context: Context, dataStore: DataStore<Preferences>) =
            AppConfigRepository(dataStore, EncryptedWebserverCredentialStore(context))

        /**
         * Creates a test instance of the repository[cite: 1].
         * Allows passing any [WebserverCredentialStore] implementation (e.g., test fakes)[cite: 1].
         *
         * @param dataStore The [DataStore] instance for application configurations[cite: 1].
         * @param credentialStore The credential storage implementation to use (e.g., [FakeWebserverCredentialStore]).
         */
        operator fun invoke(dataStore: DataStore<Preferences>, credentialStore: WebserverCredentialStore) =
            AppConfigRepository(dataStore, credentialStore)
    }

    private val webviewUrlKey = stringPreferencesKey("webview_url")
    private val latitudeKey = doublePreferencesKey("latitude")
    private val longitudeKey = doublePreferencesKey("longitude")

    private val adminPanelPasswordHashKey = stringPreferencesKey("admin_panel_password_hash")

    private val webserverVerificationEnabledKey = booleanPreferencesKey("webserver_verification_enabled")
    private val webserverPasswordHashKey = stringPreferencesKey("webserver_password_hash")

    private val webserverUserHashKey = stringPreferencesKey("webserver_user_hash")

    private val keyIsPatrolEnabled = booleanPreferencesKey("is_patrol_enabled")
    private val keyPatrolMode = stringPreferencesKey("patrol_mode")
    private val keyMinMinutes = intPreferencesKey("min_minutes")
    private val keyMaxMinutes = intPreferencesKey("max_minutes")
    private val keySelectedHours = stringPreferencesKey("selected_hours")
    private val keyPatrolRoute = stringPreferencesKey("patrol_route")

    private val adminPasswordHashKey = stringPreferencesKey("admin_password_hash")
    private val adminPasswordLegacyKey = stringPreferencesKey("admin_password")

    private val photoboxOverlayEnabledKey = booleanPreferencesKey("photobox_overlay_enabled")
    private val photoboxOverlayPositionKey = stringPreferencesKey("photobox_overlay_position")
    private val photoboxBannerEnabledKey = booleanPreferencesKey("photobox_banner_enabled")
    private val photoboxBannerKey = stringPreferencesKey("photobox_banner")
    private val driveFolderLinkKey = stringPreferencesKey("photobox_drive_folder_link")
    private val driveUploadUrlKey = stringPreferencesKey("photobox_drive_upload_url")

    private val speakerVerificationEnabledKey = booleanPreferencesKey("speaker_verification_enabled")
    private val speakerVerificationThresholdKey = doublePreferencesKey("speaker_verification_threshold")

    /** Stream of the currently configured Webview URL. Falls back to the build default if not set[cite: 1]. */
    val currentUrl: Flow<String> = dataStore.data.map {
        it[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL
    }

    private val _webserverUser = MutableStateFlow(credentialStore.getUser())

    /** Stream of the unencrypted webserver username[cite: 1]. */
    val webserverUser: Flow<String> = _webserverUser.asStateFlow()

    private val _webserverPassword = MutableStateFlow(credentialStore.getPassword())

    /** Stream of the unencrypted webserver password[cite: 1]. */
    val webserverPassword: Flow<String> = _webserverPassword.asStateFlow()

    /**
     * Updates the Webview URL in the [DataStore][cite: 1].
     * @param newUrl The new target URL for the webview[cite: 1].
     */
    suspend fun updateUrl(newUrl: String) {
        dataStore.edit {
            it[webviewUrlKey] = newUrl
        }
    }

    /** Stream of the latitude configuration for positioning[cite: 1]. */
    val latitude: Flow<Double> = dataStore.data.map {
        it[latitudeKey] ?: DEFAULT_LATITUDE
    }

    /** Stream of the longitude configuration for positioning[cite: 1]. */
    val longitude: Flow<Double> = dataStore.data.map {
        it[longitudeKey] ?: DEFAULT_LONGITUDE
    }

    /**
     * Updates the geographic coordinates[cite: 1].
     * @param latitude The new latitude value[cite: 1].
     * @param longitude The new longitude value[cite: 1].
     */
    suspend fun updateCoordinates(latitude: Double, longitude: Double) {
        dataStore.edit {
            it[latitudeKey] = latitude
            it[longitudeKey] = longitude
        }
    }

    /** Stream of the SHA-256 hash of the admin panel password, taking legacy keys into account[cite: 1]. */
    val adminPanelPasswordHash: Flow<String> = dataStore.data.map {
        it[adminPanelPasswordHashKey]
            ?: it[adminPasswordHashKey]
            ?: it[adminPasswordLegacyKey]?.let(::hashPassword)
            ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    /** Stream of the SHA-256 hash of the webserver password[cite: 1]. */
    val webserverPasswordHash: Flow<String> = dataStore.data.map {
        it[webserverPasswordHashKey] ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    /** Stream of the SHA-256 hash of the webserver username[cite: 1]. */
    val webserverUserHash: Flow<String> = dataStore.data.map {
        it[webserverUserHashKey] ?: ""
    }

    /** Alias for [adminPanelPasswordHash][cite: 1]. */
    val adminPasswordHash: Flow<String> = adminPanelPasswordHash

    /**
     * Updates the password for the admin panel and removes old legacy entries[cite: 1].
     * @param password The new plaintext password[cite: 1].
     */
    suspend fun updateAdminPanelPassword(password: String) {
        dataStore.edit {
            it[adminPanelPasswordHashKey] = hashPassword(password)
            it.remove(adminPasswordHashKey)
            it.remove(adminPasswordLegacyKey)
        }
    }

    /** Alias for [updateAdminPanelPassword][cite: 1]. */
    suspend fun updateAdminPassword(password: String) {
        updateAdminPanelPassword(password)
    }

    /**
     * Updates the webserver password. Stores the hash in the [DataStore] and the plaintext password in the
     * [credentialStore][cite: 1].
     * @param password The new plaintext webserver password[cite: 1].
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
     * [credentialStore][cite: 1].
     * @param user The new plaintext webserver username[cite: 1].
     */
    suspend fun updateWebserverUser(user: String) {
        dataStore.edit {
            it[webserverUserHashKey] = hashPassword(user)
        }
        credentialStore.saveUser(user)
        _webserverUser.value = user
    }

    /**
     * Enables or disables verification for the webserver[cite: 1].
     * @param enabled `true` to enable, `false` to disable, `null` to ignore the change[cite: 1].
     */
    suspend fun updateWebserverVerification(
        enabled: Boolean? = null
    ) {
        dataStore.edit {
            enabled?.let { value -> it[webserverVerificationEnabledKey] = value }
        }
    }

    /** Stream indicating whether webserver verification is enabled[cite: 1]. */
    val isWebserverVerificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[webserverVerificationEnabledKey] ?: false
    }

    /**
     * Checks if an entered plaintext password matches a given hash[cite: 1].
     * @param plainPassword The plaintext password to check[cite: 1].
     * @param currentHash The target hash to compare against[cite: 1].
     * @return `true` if the hashes match, `false` otherwise[cite: 1].
     */
    fun isValidPassword(plainPassword: String, currentHash: String): Boolean {
        return hashPassword(plainPassword) == currentHash
    }

    /**
     * Hashes a given string using **SHA-256**[cite: 1].
     * @param password The text string (e.g., a password) to hash[cite: 1].
     * @return The generated hex string representation of the hash[cite: 1].
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    /** Stream indicating whether the patrol mode is globally enabled[cite: 1]. */
    val isPatrolEnabled: Flow<Boolean> = dataStore.data.map {
        it[keyIsPatrolEnabled] ?: DEFAULT_PATROL_ENABLED
    }

    /** Stream of the current patrol mode configuration. Falls back to [AdminPanelPatrolSettingsDialog.RANDOM]
     * upon error.
     */
    val patrolMode: Flow<AdminPanelPatrolSettingsDialog> = dataStore.data.map { preferences ->
        val name = preferences[keyPatrolMode] ?: AdminPanelPatrolSettingsDialog.RANDOM.name

        try {
            AdminPanelPatrolSettingsDialog.valueOf(name)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Ungültiger Patrol-Modus: $name. Setze auf RANDOM.")
            AdminPanelPatrolSettingsDialog.RANDOM
        }
    }

    /** Stream for the minimum duration of a patrol sequence in minutes[cite: 1]. */
    val minPatrolMinutes: Flow<Int> = dataStore.data.map {
        it[keyMinMinutes] ?: DEFAULT_MIN_MINUTES
    }

    /** Stream for the maximum duration of a patrol sequence in minutes[cite: 1]. */
    val maxPatrolMinutes: Flow<Int> = dataStore.data.map {
        it[keyMaxMinutes] ?: DEFAULT_MAX_MINUTES
    }

    /** Stream of selected hours (times of day) scheduled for patrolling[cite: 1]. */
    val selectedPatrolHours: Flow<Set<Int>> = dataStore.data.map {
        it[keySelectedHours]
            ?.split(COMMA_SEPARATOR)
            ?.mapNotNull { hour -> hour.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    /** Stream of the defined waypoints/locations making up the patrol route[cite: 1]. */
    val patrolRoute: Flow<List<String>> = dataStore.data.map {
        it[keyPatrolRoute]
            ?.split(ROUTE_SEPARATOR)
            ?.filter { location -> location.isNotBlank() }
            ?: emptyList()
    }

    /**
     * Updates the core settings for the Temi robot patrol functionality[cite: 1].
     * @param isEnabled Flag to globally toggle patrol mode[cite: 1].
     * @param mode The selected behavior mode ([AdminPanelPatrolSettingsDialog])[cite: 1].
     * @param minMin Minimum pause or driving duration in minutes[cite: 1].
     * @param maxMin Maximum pause or driving duration in minutes[cite: 1].
     * @param hours The set of active hours/intervals[cite: 1].
     */
    suspend fun updatePatrolSettings(
        isEnabled: Boolean,
        mode: AdminPanelPatrolSettingsDialog,
        minMin: Int,
        maxMin: Int,
        hours: Set<Int>
    ) {
        dataStore.edit {
            it[keyIsPatrolEnabled] = isEnabled
            it[keyPatrolMode] = mode.name
            it[keyMinMinutes] = minMin
            it[keyMaxMinutes] = maxMin
            it[keySelectedHours] = hours.joinToString(COMMA_SEPARATOR)
        }
    }

    /**
     * Compiles and saves a list of waypoints as a single separated string for the route[cite: 1].
     * @param route The list of location names or waypoint identifiers[cite: 1].
     */
    suspend fun updatePatrolRoute(route: List<String>) {
        dataStore.edit {
            it[keyPatrolRoute] = route.joinToString(ROUTE_SEPARATOR)
        }
    }

    /** Stream indicating whether biometric speaker verification (audio) is enabled[cite: 1]. */
    val isSpeakerVerificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[speakerVerificationEnabledKey] ?: false
    }

    /** Stream of the confidence threshold value required for successful speaker verification[cite: 1]. */
    val speakerVerificationThreshold: Flow<Double> = dataStore.data.map {
        it[speakerVerificationThresholdKey] ?: DEFAULT_SPEAKER_VERIFICATION_THRESHOLD
    }

    /**
     * Updates the configuration parameters for speaker verification[cite: 1].
     * @param enabled If provided, updates the activation status[cite: 1].
     * @param threshold If provided, updates the confidence threshold (automatically coerced between `0.0` and `1.0`).
     */
    suspend fun updateSpeakerVerification(
        enabled: Boolean? = null,
        threshold: Double? = null
    ) {
        dataStore.edit {
            enabled?.let { value -> it[speakerVerificationEnabledKey] = value }
            threshold?.let { value ->
                it[speakerVerificationThresholdKey] = value.coerceIn(0.0, 1.0)
            }
        }
    }

    /** Stream indicating whether the Photobox overlay graphic should be displayed[cite: 1]. */
    val photoboxOverlayEnabled: Flow<Boolean> = dataStore.data.map {
        it[photoboxOverlayEnabledKey] ?: false
    }

    /** Stream of the layout position configuration string for the Photobox overlay[cite: 1]. */
    val photoboxOverlayPosition: Flow<String> = dataStore.data.map {
        it[photoboxOverlayPositionKey] ?: ""
    }

    /**
     * Configures the visibility status and placement position of the Photobox overlay[cite: 1].
     * @param enabled Visibility state of the overlay[cite: 1].
     * @param position Position string identifier (e.g., "TOP_LEFT")[cite: 1].
     */
    suspend fun setPhotoboxOverlay(enabled: Boolean, position: String) {
        dataStore.edit {
            it[photoboxOverlayEnabledKey] = enabled
            it[photoboxOverlayPositionKey] = position
        }
    }

    /** Stream indicating whether the promotional/informational Photobox banner is active[cite: 1]. */
    val photoboxBannerEnabled: Flow<Boolean> = dataStore.data.map {
        it[photoboxBannerEnabledKey] ?: false
    }

    /** Stream of the content value or path linked to the Photobox banner[cite: 1]. */
    val photoboxBanner: Flow<String> = dataStore.data.map {
        it[photoboxBannerKey] ?: ""
    }

    /**
     * Configures the operational state and content of the Photobox advertisement/info banner[cite: 1].
     * @param enabled Activation state of the banner[cite: 1].
     * @param banner Content string, ID, or URI for the banner asset[cite: 1].
     */
    suspend fun setPhotoboxBanner(enabled: Boolean, banner: String) {
        dataStore.edit {
            it[photoboxBannerEnabledKey] = enabled
            it[photoboxBannerKey] = banner
        }
    }

    /** Stream of the shared link pointing to the Photobox's Google Drive display folder[cite: 1]. */
    val driveFolderLink: Flow<String> = dataStore.data.map {
        it[driveFolderLinkKey] ?: DEFAULT_DRIVE_FOLDER_LINK
    }

    /** Stream of the API endpoint URL (e.g., Google Apps Script) used for handling image uploads[cite: 1]. */
    val driveUploadUrl: Flow<String> = dataStore.data.map {
        it[driveUploadUrlKey] ?: DEFAULT_DRIVE_UPLOAD_URL
    }

    /**
     * Updates the cloud storage (Google Drive) setup configurations for the Photobox feature[cite: 1].
     * @param folderLink The public URL linking to the viewing folder (optional)[cite: 1].
     * @param uploadUrl The API backend endpoint for image transmission processing (optional)[cite: 1].
     */
    suspend fun setDriveSettings(folderLink: String? = null, uploadUrl: String? = null) {
        dataStore.edit {
            folderLink?.let { value -> it[driveFolderLinkKey] = value }
            uploadUrl?.let { value -> it[driveUploadUrlKey] = value }
        }
    }

    /**
     * Irrevocably clears all settings currently saved inside the [DataStore] instance[cite: 1].
     */
    @Suppress("unused")
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
