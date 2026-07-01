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

        /** Production: wires [EncryptedWebserverCredentialStore] automatically. */
        operator fun invoke(context: Context, dataStore: DataStore<Preferences>) =
            AppConfigRepository(dataStore, EncryptedWebserverCredentialStore(context))

        /** Testing: accepts any [WebserverCredentialStore] implementation, no Context needed. */
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

    val currentUrl: Flow<String> = dataStore.data.map {
        it[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL
    }

    private val _webserverUser = MutableStateFlow(credentialStore.getUser())
    val webserverUser: Flow<String> = _webserverUser.asStateFlow()

    private val _webserverPassword = MutableStateFlow(credentialStore.getPassword())
    val webserverPassword: Flow<String> = _webserverPassword.asStateFlow()

    suspend fun updateUrl(newUrl: String) {
        dataStore.edit {
            it[webviewUrlKey] = newUrl
        }
    }

    val latitude: Flow<Double> = dataStore.data.map {
        it[latitudeKey] ?: DEFAULT_LATITUDE
    }

    val longitude: Flow<Double> = dataStore.data.map {
        it[longitudeKey] ?: DEFAULT_LONGITUDE
    }

    suspend fun updateCoordinates(latitude: Double, longitude: Double) {
        dataStore.edit {
            it[latitudeKey] = latitude
            it[longitudeKey] = longitude
        }
    }

    val adminPanelPasswordHash: Flow<String> = dataStore.data.map {
        it[adminPanelPasswordHashKey]
            ?: it[adminPasswordHashKey]
            ?: it[adminPasswordLegacyKey]?.let(::hashPassword)
            ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    val webserverPasswordHash: Flow<String> = dataStore.data.map {
        it[webserverPasswordHashKey] ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    val webserverUserHash: Flow<String> = dataStore.data.map {
        it[webserverUserHashKey] ?: ""
    }

    val adminPasswordHash: Flow<String> = adminPanelPasswordHash

    suspend fun updateAdminPanelPassword(password: String) {
        dataStore.edit {
            it[adminPanelPasswordHashKey] = hashPassword(password)
            it.remove(adminPasswordHashKey)
            it.remove(adminPasswordLegacyKey)
        }
    }

    suspend fun updateAdminPassword(password: String) {
        updateAdminPanelPassword(password)
    }

    suspend fun updateWebserverPassword(password: String) {
        dataStore.edit {
            it[webserverPasswordHashKey] = hashPassword(password)
        }
        credentialStore.savePassword(password)
        _webserverPassword.value = password
    }

    suspend fun updateWebserverUser(user: String) {
        dataStore.edit {
            it[webserverUserHashKey] = hashPassword(user)
        }
        credentialStore.saveUser(user)
        _webserverUser.value = user
    }
    suspend fun updateWebserverVerification(
        enabled: Boolean? = null
                                           ) {
        dataStore.edit {
            enabled?.let { value -> it[webserverVerificationEnabledKey] = value }
        }
    }

    val isWebserverVerificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[webserverVerificationEnabledKey] ?: false
    }

    fun isValidPassword(plainPassword: String, currentHash: String): Boolean {
        return hashPassword(plainPassword) == currentHash
    }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    val isPatrolEnabled: Flow<Boolean> = dataStore.data.map {
        it[keyIsPatrolEnabled] ?: DEFAULT_PATROL_ENABLED
    }

    val patrolMode: Flow<AdminPanelPatrolSettingsDialog> = dataStore.data.map { preferences ->
        val name = preferences[keyPatrolMode] ?: AdminPanelPatrolSettingsDialog.RANDOM.name

        try {
            AdminPanelPatrolSettingsDialog.valueOf(name)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Ungültiger Patrol-Modus: $name. Setze auf RANDOM.")
            AdminPanelPatrolSettingsDialog.RANDOM
        }
    }

    val minPatrolMinutes: Flow<Int> = dataStore.data.map {
        it[keyMinMinutes] ?: DEFAULT_MIN_MINUTES
    }

    val maxPatrolMinutes: Flow<Int> = dataStore.data.map {
        it[keyMaxMinutes] ?: DEFAULT_MAX_MINUTES
    }

    val selectedPatrolHours: Flow<Set<Int>> = dataStore.data.map {
        it[keySelectedHours]
            ?.split(COMMA_SEPARATOR)
            ?.mapNotNull { hour -> hour.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    val patrolRoute: Flow<List<String>> = dataStore.data.map {
        it[keyPatrolRoute]
            ?.split(ROUTE_SEPARATOR)
            ?.filter { location -> location.isNotBlank() }
            ?: emptyList()
    }

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

    suspend fun updatePatrolRoute(route: List<String>) {
        dataStore.edit {
            it[keyPatrolRoute] = route.joinToString(ROUTE_SEPARATOR)
        }
    }

    val isSpeakerVerificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[speakerVerificationEnabledKey] ?: false
    }

    val speakerVerificationThreshold: Flow<Double> = dataStore.data.map {
        it[speakerVerificationThresholdKey] ?: DEFAULT_SPEAKER_VERIFICATION_THRESHOLD
    }

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

    val photoboxOverlayEnabled: Flow<Boolean> = dataStore.data.map {
        it[photoboxOverlayEnabledKey] ?: false
    }

    val photoboxOverlayPosition: Flow<String> = dataStore.data.map {
        it[photoboxOverlayPositionKey] ?: ""
    }

    suspend fun setPhotoboxOverlay(enabled: Boolean, position: String) {
        dataStore.edit {
            it[photoboxOverlayEnabledKey] = enabled
            it[photoboxOverlayPositionKey] = position
        }
    }

    val photoboxBannerEnabled: Flow<Boolean> = dataStore.data.map {
        it[photoboxBannerEnabledKey] ?: false
    }

    val photoboxBanner: Flow<String> = dataStore.data.map {
        it[photoboxBannerKey] ?: ""
    }

    suspend fun setPhotoboxBanner(enabled: Boolean, banner: String) {
        dataStore.edit {
            it[photoboxBannerEnabledKey] = enabled
            it[photoboxBannerKey] = banner
        }
    }

    val driveFolderLink: Flow<String> = dataStore.data.map {
        it[driveFolderLinkKey] ?: DEFAULT_DRIVE_FOLDER_LINK
    }

    val driveUploadUrl: Flow<String> = dataStore.data.map {
        it[driveUploadUrlKey] ?: DEFAULT_DRIVE_UPLOAD_URL
    }

    suspend fun setDriveSettings(folderLink: String? = null, uploadUrl: String? = null) {
        dataStore.edit {
            folderLink?.let { value -> it[driveFolderLinkKey] = value }
            uploadUrl?.let { value -> it[driveUploadUrlKey] = value }
        }
    }

    @Suppress("unused")
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
