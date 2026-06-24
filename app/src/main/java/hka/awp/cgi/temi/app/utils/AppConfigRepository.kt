package hka.awp.cgi.temi.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private const val DEFAULT_DRIVE_FOLDER_LINK = BuildConfig.DEFAULT_DRIVE_FOLDER_LINK
private const val DEFAULT_DRIVE_UPLOAD_URL = BuildConfig.DEFAULT_DRIVE_UPLOAD_URL
// DEFAULT_DRIVE_FOLDER_LINK=https://drive.google.com/drive/folders/1k8g1Yqg8wMwvgY8urcnDer1RYke5voAp?usp=drive_link
// DEFAULT_DRIVE_UPLOAD_URL=https://script.google.com/macros/s/AKfycbxBnCIKutMCfloxpVtW50EIyFF45z3OGsY-t4bTGQYqvTQABHB-taPuWtyP2BelWLJ9sQ/exec
// für die .env zum kopieren
// TODO
/**
 * Central repository for application configurations stored in Jetpack DataStore.
 * Handles URL settings, coordinates for weather, and admin credentials.
 */
class AppConfigRepository(private val dataStore: DataStore<Preferences>) {
    private val webviewUrlKey = stringPreferencesKey("webview_url")
    private val latitudeKey = doublePreferencesKey("latitude")
    private val longitudeKey = doublePreferencesKey("longitude")
    private val adminPasswordHashKey = stringPreferencesKey("admin_password_hash")
    private val adminPasswordLegacyKey = stringPreferencesKey("admin_password")
    private val photoboxOverlayEnabledKey = booleanPreferencesKey("photobox_overlay_enabled")
    private val photoboxOverlayPositionKey = stringPreferencesKey("photobox_overlay_position")
    private val driveFolderLinkKey = stringPreferencesKey("photobox_drive_folder_link")
    private val driveUploadUrlKey = stringPreferencesKey("photobox_drive_upload_url")

    // --- Webview URL ---

    val currentUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL
    }

    suspend fun updateUrl(newUrl: String) {
        dataStore.edit { preferences ->
            preferences[webviewUrlKey] = newUrl
        }
    }

    // --- Coordinates ---

    @Suppress("MagicNumber")
    val latitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[latitudeKey] ?: 49.0138 // Default Karlsruhe
    }

    @Suppress("MagicNumber")
    val longitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[longitudeKey] ?: 8.3573 // Default Karlsruhe
    }

    suspend fun updateCoordinates(latitude: Double, longitude: Double) {
        dataStore.edit { preferences ->
            preferences[latitudeKey] = latitude
            preferences[longitudeKey] = longitude
        }
    }

    // --- Admin Password ---

    val adminPasswordHash: Flow<String> = dataStore.data.map { preferences ->
        preferences[adminPasswordHashKey]
            ?: preferences[adminPasswordLegacyKey]?.let(::hashPassword)
            ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    suspend fun updateAdminPassword(password: String) {
        val hash = hashPassword(password)
        dataStore.edit { preferences ->
            preferences[adminPasswordHashKey] = hash
            preferences.remove(adminPasswordLegacyKey)
        }
    }

    suspend fun resetWebserverDefaults() {
        dataStore.edit { preferences ->
            preferences[webviewUrlKey] = BuildConfig.WEBVIEW_URL
            preferences[adminPasswordHashKey] = hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
            preferences.remove(adminPasswordLegacyKey)
        }
    }

    // --- Photobox ---

    val photoboxOverlayEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[photoboxOverlayEnabledKey] ?: false
    }

    suspend fun setPhotoboxOverlayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[photoboxOverlayEnabledKey] = enabled
        }
    }

    // Raw enum name (e.g. "LEFT"/"CENTER"/"RIGHT") — parsing and the default live with
    // PhotoboxOverlaySettings so this repository doesn't need to depend on that enum.
    val photoboxOverlayPosition: Flow<String> = dataStore.data.map { preferences ->
        preferences[photoboxOverlayPositionKey] ?: ""
    }

    suspend fun setPhotoboxOverlayPosition(position: String) {
        dataStore.edit { preferences ->
            preferences[photoboxOverlayPositionKey] = position
        }
    }

    // The Drive folder photos get uploaded to. Swappable so the destination can change without
    // a code change — just paste a new folder share-link in the Photobox settings.
    val driveFolderLink: Flow<String> = dataStore.data.map { preferences ->
        preferences[driveFolderLinkKey] ?: DEFAULT_DRIVE_FOLDER_LINK
    }

    suspend fun setDriveFolderLink(link: String) {
        dataStore.edit { preferences ->
            preferences[driveFolderLinkKey] = link
        }
    }

    // URL of the Apps Script web app that accepts the upload and writes it into the Drive folder.
    val driveUploadUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[driveUploadUrlKey] ?: DEFAULT_DRIVE_UPLOAD_URL
    }

    suspend fun setDriveUploadUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[driveUploadUrlKey] = url
        }
    }

    fun isValidAdminPassword(plainPassword: String, currentHash: String): Boolean {
        return hashPassword(plainPassword) == currentHash
    }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }
}
