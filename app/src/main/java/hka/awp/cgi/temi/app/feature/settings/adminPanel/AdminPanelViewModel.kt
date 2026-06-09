package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.lifecycle.ViewModel
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminPanelViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminPanelState())
    val uiState: StateFlow<AdminPanelState> = _uiState.asStateFlow()

    fun onEditCoordinates() {
        TODO()
    }

    fun onOpenCoordinates() {
        TODO()
    }

    fun onOpenMqttReports() {
        TODO()
    }

    fun onChangePassword() {
        TODO()
    }
}

data class AdminPanelState(

    val webserverUrl: String = "",

    val appVersion: String = BuildConfig.VERSION_NAME,

    val coordinates: String = "Latittude: ${WeatherRepository.LATITUDE} Longitude: ${WeatherRepository.LONGITUDE}"
)
