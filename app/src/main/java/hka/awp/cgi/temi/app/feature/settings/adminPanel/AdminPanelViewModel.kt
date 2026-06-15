package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import hka.awp.cgi.temi.app.feature.mqtt.MqttTrafficEvent
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.DialogPatrolMode
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
import timber.log.Timber
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
        appConfigRepository.currentUrl,           // 0
        appConfigRepository.latitude,             // 1
        appConfigRepository.longitude,            // 2
        mqttManager.trafficEvents,                // 3
        appConfigRepository.isPatrolEnabled,      // 4
        appConfigRepository.patrolMode,           // 5
        appConfigRepository.minPatrolMinutes,     // 6
        appConfigRepository.maxPatrolMinutes,     // 7
        appConfigRepository.selectedPatrolHours   // 8
                                                     ) { args ->
        val url = args[0] as String
        val lat = args[1] as Double
        val lon = args[2] as Double
        val trafficEvents = args[3] as List<MqttTrafficEvent>
        val isEnabled = args[4] as Boolean
        val mode = args[5] as DialogPatrolMode
        val min = args[6] as Int
        val max = args[7] as Int
        val hours = args[8] as Set<Int>

        AdminPanelState(
            webserverUrl = url,
            latitude = lat,
            longitude = lon,
            coordinates = "Länge: $lon Breite: $lat",
            mqttReportTopics = MqttManager.reportTopics,
            mqttTrafficEvents = trafficEvents.filter { it.topic in MqttManager.reportTopics },
            isPatrolEnabled = isEnabled,
            patrolMode = mode,
            minMinutes = min,
            maxMinutes = max,
            selectedHours = hours
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

    fun onSavePatrolSettings(
        isEnabled: Boolean,
        mode: DialogPatrolMode,
        minMin: Int,
        maxMin: Int,
        hours: Set<Int>
    ) {
        viewModelScope.launch {
            appConfigRepository.updatePatrolSettings(isEnabled, mode, minMin, maxMin, hours)
        }
    }

    fun onTriggerImmediatePatrol() {

        // TODO Anbindung der Backend Logik

        //patrolMode = FIXED oder RANDOM
        // isPatrolEnabled = true oder false
        //selected Hours gibt int von 1 bis 24 für die Stunden

        val meinStundenPlan = uiState.value.selectedHours

        Timber.d("Aktueller Stundenplan: $meinStundenPlan")
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
    val isPatrolEnabled: Boolean = false,
    val patrolMode: DialogPatrolMode = DialogPatrolMode.RANDOM,
    val minMinutes: Int = 40,
    val maxMinutes: Int = 60,
    val selectedHours: Set<Int> = emptySet(),
    @Suppress("MagicNumber")
    val longitude: Double = 8.3573,
    @Suppress("MagicNumber")
    val latitude: Double = 49.0138
)
