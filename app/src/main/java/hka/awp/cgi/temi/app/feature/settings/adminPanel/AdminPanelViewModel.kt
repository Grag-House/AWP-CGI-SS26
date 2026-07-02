package hka.awp.cgi.temi.app.feature.settings.adminPanel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.data.repository.GeneralConfigRepository
import hka.awp.cgi.temi.app.data.repository.PatrolConfigRepository
import hka.awp.cgi.temi.app.data.repository.SecurityConfigRepository
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotFilterManager
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotRepository
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import hka.awp.cgi.temi.app.feature.mqtt.MqttTrafficEvent
import hka.awp.cgi.temi.app.feature.patrol.PatrolCameraStreamManager
import hka.awp.cgi.temi.app.feature.patrol.PatrolManager
import hka.awp.cgi.temi.app.feature.patrol.PatrolMode
import hka.awp.cgi.temi.app.feature.patrol.PatrolSettings
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.AdminPanelPatrolSettingsDialog
import hka.awp.cgi.temi.app.feature.voiceRecognition.SpeakerVector
import hka.awp.cgi.temi.app.feature.voiceRecognition.TemiVoiceRecognitionViewModel
import hka.awp.cgi.temi.app.feature.voiceRecognition.VoiceProfileRepository
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

/**
 * ViewModel for the Admin Panel, managing security, configuration, voice profiles,
 * and robot patrol settings.
 *
 * @property generalConfigRepository Handles URL, coordinates, and general settings.
 * @property patrolConfigRepository Handles patrol timing, modes, and routes.
 * @property securityConfigRepository Handles password verification and updates.
 * @property mqttManager Manages MQTT communication and event logging.
 * @property voiceProfileRepository Manages speaker voice profiles.
 * @property voiceRecognitionViewModel Manages voice recognition state and enrollment.
 * @property robot The [Robot] instance for hardware interaction.
 * @property patrolManager Logic for executing and scheduling patrols.
 * @property patrolCameraStreamManager Manages the video stream during patrol.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class AdminPanelViewModel(
    private val generalConfigRepository: GeneralConfigRepository,
    private val patrolConfigRepository: PatrolConfigRepository,
    private val securityConfigRepository: SecurityConfigRepository,
    private val mqttManager: MqttManager,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val voiceRecognitionViewModel: TemiVoiceRecognitionViewModel,
    private val robot: Robot?,
    hidingSpotRepository: HidingSpotRepository,
    private val patrolManager: PatrolManager,
    private val patrolCameraStreamManager: PatrolCameraStreamManager
) : ViewModel() {

    /** Manager for filtering locations during hide and seek. */
    val filterManager = HidingSpotFilterManager(robot, hidingSpotRepository)

    private val _events = MutableSharedFlow<AdminPanelEvent>()

    /** Stream of events (e.g., password changes, restarts) for the UI to handle. */
    val events = _events.asSharedFlow()

    private val _isAuthorized = MutableStateFlow(false)

    /** Whether the user is currently authorized to access protected Admin Panel settings. */
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _passwordError = MutableStateFlow(false)

    /** Whether a password check failed. */
    val passwordError = _passwordError.asStateFlow()

    private val patrolRouteSettings = MutableStateFlow(PatrolRouteSettingsState())
    private val patrolLocationPrefix = "patrol_"

    /** Current video frame bitmap from the patrol camera. */
    val videoFrame: StateFlow<Bitmap?> = patrolCameraStreamManager.videoFrame

    private data class PatrolRouteSettingsState(
        val savedLocations: List<String> = emptyList()
    )

    private data class AdminPanelFlows(
        val url: String,
        val latitude: Double,
        val longitude: Double,
        val speakerEnabled: Boolean,
        val speakerThreshold: Double,
    )

    /** Loads locations from the robot that match the patrol prefix. */
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

    private val baseConfigFlow = combine(
        generalConfigRepository.currentUrl,
        generalConfigRepository.latitude,
        generalConfigRepository.longitude,
        generalConfigRepository.isSpeakerVerificationEnabled,
    ) { url, lat, lon, speakerEnabled ->
        AdminPanelFlows(
            url = url,
            latitude = lat,
            longitude = lon,
            speakerEnabled = speakerEnabled,
            speakerThreshold = GeneralConfigRepository.DEFAULT_SPEAKER_VERIFICATION_THRESHOLD,
        )
    }

    private val configWithThresholdFlow = combine(
        baseConfigFlow,
        generalConfigRepository.speakerVerificationThreshold,
    ) { config, threshold ->
        config.copy(speakerThreshold = threshold)
    }

    /**
     * The unified UI state for the Admin Panel, combining settings, MQTT traffic,
     * voice profiles, and patrol status.
     */
    @Suppress("UNCHECKED_CAST", "MagicNumber")
    val uiState: StateFlow<AdminPanelState> = combine(
        configWithThresholdFlow,
        mqttManager.trafficEvents,
        voiceProfileRepository.voiceProfiles,
        voiceRecognitionViewModel.isEnrollmentActive,
        patrolConfigRepository.isPatrolEnabled,
        patrolConfigRepository.patrolMode,
        patrolConfigRepository.minPatrolMinutes,
        patrolConfigRepository.maxPatrolMinutes,
        patrolConfigRepository.selectedPatrolHours,
        patrolRouteSettings,
        patrolConfigRepository.patrolRoute,
        videoFrame,
        patrolManager.isRunning,
    ) { args ->
        val config = args[0] as AdminPanelFlows
        val trafficEvents = args[1] as List<MqttTrafficEvent>
        val voiceProfiles = args[2] as Map<String, SpeakerVector>
        val isEnrollmentActive = args[3] as Boolean
        val isEnabled = args[4] as Boolean
        val mode = args[5] as AdminPanelPatrolSettingsDialog
        val min = args[6] as Int
        val max = args[7] as Int
        val hours = args[8] as Set<Int>
        val routeSettings = args[9] as PatrolRouteSettingsState
        val patrolRoute = args[10] as List<String>
        val currentFrame = args[11] as Bitmap?
        val isPatrolStreaming = args[12] as Boolean

        AdminPanelState(
            webserverUrl = config.url,
            latitude = config.latitude,
            longitude = config.longitude,
            coordinates = "Länge: ${config.longitude} Breite: ${config.latitude}",
            isSpeakerVerificationEnabled = config.speakerEnabled,
            speakerVerificationThreshold = config.speakerThreshold,
            mqttReportTopics = MqttManager.reportTopics,
            mqttTrafficEvents = trafficEvents.filter { it.topic in MqttManager.reportTopics },
            voiceProfileCount = voiceProfiles.size,
            voiceProfiles = voiceProfiles,
            isEnrollmentActive = isEnrollmentActive,
            isPatrolEnabled = isEnabled,
            patrolMode = mode,
            minMinutes = min,
            maxMinutes = max,
            selectedHours = hours,
            savedLocations = routeSettings.savedLocations,
            patrolRoute = patrolRoute,
            videoFrame = currentFrame,
            isPatrolStreaming = isPatrolStreaming,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT),
        initialValue = AdminPanelState()
    )

    /**
     * Handles UI actions dispatched from the Admin Panel.
     *
     * @param action The action to perform.
     */
    fun onAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.CheckWebserverPassword,
            is AdminPanelAction.CheckAdminPassword,
            AdminPanelAction.ClearPasswordError,
            AdminPanelAction.ResetAuthorization,
            is AdminPanelAction.ChangeAdminPassword,
            is AdminPanelAction.ChangeWebserverPassword -> handleSecurityAction(action)

            is AdminPanelAction.EditCoordinates,
            is AdminPanelAction.EditWebserverUrl,
            AdminPanelAction.ResetCoordinates,
            AdminPanelAction.OpenMqttReports,
            AdminPanelAction.ClearMqttReports,
            AdminPanelAction.RequestRestart,
            is AdminPanelAction.RequestCloseApp -> handleConfigAction(action)

            is AdminPanelAction.ToggleSpeakerVerification,
            is AdminPanelAction.EditSpeakerVerificationThreshold,
            AdminPanelAction.ResetVoiceProfiles,
            is AdminPanelAction.ToggleEnrollment,
            is AdminPanelAction.DeleteVoiceProfile -> handleVoiceAction(action)

            is AdminPanelAction.SavePatrolSettings,
            is AdminPanelAction.SavePatrolRoute,
            AdminPanelAction.TriggerImmediatePatrol,
            AdminPanelAction.ExitPatrol -> handlePatrolAction(action)
        }
    }

    private fun handleSecurityAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.CheckWebserverPassword -> checkWebserverPassword(action.password)
            is AdminPanelAction.CheckAdminPassword -> checkAdminPassword(action.password)
            AdminPanelAction.ClearPasswordError -> _passwordError.value = false
            AdminPanelAction.ResetAuthorization -> {
                _isAuthorized.value = false
                _passwordError.value = false
            }
            is AdminPanelAction.ChangeAdminPassword -> viewModelScope.launch {
                securityConfigRepository.updateAdminPanelPassword(action.password)
                _events.emit(AdminPanelEvent.PasswordChanged)
            }
            is AdminPanelAction.ChangeWebserverPassword -> viewModelScope.launch {
                securityConfigRepository.updateWebserverPassword(action.password)
                _events.emit(AdminPanelEvent.WebserverPasswordChanged)
            }
            else -> Unit
        }
    }

    private fun checkWebserverPassword(input: String) {
        viewModelScope.launch {
            val currentHash = securityConfigRepository.webserverPasswordHash.first()
            val isValid = securityConfigRepository.isValidPassword(input, currentHash)

            _passwordError.value = !isValid
            if (isValid) {
                _isAuthorized.value = true
            }
        }
    }

    private fun checkAdminPassword(input: String) {
        viewModelScope.launch {
            val currentHash = securityConfigRepository.adminPanelPasswordHash.first()
            val isValid = securityConfigRepository.isValidPassword(input, currentHash)

            _passwordError.value = !isValid
            if (isValid) {
                _isAuthorized.value = true
            }
        }
    }

    private fun handleConfigAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.EditCoordinates -> updateCoordinates(action.latitude, action.longitude)
            is AdminPanelAction.EditWebserverUrl -> viewModelScope.launch {
                generalConfigRepository.updateUrl(action.url)
            }
            AdminPanelAction.ResetCoordinates -> viewModelScope.launch {
                generalConfigRepository.updateCoordinates(
                    GeneralConfigRepository.DEFAULT_LATITUDE,
                    GeneralConfigRepository.DEFAULT_LONGITUDE
                )
            }
            AdminPanelAction.OpenMqttReports -> viewModelScope.launch {
                _events.emit(AdminPanelEvent.OpenMqttReports)
            }
            AdminPanelAction.ClearMqttReports -> mqttManager.clearTrafficEvents()
            AdminPanelAction.RequestRestart -> viewModelScope.launch {
                _events.emit(AdminPanelEvent.RestartAppTriggered)
            }
            AdminPanelAction.RequestCloseApp -> viewModelScope.launch {
                _events.emit(AdminPanelEvent.CloseAppTriggered)
            }
            else -> Unit
        }
    }

    private fun handleVoiceAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.ToggleSpeakerVerification -> viewModelScope.launch {
                generalConfigRepository.updateSpeakerVerification(enabled = action.enabled)
            }
            is AdminPanelAction.EditSpeakerVerificationThreshold -> viewModelScope.launch {
                generalConfigRepository.updateSpeakerVerification(threshold = action.threshold)
            }
            AdminPanelAction.ResetVoiceProfiles -> viewModelScope.launch {
                voiceProfileRepository.clearAllProfiles()
            }
            is AdminPanelAction.ToggleEnrollment -> {
                voiceRecognitionViewModel.toggleEnrollment(action.active, action.name)
            }
            is AdminPanelAction.DeleteVoiceProfile -> viewModelScope.launch {
                voiceProfileRepository.deleteVoiceProfile(action.name)
            }
            else -> Unit
        }
    }

    private fun handlePatrolAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.SavePatrolSettings -> onSavePatrolSettings(
                isEnabled = action.isEnabled,
                mode = action.mode,
                minMin = action.minMinutes,
                maxMin = action.maxMinutes,
                hours = action.hours,
            )
            is AdminPanelAction.SavePatrolRoute -> onSavePatrolRoute(action.route)
            AdminPanelAction.TriggerImmediatePatrol -> onTriggerImmediatePatrol()
            AdminPanelAction.ExitPatrol -> onExitPatrol()
            else -> Unit
        }
    }

    private fun updateCoordinates(latitude: Double, longitude: Double) {
        val roundedLat = round(latitude * COORDINATE_PRECISION) / COORDINATE_PRECISION
        val roundedLon = round(longitude * COORDINATE_PRECISION) / COORDINATE_PRECISION

        viewModelScope.launch {
            generalConfigRepository.updateCoordinates(roundedLat, roundedLon)
        }
    }

    /** Saves patrol settings and updates the active patrol schedule. */
    fun onSavePatrolSettings(
        isEnabled: Boolean,
        mode: AdminPanelPatrolSettingsDialog,
        minMin: Int,
        maxMin: Int,
        hours: Set<Int>
    ) {
        viewModelScope.launch {
            patrolConfigRepository.updatePatrolSettings(isEnabled, mode, minMin, maxMin, hours)

            patrolManager.updateSchedule(
                PatrolSettings(
                    isEnabled = isEnabled,
                    mode = when (mode) {
                        AdminPanelPatrolSettingsDialog.RANDOM -> PatrolMode.RANDOM
                        AdminPanelPatrolSettingsDialog.FIXED -> PatrolMode.FIXED
                    },
                    minMinutes = minMin,
                    maxMinutes = maxMin,
                    hours = hours,
                    route = uiState.value.patrolRoute,
                )
            )
        }
    }

    /** Triggers an immediate patrol run. */
    fun onTriggerImmediatePatrol() {
        val success = patrolManager.startImmediatePatrol(uiState.value.patrolRoute)
        if (!success) {
            viewModelScope.launch { _events.emit(AdminPanelEvent.NoRouteSelected) }
        }
    }

    /** Saves the patrol route and updates the active patrol schedule. */
    fun onSavePatrolRoute(route: List<String>) {
        viewModelScope.launch {
            patrolConfigRepository.updatePatrolRoute(route)

            val state = uiState.value

            patrolManager.updateSchedule(
                PatrolSettings(
                    isEnabled = state.isPatrolEnabled,
                    mode = when (state.patrolMode) {
                        AdminPanelPatrolSettingsDialog.RANDOM -> PatrolMode.RANDOM
                        AdminPanelPatrolSettingsDialog.FIXED -> PatrolMode.FIXED
                    },
                    minMinutes = state.minMinutes,
                    maxMinutes = state.maxMinutes,
                    hours = state.selectedHours,
                    route = route,
                )
            )
        }
    }

    /** Stops any currently running patrol. */
    fun onExitPatrol() {
        patrolManager.stopPatrol()
    }

    companion object {
        private const val STATE_TIMEOUT = 5000L
        private const val COORDINATE_PRECISION = 10000.0
    }
}

