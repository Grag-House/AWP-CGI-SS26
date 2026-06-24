package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import hka.awp.cgi.temi.app.feature.mqtt.MqttTrafficEvent
import hka.awp.cgi.temi.app.feature.voiceRecognition.TemiVoiceRecognitionViewModel
import hka.awp.cgi.temi.app.feature.voiceRecognition.VoiceProfileRepository
import hka.awp.cgi.temi.app.feature.webserver.AppConfigRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.round
class AdminPanelViewModel(
    private val appConfigRepository: AppConfigRepository,
    private val mqttManager: MqttManager,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val voiceRecognitionViewModel: TemiVoiceRecognitionViewModel
) : ViewModel() {

    private val _events = MutableSharedFlow<AdminPanelEvent>()
    val events = _events.asSharedFlow()

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

    fun onToggleSpeakerVerification(enabled: Boolean) {
        viewModelScope.launch {
            appConfigRepository.updateSpeakerVerificationEnabled(enabled)
        }
    }

    fun onEditSpeakerVerificationThreshold(threshold: Double) {
        viewModelScope.launch {
            appConfigRepository.updateSpeakerVerificationThreshold(threshold)
        }
    }

    fun onResetVoiceProfiles() {
        viewModelScope.launch {
            voiceProfileRepository.clearAllProfiles()
        }
    }

    fun onToggleEnrollment(active: Boolean, name: String? = null) {
        voiceRecognitionViewModel.toggleEnrollment(active, name)
    }

    fun onDeleteVoiceProfile(name: String) {
        viewModelScope.launch {
            voiceProfileRepository.deleteVoiceProfile(name)
        }
    }

    companion object {
        private const val STATE_TIMEOUT = 5000L
    }
}

sealed interface AdminPanelEvent {
    data object OpenMqttReports : AdminPanelEvent
    data object PasswordChanged : AdminPanelEvent
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
