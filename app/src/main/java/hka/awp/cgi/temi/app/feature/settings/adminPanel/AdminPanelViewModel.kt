package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.lifecycle.ViewModel
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.round

class AdminPanelViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPanelState())
    val uiState: StateFlow<AdminPanelState> = _uiState.asStateFlow()

    fun onEditCoordinates(latitude: Double, longitude: Double) {
        if (longitude > 0 && latitude > 0) {
            WeatherRepository.LONGITUDE = round(longitude * 10000) / 10000
            WeatherRepository.LATITUDE = round(latitude * 10000) / 10000
        } else {
            @Suppress("MagicNumber")
            WeatherRepository.LONGITUDE = 8.3573
            @Suppress("MagicNumber")
            WeatherRepository.LATITUDE = 49.0138
        }
        updateCoordinateState()
    }

    fun onResetCoordinates() {
        @Suppress("MagicNumber")
        WeatherRepository.LONGITUDE = 8.3573
        @Suppress("MagicNumber")
        WeatherRepository.LATITUDE = 49.0138
        updateCoordinateState()
    }

    private fun updateCoordinateState() {
        _uiState.value = _uiState.value.copy(
            longitude = WeatherRepository.LONGITUDE,
            latitude = WeatherRepository.LATITUDE,
            coordinates = "Länge: ${WeatherRepository.LONGITUDE} Breite: ${WeatherRepository.LATITUDE}"
        )
    }

    fun onOpenMqttReports() {
        TODO() // Absprechen was eigentlich gewollt ist
    }

    fun onChangePassword(oldPassword: String, newPassword: String) {
        TODO() // Anbindung an Webserver Passwort Implementation
    }
}

data class AdminPanelState(

    val webserverUrl: String = BuildConfig.WEBVIEW_URL,

    val appVersion: String = BuildConfig.VERSION_NAME,

    val coordinates: String = "Länge: ${WeatherRepository.LONGITUDE} Breite: ${WeatherRepository.LATITUDE}",

    @Suppress("MagicNumber")
    var longitude: Double = 8.3573,

    @Suppress("MagicNumber")
    var latitude: Double = 49.0138
)
