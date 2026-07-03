package hka.awp.cgi.temi.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.utils.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository responsible for managing security-related configurations,
 * primarily password hashing and storage using DataStore.
 *
 * @property dataStore The [DataStore] instance used for persisting security settings.
 * @property passwordHasher The [PasswordHasher] used for hashing passwords.
 */
class SecurityConfigRepository(
    private val dataStore: DataStore<Preferences>,
    private val passwordHasher: PasswordHasher
) {
    private val adminPanelPasswordHashKey = stringPreferencesKey("admin_panel_password_hash")
    private val adminPasswordHashKey = stringPreferencesKey("admin_password_hash")
    private val adminPasswordLegacyKey = stringPreferencesKey("admin_password")

    /**
     * Flow of the hashed password for the admin panel.
     * Falls back to legacy keys or default password if not set.
     */
    val adminPanelPasswordHash: Flow<String> = dataStore.data.map { preferences ->
        preferences[adminPanelPasswordHashKey]
            ?: preferences[adminPasswordHashKey]
            ?: preferences[adminPasswordLegacyKey]?.let(passwordHasher::hashPassword)
            ?: passwordHasher.hashPassword(BuildConfig.DEFAULT_ADMIN_PASSWORD)
    }

    /**
     * Updates the admin panel password.
     * Hashes the plain text password before storing it and removes legacy keys.
     *
     * @param password The new plain text password.
     */
    suspend fun updateAdminPanelPassword(password: String) {
        dataStore.edit { preferences ->
            preferences[adminPanelPasswordHashKey] = passwordHasher.hashPassword(password)
            preferences.remove(adminPasswordHashKey)
            preferences.remove(adminPasswordLegacyKey)
        }
    }

    /**
     * Checks if a plain text password matches a given hash.
     *
     * @param plainPassword The password to check.
     * @param currentHash The hash to compare against.
     * @return True if they match, false otherwise.
     */
    fun isValidPassword(plainPassword: String, currentHash: String): Boolean {
        return passwordHasher.hashPassword(plainPassword) == currentHash
    }
}
