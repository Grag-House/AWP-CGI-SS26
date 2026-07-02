package hka.awp.cgi.temi.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository responsible for managing photobox-related configurations.
 * Handles UI overlay visibility, banners, and Google Drive upload links.
 *
 * @property dataStore The [DataStore] instance used for persisting photobox settings.
 */
class PhotoboxConfigRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val photoboxOverlayEnabledKey = booleanPreferencesKey("photobox_overlay_enabled")
    private val photoboxOverlayPositionKey = stringPreferencesKey("photobox_overlay_position")
    private val photoboxBannerEnabledKey = booleanPreferencesKey("photobox_banner_enabled")
    private val photoboxBannerKey = stringPreferencesKey("photobox_banner")
    private val driveFolderLinkKey = stringPreferencesKey("photobox_drive_folder_link")
    private val driveUploadUrlKey = stringPreferencesKey("photobox_drive_upload_url")

    /**
     * Flow indicating whether the photobox overlay is enabled.
     */
    val photoboxOverlayEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[photoboxOverlayEnabledKey] ?: false
    }

    /**
     * Flow of the photobox overlay position string.
     */
    val photoboxOverlayPosition: Flow<String> = dataStore.data.map { preferences ->
        preferences[photoboxOverlayPositionKey] ?: ""
    }

    /**
     * Updates the photobox overlay settings.
     *
     * @param enabled Whether the overlay should be visible.
     * @param position The UI position of the overlay.
     */
    suspend fun setPhotoboxOverlay(enabled: Boolean, position: String) {
        dataStore.edit { preferences ->
            preferences[photoboxOverlayEnabledKey] = enabled
            preferences[photoboxOverlayPositionKey] = position
        }
    }

    /**
     * Flow indicating whether the photobox banner is enabled.
     */
    val photoboxBannerEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[photoboxBannerEnabledKey] ?: false
    }

    /**
     * Flow of the photobox banner text or resource identifier.
     */
    val photoboxBanner: Flow<String> = dataStore.data.map { preferences ->
        preferences[photoboxBannerKey] ?: ""
    }

    /**
     * Updates the photobox banner settings.
     *
     * @param enabled Whether the banner should be visible.
     * @param banner The content or ID for the banner.
     */
    suspend fun setPhotoboxBanner(enabled: Boolean, banner: String) {
        dataStore.edit { preferences ->
            preferences[photoboxBannerEnabledKey] = enabled
            preferences[photoboxBannerKey] = banner
        }
    }

    /**
     * Flow of the Google Drive folder link for photo access.
     */
    val driveFolderLink: Flow<String> = dataStore.data.map { preferences ->
        preferences[driveFolderLinkKey] ?: BuildConfig.DEFAULT_DRIVE_FOLDER_LINK
    }

    /**
     * Flow of the Google Drive upload URL for photos.
     */
    val driveUploadUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[driveUploadUrlKey] ?: BuildConfig.DEFAULT_DRIVE_UPLOAD_URL
    }

    /**
     * Updates Google Drive-related settings.
     *
     * @param folderLink Optional new folder link.
     * @param uploadUrl Optional new upload URL.
     */
    suspend fun setDriveSettings(folderLink: String? = null, uploadUrl: String? = null) {
        dataStore.edit { preferences ->
            folderLink?.let { preferences[driveFolderLinkKey] = it }
            uploadUrl?.let { preferences[driveUploadUrlKey] = it }
        }
    }
}
