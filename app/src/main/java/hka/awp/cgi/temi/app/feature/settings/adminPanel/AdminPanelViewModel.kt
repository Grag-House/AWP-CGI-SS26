package hka.awp.cgi.temi.app.feature.settings.adminPanel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import hka.awp.cgi.temi.app.feature.mqtt.MqttTrafficEvent
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.DialogPatrolMode
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolCameraStreamManager
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolManager
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolMode
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolSettings
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.round

class AdminPanelViewModel(
    private val appConfigRepository: AppConfigRepository,
    private val mqttManager: MqttManager,
    private val robot: Robot?,
    private val patrolManager: PatrolManager,
    private val patrolCameraStreamManager: PatrolCameraStreamManager
) : ViewModel() {

    private val _events = MutableSharedFlow<AdminPanelEvent>()
    val events = _events.asSharedFlow()
    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _passwordError = MutableStateFlow(false)
    val passwordError = _passwordError.asStateFlow()

    private val patrolRouteSettings = MutableStateFlow(PatrolRouteSettingsState())
    private val patrolLocationPrefix = "patrol_"
    val videoFrame: StateFlow<Bitmap?> = patrolCameraStreamManager.videoFrame

    fun loadPatrolLocations() {
        patrolRouteSettings.update {
            it.copy(
                savedLocations = robot
                    ?.locations
                    ?.filter { location -> location.startsWith(patrolLocationPrefix) }
                    ?: emptyList()
            )
        }
    }

    val uiState: StateFlow<AdminPanelState> = combine(
        appConfigRepository.currentUrl,
        appConfigRepository.latitude,
        appConfigRepository.longitude,
        mqttManager.trafficEvents,
        appConfigRepository.isPatrolEnabled,
        appConfigRepository.patrolMode,
        appConfigRepository.minPatrolMinutes,
        appConfigRepository.maxPatrolMinutes,
        appConfigRepository.selectedPatrolHours,
        patrolRouteSettings,
        appConfigRepository.patrolRoute,
        videoFrame,
        patrolManager.isRunning
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
        val routeSettings = args[9] as PatrolRouteSettingsState
        val patrolRoute = args[10] as List<String>
        val currentFrame = args[11] as Bitmap?
        val isPatrolStreaming = args[12] as Boolean

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
            selectedHours = hours,
            savedLocations = routeSettings.savedLocations,
            patrolRoute = patrolRoute,
            patrolRouteText = if (patrolRoute.isEmpty()) {
                "Keine Route ausgewählt"
            } else {
                patrolRoute.joinToString(" → ")
            },
            patrolModeText = if (!isEnabled) {
                "Deaktiviert"
            } else {
                patrolRoute.joinToString(" → ")
            },
            videoFrame = currentFrame,
            isPatrolStreaming = isPatrolStreaming,
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

    // TODO Funktion des PW resetten möglich machen per viewModel.onResetPassword
    fun onResetPassword(standardPassword: String) {
        viewModelScope.launch {
            appConfigRepository.resetAdminPassword(standardPassword)
            _events.emit(AdminPanelEvent.PasswordChanged)
        }
    }

    fun onRestartAppRequested() {
        viewModelScope.launch {
            _events.emit(AdminPanelEvent.RestartAppTriggered)
        }
    }

    fun requestCloseApp() {
        viewModelScope.launch {
            _events.emit(AdminPanelEvent.CloseAppTriggered)
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

            patrolManager.updateSchedule(
                PatrolSettings(
                    isEnabled = isEnabled,
                    mode = when (mode) {
                        DialogPatrolMode.RANDOM -> PatrolMode.RANDOM
                        DialogPatrolMode.FIXED -> PatrolMode.FIXED
                    },
                    minMinutes = minMin,
                    maxMinutes = maxMin,
                    hours = hours,
                    route = uiState.value.patrolRoute
                )
            )
        }
    }

    fun onTriggerImmediatePatrol(): Boolean {
        val success = patrolManager.startImmediatePatrol(uiState.value.patrolRoute)
        return success
    }

    fun onSavePatrolRoute(route: List<String>) {
        viewModelScope.launch {
            appConfigRepository.updatePatrolRoute(route)

            val state = uiState.value

            patrolManager.updateSchedule(
                PatrolSettings(
                    isEnabled = state.isPatrolEnabled,
                    mode = when (state.patrolMode) {
                        DialogPatrolMode.RANDOM -> PatrolMode.RANDOM
                        DialogPatrolMode.FIXED -> PatrolMode.FIXED
                    },
                    minMinutes = state.minMinutes,
                    maxMinutes = state.maxMinutes,
                    hours = state.selectedHours,
                    route = route
                )
            )
        }
    }

    fun onExitPatrol() {
        patrolManager.stopPatrol()
    }
    companion object {
        private const val STATE_TIMEOUT = 5000L
    }
}

sealed interface AdminPanelEvent {
    data object OpenMqttReports : AdminPanelEvent
    data object PasswordChanged : AdminPanelEvent
    data object RestartAppTriggered : AdminPanelEvent
    data object CloseAppTriggered : AdminPanelEvent
}

data class AdminPanelState(
    val webserverUrl: String = BuildConfig.WEBVIEW_URL,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val mqttReportTopics: Set<String> = emptySet(),
    val mqttTrafficEvents: List<MqttTrafficEvent> = emptyList(),
    val coordinates: String = "",
    val patrolModeText: String = "Deaktiviert",
    val patrolRouteText: String = "Keine Route ausgewählt",
    val savedLocations: List<String> = emptyList(),
    val patrolRoute: List<String> = emptyList(),
    val isPatrolEnabled: Boolean = false,
    val patrolMode: DialogPatrolMode = DialogPatrolMode.RANDOM,
    val videoFrame: Bitmap? = null,
    val isPatrolStreaming: Boolean = false,
    val minMinutes: Int = 40,
    val maxMinutes: Int = 60,
    val selectedHours: Set<Int> = emptySet(),
    @Suppress("MagicNumber")
    val longitude: Double = 8.3573,
    @Suppress("MagicNumber")
    val latitude: Double = 49.0138
)

private data class PatrolRouteSettingsState(
    val savedLocations: List<String> = emptyList()
)
