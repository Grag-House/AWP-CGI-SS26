package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import hka.awp.cgi.temi.app.feature.mqtt.MqttTrafficEvent
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.round

class AdminPanelViewModel(
    private val appConfigRepository: AppConfigRepository,
    private val mqttManager: MqttManager
) : ViewModel() {

    private val _events = MutableSharedFlow<AdminPanelEvent>()
    val events = _events.asSharedFlow()
    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _passwordError = MutableStateFlow(false)
    val passwordError = _passwordError.asStateFlow()

    val uiState: StateFlow<AdminPanelState> = combine(
        appConfigRepository.currentUrl,
        appConfigRepository.latitude,
        appConfigRepository.longitude,
        mqttManager.trafficEvents
    ) { url, lat, lon, trafficEvents ->
        AdminPanelState(
            webserverUrl = url,
            latitude = lat,
            longitude = lon,
            coordinates = "Länge: $lon Breite: $lat",
            mqttReportTopics = MqttManager.reportTopics,
            mqttTrafficEvents = trafficEvents.filter { it.topic in MqttManager.reportTopics }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT),
        initialValue = AdminPanelState()
    )

    fun checkPassword(input: String) {
        viewModelScope.launch {
            val currentHash = appConfigRepository.adminPasswordHash.first()

            val isValid = appConfigRepository.isValidAdminPassword(input, currentHash)

            if (isValid) {
                _passwordError.value = false
                _isAuthorized.value = true
            } else {
                _passwordError.value = true
            }
        }
    }

    fun clearPasswordError() {
        _passwordError.value = false
    }

    fun resetAuthorization() {
        _isAuthorized.value = false
        _passwordError.value = false
    }

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
        viewModelScope.launch {
            _events.emit(AdminPanelEvent.OpenMqttReports)
        }
    }

    fun onClearMqttReports() {
        mqttManager.clearTrafficEvents()
    }

    fun onChangePassword(newPassword: String) {
        viewModelScope.launch {
            appConfigRepository.updateAdminPassword(newPassword)
            _events.emit(AdminPanelEvent.PasswordChanged)
        }
    }

    fun onRestartAppRequested() {
        viewModelScope.launch {
            _events.emit(AdminPanelEvent.RestartAppTriggered)
        }
    }

    fun onSavePatrolSettings(mode: Any, minMin: Int, maxMin: Int, hours: Set<Int>) {
        // TODO
    }
    companion object {
        private const val STATE_TIMEOUT = 5000L
    }
}

sealed interface AdminPanelEvent {
    data object OpenMqttReports : AdminPanelEvent
    data object PasswordChanged : AdminPanelEvent
    data object RestartAppTriggered : AdminPanelEvent
}

data class AdminPanelState(
    val webserverUrl: String = BuildConfig.WEBVIEW_URL,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val mqttReportTopics: Set<String> = emptySet(),
    val mqttTrafficEvents: List<MqttTrafficEvent> = emptyList(),
    val coordinates: String = "",
    val patrolModeText: String = "Deaktiviert",
    @Suppress("MagicNumber")
    var longitude: Double = 8.3573,
    @Suppress("MagicNumber")
    var latitude: Double = 49.0138
)
