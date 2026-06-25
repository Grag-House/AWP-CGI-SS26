package hka.awp.cgi.temi.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolSettingsDialog
import hka.awp.cgi.temi.app.utils.AppConfigRepository.Companion.COMMA_SEPARATOR
import hka.awp.cgi.temi.app.utils.AppConfigRepository.Companion.DEFAULT_LATITUDE
import hka.awp.cgi.temi.app.utils.AppConfigRepository.Companion.DEFAULT_LONGITUDE
import hka.awp.cgi.temi.app.utils.AppConfigRepository.Companion.DEFAULT_MAX_MINUTES
import hka.awp.cgi.temi.app.utils.AppConfigRepository.Companion.DEFAULT_MIN_MINUTES
import hka.awp.cgi.temi.app.utils.AppConfigRepository.Companion.DEFAULT_PATROL_ENABLED
import hka.awp.cgi.temi.app.utils.AppConfigRepository.Companion.ROUTE_SEPARATOR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.security.MessageDigest
import kotlin.text.split

private const val DEFAULT_DRIVE_FOLDER_LINK = BuildConfig.DEFAULT_DRIVE_FOLDER_LINK
private const val DEFAULT_DRIVE_UPLOAD_URL = BuildConfig.DEFAULT_DRIVE_UPLOAD_URL
// DEFAULT_DRIVE_FOLDER_LINK=https://drive.google.com/drive/folders/1k8g1Yqg8wMwvgY8urcnDer1RYke5voAp?usp=drive_link
// DEFAULT_DRIVE_UPLOAD_URL=
// https://script.google.com/macros/s/AKfycbxBnCIKutMCfloxpVtW50EIyFF45z3OGsY-t4bTGQYqvTQABHB-taPuWtyP2BelWLJ9sQ/exec
// für die .env zum kopieren
// TODO
/**
 * Central repository for application configurations stored in Jetpack DataStore.
 * Handles URL settings, coordinates for weather, and admin credentials.
 */
class AppConfigRepository(private val dataStore: DataStore<Preferences>) {

    // Keys
    private val webviewUrlKey = stringPreferencesKey("webview_url")
    private val latitudeKey = doublePreferencesKey("latitude")
    private val longitudeKey = doublePreferencesKey("longitude")
    private val adminPanelPasswordHashKey = stringPreferencesKey("admin_panel_password_hash")
    private val webserverPasswordHashKey = stringPreferencesKey("webserver_password_hash")
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

    // --- Speaker Verification ---

    val isSpeakerVerificationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[speakerVerificationEnabledKey] ?: false // Default off
    }

    val speakerVerificationThreshold: Flow<Double> = dataStore.data.map { preferences ->
        preferences[speakerVerificationThresholdKey] ?: DEFAULT_SPEAKER_VERIFICATION_THRESHOLD
    }

    suspend fun updateSpeakerVerification(enabled: Boolean? = null, threshold: Double? = null) {
        dataStore.edit { preferences ->
            enabled?.let { preferences[speakerVerificationEnabledKey] = it }
            threshold?.let { preferences[speakerVerificationThresholdKey] = it.coerceIn(0.0, 1.0) }
        }
    }

    // --- Photobox ---

    val photoboxOverlayEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[photoboxOverlayEnabledKey] ?: false
    }

    // Raw enum name (e.g. "LEFT"/"CENTER"/"RIGHT") — parsing and the default live with
    // PhotoboxOverlaySettings so this repository doesn't need to depend on that enum.
    val photoboxOverlayPosition: Flow<String> = dataStore.data.map { preferences ->
        preferences[photoboxOverlayPositionKey] ?: ""
    }

    suspend fun setPhotoboxOverlay(enabled: Boolean, position: String) {
        dataStore.edit { preferences ->
            preferences[photoboxOverlayEnabledKey] = enabled
            preferences[photoboxOverlayPositionKey] = position
        }
    }

    private companion object {
        const val ROUTE_SEPARATOR = "|"
        const val COMMA_SEPARATOR = ","
        const val DEFAULT_LATITUDE = 49.0138
        const val DEFAULT_LONGITUDE = 8.3573
        const val DEFAULT_PATROL_ENABLED = false
        const val DEFAULT_MIN_MINUTES = 40
        const val DEFAULT_MAX_MINUTES = 60
        }

    val photoboxBannerEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[photoboxBannerEnabledKey] ?: false
    }

    // Raw enum name — parsing and the default live with PhotoboxBannerSettings so this
    // repository doesn't need to depend on that enum.
    val photoboxBanner: Flow<String> = dataStore.data.map { preferences ->
        preferences[photoboxBannerKey] ?: ""
    }

    suspend fun setPhotoboxBanner(enabled: Boolean, banner: String) {
        dataStore.edit { preferences ->
            preferences[photoboxBannerEnabledKey] = enabled
            preferences[photoboxBannerKey] = banner
        }
    }

    // The Drive folder photos get uploaded to. Swappable so the destination can change without
    // a code change — just paste a new folder share-link in the Photobox settings.
    val driveFolderLink: Flow<String> = dataStore.data.map { preferences ->
        preferences[driveFolderLinkKey] ?: DEFAULT_DRIVE_FOLDER_LINK
    }

    // URL of the Apps Script web app that accepts the upload and writes it into the Drive folder.
    val driveUploadUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[driveUploadUrlKey] ?: DEFAULT_DRIVE_UPLOAD_URL
    }

    suspend fun setDriveSettings(folderLink: String? = null, uploadUrl: String? = null) {
        dataStore.edit { preferences ->
            folderLink?.let { preferences[driveFolderLinkKey] = it }
            uploadUrl?.let { preferences[driveUploadUrlKey] = it }
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

    // --- General ---

    @Suppress("unused")
    /**
     * Should only be used to clear the ENTIRE dataStore only use this if you know what you are doing :)
     */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        const val DEFAULT_SPEAKER_VERIFICATION_THRESHOLD = 0.82
    }
}