/**
 * Interface representing actions that can be performed in the Admin Panel.
 */
sealed interface AdminPanelAction {
    /** Clears the current password error state. */
    data object ClearPasswordError : AdminPanelAction

    /** Resets the authorization status, requiring re-login. */
    data object ResetAuthorization : AdminPanelAction

    /** Updates application coordinates. */
    data class EditCoordinates(val latitude: Double, val longitude: Double) : AdminPanelAction

    /** Updates the main WebView URL. */
    data class EditWebserverUrl(val url: String) : AdminPanelAction

    /** Resets coordinates to default values. */
    data object ResetCoordinates : AdminPanelAction

    /** Triggers navigation to the MQTT report screen. */
    data object OpenMqttReports : AdminPanelAction

    /** Clears the MQTT traffic log. */
    data object ClearMqttReports : AdminPanelAction

    /** Requests an application restart. */
    data object RequestRestart : AdminPanelAction

    /** Checks the webserver-specific password. */
    data class CheckWebserverPassword(val password: String) : AdminPanelAction

    /** Checks the admin-specific password. */
    data class CheckAdminPassword(val password: String) : AdminPanelAction

    /** Updates the admin password. */
    data class ChangeAdminPassword(val password: String) : AdminPanelAction

    /** Updates the webserver password. */
    data class ChangeWebserverPassword(val password: String) : AdminPanelAction

