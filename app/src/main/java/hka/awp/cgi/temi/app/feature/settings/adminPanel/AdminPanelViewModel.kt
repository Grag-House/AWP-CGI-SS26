package hka.awp.cgi.temi.app.feature.settings.adminPanel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.BuildConfig
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

/**
 * ViewModel for the Admin Panel, managing security, configuration, voice profiles,
 * and robot patrol settings.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class AdminPanelViewModel(
    private val appConfigRepository: AppConfigRepository,
    private val mqttManager: MqttManager,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val voiceRecognitionViewModel: TemiVoiceRecognitionViewModel,
    private val robot: Robot?,
    hidingSpotRepository: HidingSpotRepository,
    private val patrolManager: PatrolManager,
    private val patrolCameraStreamManager: PatrolCameraStreamManager
) : ViewModel() {

    val filterManager = HidingSpotFilterManager(robot, hidingSpotRepository)

    private val _events = MutableSharedFlow<AdminPanelEvent>()
    val events = _events.asSharedFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _passwordError = MutableStateFlow(false)
    val passwordError = _passwordError.asStateFlow()

    private val patrolRouteSettings = MutableStateFlow(PatrolRouteSettingsState())
    private val patrolLocationPrefix = "patrol_"

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
        val basicAuthEnabled: Boolean,
    )

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
        appConfigRepository.currentUrl,
        appConfigRepository.latitude,
        appConfigRepository.longitude,
        appConfigRepository.isSpeakerVerificationEnabled,
        appConfigRepository.isWebserverVerificationEnabled
                                        ) { url, lat, lon, speakerEnabled, basicAuthEnabled ->
        AdminPanelFlows(
            url = url,
            latitude = lat,
            longitude = lon,
            speakerEnabled = speakerEnabled,
            speakerThreshold = AppConfigRepository.DEFAULT_SPEAKER_VERIFICATION_THRESHOLD,
            basicAuthEnabled = basicAuthEnabled,
        )
    }

    private val configWithThresholdFlow = combine(
        baseConfigFlow,
        appConfigRepository.speakerVerificationThreshold,
    ) { config, threshold ->
        config.copy(speakerThreshold = threshold)
    }

    @Suppress("UNCHECKED_CAST", "MagicNumber")
    val uiState: StateFlow<AdminPanelState> = combine(
        configWithThresholdFlow,
        mqttManager.trafficEvents,
        voiceProfileRepository.voiceProfiles,
        voiceRecognitionViewModel.isEnrollmentActive,
        appConfigRepository.isPatrolEnabled,
        appConfigRepository.patrolMode,
        appConfigRepository.minPatrolMinutes,
        appConfigRepository.maxPatrolMinutes,
        appConfigRepository.selectedPatrolHours,
        patrolRouteSettings,
        appConfigRepository.patrolRoute,
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
            isWebserverVerificationEnabled = config.basicAuthEnabled,
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

    fun onAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.CheckWebserverPassword,
            is AdminPanelAction.CheckAdminPassword,
            AdminPanelAction.ClearPasswordError,
            AdminPanelAction.ResetAuthorization,
            is AdminPanelAction.ChangeAdminPassword,
            is AdminPanelAction.ChangeWebserverPassword,
            is AdminPanelAction.ToggleWebserverVerification -> handleSecurityAction(action)

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
                appConfigRepository.updateAdminPanelPassword(action.password)
                _events.emit(AdminPanelEvent.PasswordChanged)
            }
            is AdminPanelAction.ChangeWebserverPassword -> viewModelScope.launch {
                appConfigRepository.updateWebserverPassword(action.password)
                appConfigRepository.updateWebserverUser(action.user)
                _events.emit(AdminPanelEvent.WebserverPasswordChanged)
            }
            is AdminPanelAction.ToggleWebserverVerification -> viewModelScope.launch {
                appConfigRepository.updateWebserverVerification(enabled = action.enabled)
            }
            else -> Unit
        }
    }

    private fun checkWebserverPassword(input: String) {
        viewModelScope.launch {
            val currentHash = appConfigRepository.webserverPasswordHash.first()
            val isValid = appConfigRepository.isValidPassword(input, currentHash)

            _passwordError.value = !isValid
            if (isValid) {
                _isAuthorized.value = true
            }
        }
    }

    private fun checkAdminPassword(input: String) {
        viewModelScope.launch {
            val currentHash = appConfigRepository.adminPanelPasswordHash.first()
            val isValid = appConfigRepository.isValidPassword(input, currentHash)

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
                appConfigRepository.updateUrl(action.url)
            }
            AdminPanelAction.ResetCoordinates -> viewModelScope.launch {
                appConfigRepository.updateCoordinates(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
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
                appConfigRepository.updateSpeakerVerification(enabled = action.enabled)
            }
            is AdminPanelAction.EditSpeakerVerificationThreshold -> viewModelScope.launch {
                appConfigRepository.updateSpeakerVerification(threshold = action.threshold)
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
            appConfigRepository.updateCoordinates(roundedLat, roundedLon)
        }
    }

    fun onSavePatrolSettings(
        isEnabled: Boolean,
        mode: AdminPanelPatrolSettingsDialog,
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

    fun onTriggerImmediatePatrol() {
        val success = patrolManager.startImmediatePatrol(uiState.value.patrolRoute)
        if (!success) {
            viewModelScope.launch { _events.emit(AdminPanelEvent.NoRouteSelected) }
        }
    }

    fun onSavePatrolRoute(route: List<String>) {
        viewModelScope.launch {
            appConfigRepository.updatePatrolRoute(route)

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

    fun onExitPatrol() {
        patrolManager.stopPatrol()
    }

    companion object {
        private const val STATE_TIMEOUT = 5000L
        private const val DEFAULT_LATITUDE = 49.0138
        private const val DEFAULT_LONGITUDE = 8.3573
        private const val COORDINATE_PRECISION = 10000.0
    }
}

/**
 * Interface representing actions that can be performed in the Admin Panel.
 */
