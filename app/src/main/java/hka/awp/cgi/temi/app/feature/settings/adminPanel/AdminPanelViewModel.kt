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


    fun onEditCoordinates(coordinates: AdminPanelState) {
        if(coordinates.longitude > 0 && coordinates.latitude > 0){
            WeatherRepository.LONGITUDE = round(coordinates.longitude * 10000) / 10000
            WeatherRepository.LATITUDE = round(coordinates.latitude * 10000) / 10000
        }
        else {
            WeatherRepository.LONGITUDE = 8.3573
                WeatherRepository.LATITUDE = 49.0138
        }
    }

    fun onOpenMqttReports() {
        TODO()
    }

    fun onChangePassword() {
        TODO()
    }
}

data class AdminPanelState(

    val webserverUrl: String = BuildConfig.WEBVIEW_URL,

    val appVersion: String = BuildConfig.VERSION_NAME,

    val coordinates: String = "Länge: ${WeatherRepository.LONGITUDE} Breite: ${WeatherRepository.LATITUDE}",

    var longitude: Double = 0.0,

    var latitude: Double = 0.0
                          )