    /** Toggles speaker verification. */
    data class ToggleSpeakerVerification(val enabled: Boolean) : AdminPanelAction

    /** Updates the sensitivity threshold for speaker verification. */
    data class EditSpeakerVerificationThreshold(val threshold: Double) : AdminPanelAction

    /** Deletes all enrolled voice profiles. */
    data object ResetVoiceProfiles : AdminPanelAction

    /** Toggles enrollment mode for voice profiles. */
    data class ToggleEnrollment(val active: Boolean, val name: String? = null) : AdminPanelAction

    /** Deletes a specific voice profile by name. */
    data class DeleteVoiceProfile(val name: String) : AdminPanelAction

    /** Requests to close the application. */
    data object RequestCloseApp : AdminPanelAction

    /** Saves patrol timing and mode settings. */
    data class SavePatrolSettings(
        val isEnabled: Boolean,
        val mode: AdminPanelPatrolSettingsDialog,
        val minMinutes: Int,
        val maxMinutes: Int,
        val hours: Set<Int>,
    ) : AdminPanelAction

    /** Saves the defined patrol route. */
    data class SavePatrolRoute(val route: List<String>) : AdminPanelAction

    /** Starts a patrol immediately. */
    data object TriggerImmediatePatrol : AdminPanelAction

    /** Stops the active patrol. */
    data object ExitPatrol : AdminPanelAction
}

