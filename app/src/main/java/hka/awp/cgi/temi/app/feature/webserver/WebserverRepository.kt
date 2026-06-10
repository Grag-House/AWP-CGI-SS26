package hka.awp.cgi.temi.app.feature.webserver

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WebserverRepository(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("webview_url")

    val currentURL: Flow<String> = dataStore.data.map { preferences ->
        preferences[key] ?: BuildConfig.WEBVIEW_URL
    }

    suspend fun updateURL(newDomain: String) {
        dataStore.edit { preferences ->
            preferences[key] = newDomain
        }
    }
}
