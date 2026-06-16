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

    // TODO this is ugly and potentially unsafe --> maybe create a data class for this instead of relying on the order
    // of the combine arguments?
    // Or at least add some comments to clarify the order and types of the arguments
    val uiState: StateFlow<AdminPanelState> = combine(
        appConfigRepository.currentUrl,
        appConfigRepository.latitude,
        appConfigRepository.longitude,
        appConfigRepository.isSpeakerVerificationEnabled,
        mqttManager.trafficEvents,
        voiceProfileRepository.voiceProfiles,
        voiceRecognitionViewModel.isEnrollmentActive
    ) { args: Array<*> ->
        val url = args[0] as String
        val lat = args[1] as Double
        val lon = args[2] as Double
        val speakerEnabled = args[3] as Boolean
        val trafficEvents = args[4] as List<MqttTrafficEvent>
        val voiceProfiles = args[5] as Map<String, List<Float>>
        val isEnrollmentActive = args[6] as Boolean

        AdminPanelState(
            webserverUrl = url,
            latitude = lat,
            longitude = lon,
            coordinates = "Länge: $lon Breite: $lat",
            isSpeakerVerificationEnabled = speakerEnabled,
            mqttReportTopics = MqttManager.reportTopics,
            mqttTrafficEvents = trafficEvents.filter { it.topic in MqttManager.reportTopics },
            voiceProfileCount = voiceProfiles.size,
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

    fun onResetVoiceProfiles() {
        viewModelScope.launch {
            voiceProfileRepository.clearAllProfiles()
        }
    }

    fun onToggleEnrollment(active: Boolean, name: String = "Default") {
        voiceRecognitionViewModel.toggleEnrollment(active, name)
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
    val voiceProfileCount: Int = 0,
    val isEnrollmentActive: Boolean = false,
    @Suppress("MagicNumber")
    var longitude: Double = 8.3573,
    @Suppress("MagicNumber")
    var latitude: Double = 49.0138
)