/**
 * Events emitted by the Admin Panel to the UI.
 */
sealed interface AdminPanelEvent {
    /** Request to open MQTT reports. */
    data object OpenMqttReports : AdminPanelEvent

    /** Feedback that the webserver password was changed. */
    data object WebserverPasswordChanged : AdminPanelEvent

    /** Feedback that the admin password was changed. */
    data object PasswordChanged : AdminPanelEvent

    /** Feedback that an app restart was triggered. */
    data object RestartAppTriggered : AdminPanelEvent

    /** Feedback that app closure was triggered. */
    data object CloseAppTriggered : AdminPanelEvent

    /** Feedback that a patrol cannot start because no route is selected. */
    data object NoRouteSelected : AdminPanelEvent
}

/**
 * State representing the UI of the Admin Panel.
 */
data class AdminPanelState(
    val webserverUrl: String = BuildConfig.WEBVIEW_URL,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val mqttReportTopics: Set<String> = emptySet(),
    val mqttTrafficEvents: List<MqttTrafficEvent> = emptyList(),
    val coordinates: String = "",
    val isSpeakerVerificationEnabled: Boolean = false,
    val speakerVerificationThreshold: Double = GeneralConfigRepository.DEFAULT_SPEAKER_VERIFICATION_THRESHOLD,
    val voiceProfileCount: Int = 0,
    val voiceProfiles: Map<String, SpeakerVector> = emptyMap(),
    val isEnrollmentActive: Boolean = false,

    val savedLocations: List<String> = emptyList(),
    val patrolRoute: List<String> = emptyList(),
    val isPatrolEnabled: Boolean = false,
    val patrolMode: AdminPanelPatrolSettingsDialog = AdminPanelPatrolSettingsDialog.RANDOM,
    val videoFrame: Bitmap? = null,
    val isPatrolStreaming: Boolean = false,
    val minMinutes: Int = 40,
    val maxMinutes: Int = 60,
    val selectedHours: Set<Int> = emptySet(),

    val longitude: Double = 8.3573,
    val latitude: Double = 49.0138,
)
