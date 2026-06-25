package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotFilterManager
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotRepository
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import hka.awp.cgi.temi.app.feature.mqtt.MqttTrafficEvent
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
import kotlinx.coroutines.launch
import kotlin.math.round

class AdminPanelViewModel(
    private val appConfigRepository: AppConfigRepository,
    private val mqttManager: MqttManager,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val voiceRecognitionViewModel: TemiVoiceRecognitionViewModel,
    robot: Robot?,
    hidingSpotRepository: HidingSpotRepository
) : ViewModel() {

    val filterManager = HidingSpotFilterManager(robot, hidingSpotRepository)

    private val _events = MutableSharedFlow<AdminPanelEvent>()
    val events = _events.asSharedFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _passwordError = MutableStateFlow(false)
    val passwordError = _passwordError.asStateFlow()

    private data class AdminPanelFlows(
        val url: String,
        val latitude: Double,
        val longitude: Double,
        val speakerEnabled: Boolean,
        val speakerThreshold: Double,
    )

    private val baseConfigFlow = combine(
        appConfigRepository.currentUrl,
        appConfigRepository.latitude,
        appConfigRepository.longitude,
        appConfigRepository.isSpeakerVerificationEnabled,
    ) { url, lat, lon, speakerEnabled ->
        AdminPanelFlows(
            url = url,
            latitude = lat,
            longitude = lon,
            speakerEnabled = speakerEnabled,
            speakerThreshold = AppConfigRepository.DEFAULT_SPEAKER_VERIFICATION_THRESHOLD,
        )
    }

    private val configWithThresholdFlow = combine(
        baseConfigFlow,
        appConfigRepository.speakerVerificationThreshold,
    ) { config, threshold ->
        config.copy(speakerThreshold = threshold)
    }

    val uiState: StateFlow<AdminPanelState> = combine(
        configWithThresholdFlow,
        mqttManager.trafficEvents,
        voiceProfileRepository.voiceProfiles,
        voiceRecognitionViewModel.isEnrollmentActive,
    ) { config, trafficEvents, voiceProfiles, isEnrollmentActive ->
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
            isEnrollmentActive = isEnrollmentActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT),
        initialValue = AdminPanelState()
    )

    fun onAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.CheckPassword,
            AdminPanelAction.ClearPasswordError,
            AdminPanelAction.ResetAuthorization,
            is AdminPanelAction.ChangePassword -> handleSecurityAction(action)

            is AdminPanelAction.EditCoordinates,
            is AdminPanelAction.EditWebserverUrl,
            AdminPanelAction.ResetCoordinates,
            AdminPanelAction.OpenMqttReports,
            AdminPanelAction.ClearMqttReports,
            AdminPanelAction.RequestRestart -> handleConfigAction(action)

            is AdminPanelAction.ToggleSpeakerVerification,
            is AdminPanelAction.EditSpeakerVerificationThreshold,
            AdminPanelAction.ResetVoiceProfiles,
            is AdminPanelAction.ToggleEnrollment,
            is AdminPanelAction.DeleteVoiceProfile -> handleVoiceAction(action)
        }
    }

    private fun handleSecurityAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.CheckPassword -> handleCheckPassword(action.password)
            AdminPanelAction.ClearPasswordError -> _passwordError.value = false
            AdminPanelAction.ResetAuthorization -> {
                _isAuthorized.value = false
                _passwordError.value = false
            }
            is AdminPanelAction.ChangePassword -> viewModelScope.launch {
                appConfigRepository.updateAdminPassword(action.password)
                _events.emit(AdminPanelEvent.PasswordChanged)
            }
            else -> Unit
        }
    }

    private fun handleConfigAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.EditCoordinates -> updateCoordinates(action.latitude, action.longitude)
            is AdminPanelAction.EditWebserverUrl -> viewModelScope.launch {
                appConfigRepository.updateUrl(action.url)
            }
            AdminPanelAction.ResetCoordinates -> viewModelScope.launch {
                @Suppress("MagicNumber")
                appConfigRepository.updateCoordinates(49.0138, 8.3573)
            }
            AdminPanelAction.OpenMqttReports -> viewModelScope.launch {
                _events.emit(AdminPanelEvent.OpenMqttReports)
            }
            AdminPanelAction.ClearMqttReports -> mqttManager.clearTrafficEvents()
            AdminPanelAction.RequestRestart -> viewModelScope.launch {
                _events.emit(AdminPanelEvent.RestartAppTriggered)
            }
            else -> Unit
        }
    }

    private fun handleVoiceAction(action: AdminPanelAction) {
        when (action) {
            is AdminPanelAction.ToggleSpeakerVerification -> viewModelScope.launch {
                appConfigRepository.updateSpeakerVerificationEnabled(action.enabled)
            }
            is AdminPanelAction.EditSpeakerVerificationThreshold -> viewModelScope.launch {
                appConfigRepository.updateSpeakerVerificationThreshold(action.threshold)
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

    private fun handleCheckPassword(input: String) {
        viewModelScope.launch {
            val currentHash = appConfigRepository.adminPasswordHash.first()
            val isValid = appConfigRepository.isValidAdminPassword(input, currentHash)
            _passwordError.value = !isValid
            if (isValid) _isAuthorized.value = true
        }
    }

    private fun updateCoordinates(latitude: Double, longitude: Double) {
        @Suppress("MagicNumber")
        val roundedLat = round(latitude * 10000.0) / 10000.0

        @Suppress("MagicNumber")
        val roundedLon = round(longitude * 10000.0) / 10000.0

        viewModelScope.launch {
            appConfigRepository.updateCoordinates(roundedLat, roundedLon)
        }
    }

    companion object {
        private const val STATE_TIMEOUT = 5000L
    }
}

sealed interface AdminPanelAction {
    data class CheckPassword(val password: String) : AdminPanelAction
    data object ClearPasswordError : AdminPanelAction
    data object ResetAuthorization : AdminPanelAction
    data class EditCoordinates(val latitude: Double, val longitude: Double) : AdminPanelAction
    data class EditWebserverUrl(val url: String) : AdminPanelAction
    data object ResetCoordinates : AdminPanelAction
    data object OpenMqttReports : AdminPanelAction
    data object ClearMqttReports : AdminPanelAction
    data class ChangePassword(val password: String) : AdminPanelAction
    data class ToggleSpeakerVerification(val enabled: Boolean) : AdminPanelAction
    data class EditSpeakerVerificationThreshold(val threshold: Double) : AdminPanelAction
    data object ResetVoiceProfiles : AdminPanelAction
    data class ToggleEnrollment(val active: Boolean, val name: String? = null) : AdminPanelAction
    data class DeleteVoiceProfile(val name: String) : AdminPanelAction
    data object RequestRestart : AdminPanelAction
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
    val isSpeakerVerificationEnabled: Boolean = false,
    val speakerVerificationThreshold: Double = AppConfigRepository.DEFAULT_SPEAKER_VERIFICATION_THRESHOLD,
    val voiceProfileCount: Int = 0,
    val voiceProfiles: Map<String, SpeakerVector> = emptyMap(),
    val isEnrollmentActive: Boolean = false,

    @Suppress("MagicNumber")
    var longitude: Double = 8.3573,

    @Suppress("MagicNumber")
    var latitude: Double = 49.0138
)
