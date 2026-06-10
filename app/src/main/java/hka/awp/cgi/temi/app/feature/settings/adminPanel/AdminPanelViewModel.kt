package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.webserver.AppConfigRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.round

class AdminPanelViewModel(private val appConfigRepository: AppConfigRepository) : ViewModel() {

    private val _events = MutableSharedFlow<AdminPanelEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<AdminPanelState> = combine(
        appConfigRepository.currentUrl,
        appConfigRepository.latitude,
        appConfigRepository.longitude
    ) { url, lat, lon ->
        AdminPanelState(
            webserverUrl = url,
            latitude = lat,
            longitude = lon,
            coordinates = "Länge: $lon Breite: $lat"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT),
        initialValue = AdminPanelState()
    )

    // Standard range for coordinates is -180 -> 180 and -90 -> 90
    @Suppress("MagicNumber")
    fun onEditCoordinates(latitude: Double, longitude: Double) {
        val roundedLat = round(latitude * 10000.0) / 10000.0
        val roundedLon = round(longitude * 10000.0) / 10000.0

        viewModelScope.launch {
            appConfigRepository.updateCoordinates(roundedLat, roundedLon)
        }
    }

    fun onEditWebserverUrl(newUrl: String) {
        viewModelScope.launch {
            appConfigRepository.updateUrl(newUrl)
        }
    }

    @Suppress("MagicNumber")
    fun onResetCoordinates() {
        viewModelScope.launch {
            // Karlsruhe
            appConfigRepository.updateCoordinates(49.0138, 8.3573)
        }
    }

    fun onOpenMqttReports() {
        // Implementation pending
    }

    fun onChangePassword(newPassword: String) {
        viewModelScope.launch {
            appConfigRepository.updateAdminPassword(newPassword)
            _events.emit(AdminPanelEvent.PasswordChanged)
        }
    }
    companion object {
        private const val STATE_TIMEOUT = 5000L
    }
}

sealed interface AdminPanelEvent {
    data object PasswordChanged : AdminPanelEvent
}

data class AdminPanelState(
    val webserverUrl: String = BuildConfig.WEBVIEW_URL,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val coordinates: String = "",
    @Suppress("MagicNumber")
    var longitude: Double = 8.3573,
    @Suppress("MagicNumber")
    var latitude: Double = 49.0138
)