//
//
//val adminPanelPasswordHash: Flow<String> = dataStore.data.map {
//    it[adminPanelPasswordHashKey] ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
//}
//
//val webserverPasswordHash: Flow<String> = dataStore.data.map {
//    it[webserverPasswordHashKey] ?: hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
//}
//
//suspend fun updateAdminPanelPassword(password: String) {
//    dataStore.edit { it[adminPanelPasswordHashKey] = hashPassword(password) }
//}
//
//suspend fun updateWebserverPassword(password: String) {
//    dataStore.edit { it[webserverPasswordHashKey] = hashPassword(password) }
//}
//
//fun isValidPassword(plainPassword: String, currentHash: String): Boolean {
//    return hashPassword(plainPassword) == currentHash
//}
//
//fun hashPassword(password: String): String {
//    val digest = MessageDigest.getInstance("SHA-256")
//    return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
//}
//
//// --- Webview & Koordinaten ---
//
//val currentUrl: Flow<String> = dataStore.data.map { it[webviewUrlKey] ?: BuildConfig.WEBVIEW_URL }
//val latitude: Flow<Double> = dataStore.data.map { it[latitudeKey] ?: DEFAULT_LATITUDE }
//val longitude: Flow<Double> = dataStore.data.map { it[longitudeKey] ?: DEFAULT_LONGITUDE }
//
//suspend fun updateUrl(newUrl: String) {
//    dataStore.edit { it[webviewUrlKey] = newUrl }
//}
//
//suspend fun updateCoordinates(lat: Double, lon: Double) {
//    dataStore.edit {
//        it[latitudeKey] = lat
//        it[longitudeKey] = lon
//    }
//}
//
//// --- Patrol Settings ---
//
//suspend fun updatePatrolSettings(
//    isEnabled: Boolean,
//    mode: PatrolSettingsDialog,
//    minMin: Int,
//    maxMin: Int,
//    hours: Set<Int>
//                                ) {
//    dataStore.edit {
//        it[keyIsPatrolEnabled] = isEnabled
//        it[keyPatrolMode] = mode.name
//        it[keyMinMinutes] = minMin
//        it[keyMaxMinutes] = maxMin
//        it[keySelectedHours] = hours.joinToString(COMMA_SEPARATOR)
//    }
//}
//
//val isPatrolEnabled: Flow<Boolean> = dataStore.data.map {
//    it[keyIsPatrolEnabled] ?: DEFAULT_PATROL_ENABLED
//}
//val patrolMode: Flow<PatrolSettingsDialog> = dataStore.data.map { preferences ->
//    val name = preferences[keyPatrolMode] ?: PatrolSettingsDialog.RANDOM.name
//    try {
//        PatrolSettingsDialog.valueOf(name)
//    } catch (e: IllegalArgumentException) {
//        Timber.e(e, "Ungültiger Patrol-Modus: $name. Setze auf RANDOM.")
//        PatrolSettingsDialog.RANDOM
//    }
//}
//val minPatrolMinutes: Flow<Int> = dataStore.data.map { it[keyMinMinutes] ?: DEFAULT_MIN_MINUTES }
//val maxPatrolMinutes: Flow<Int> = dataStore.data.map { it[keyMaxMinutes] ?: DEFAULT_MAX_MINUTES }
//
//val selectedPatrolHours: Flow<Set<Int>> = dataStore.data.map {
//    it[keySelectedHours]?.split(COMMA_SEPARATOR)?.mapNotNull { s -> s.toIntOrNull() }?.toSet() ?: emptySet()
//}
//
//val patrolRoute: Flow<List<String>> = dataStore.data.map {
//    it[keyPatrolRoute]?.split(ROUTE_SEPARATOR)?.filter { s -> s.isNotBlank() } ?: emptyList()
//}
//
//suspend fun updatePatrolRoute(route: List<String>) {
//    dataStore.edit { it[keyPatrolRoute] = route.joinToString(ROUTE_SEPARATOR) }
//}
//}