sealed interface AdminPanelAction {
    data object ClearPasswordError : AdminPanelAction
    data object ResetAuthorization : AdminPanelAction

    data class EditCoordinates(val latitude: Double, val longitude: Double) : AdminPanelAction
    data class EditWebserverUrl(val url: String) : AdminPanelAction
    data object ResetCoordinates : AdminPanelAction
    data object OpenMqttReports : AdminPanelAction
    data object ClearMqttReports : AdminPanelAction
    data object RequestRestart : AdminPanelAction

    data class CheckWebserverPassword(val password: String) : AdminPanelAction
    data class CheckAdminPassword(val password: String) : AdminPanelAction
    data class ChangeAdminPassword(val password: String) : AdminPanelAction
    data class ToggleWebserverVerification(val enabled: Boolean) : AdminPanelAction
    data class ChangeWebserverPassword(val password: String, val user: String) : AdminPanelAction

    data class ToggleSpeakerVerification(val enabled: Boolean) : AdminPanelAction
    data class EditSpeakerVerificationThreshold(val threshold: Double) : AdminPanelAction
    data object ResetVoiceProfiles : AdminPanelAction
    data class ToggleEnrollment(val active: Boolean, val name: String? = null) : AdminPanelAction
    data class DeleteVoiceProfile(val name: String) : AdminPanelAction
    data object RequestCloseApp : AdminPanelAction

    data class SavePatrolSettings(
        val isEnabled: Boolean,
        val mode: AdminPanelPatrolSettingsDialog,
        val minMinutes: Int,
        val maxMinutes: Int,
        val hours: Set<Int>,
    ) : AdminPanelAction

    data class SavePatrolRoute(val route: List<String>) : AdminPanelAction
    data object TriggerImmediatePatrol : AdminPanelAction
    data object ExitPatrol : AdminPanelAction
}

/**
 * Events emitted by the Admin Panel to the UI.
 */
sealed interface AdminPanelEvent {
    data object OpenMqttReports : AdminPanelEvent
    data object WebserverPasswordChanged : AdminPanelEvent
    data object PasswordChanged : AdminPanelEvent
    data object RestartAppTriggered : AdminPanelEvent
    data object CloseAppTriggered : AdminPanelEvent
    data object NoRouteSelected : AdminPanelEvent
}

/**
 * State representing the UI of the Admin Panel.
 */
data class AdminPanelState(
    val webserverUrl: String = BuildConfig.WEBVIEW_URL,
    val isWebserverVerificationEnabled: Boolean = false,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val mqttReportTopics: Set<String> = emptySet(),
    val mqttTrafficEvents: List<MqttTrafficEvent> = emptyList(),
    val coordinates: String = "",
    val isSpeakerVerificationEnabled: Boolean = false,
    val speakerVerificationThreshold: Double = AppConfigRepository.DEFAULT_SPEAKER_VERIFICATION_THRESHOLD,
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
