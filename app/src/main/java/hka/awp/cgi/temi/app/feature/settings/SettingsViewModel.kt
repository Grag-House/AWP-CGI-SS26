package hka.awp.cgi.temi.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.data.model.RobotInfo
import hka.awp.cgi.temi.app.data.repository.RobotRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Master ViewModel responsible for handling user item selections within the global Settings hub.
 *
 * It serves as an central routing engine that takes abstract layout interaction events ([SettingsItem])
 * and converts them into downstream side-effects. This includes processing asynchronous window operations,
 * querying diagnostic hardware configurations from the [RobotRepository], and emitting explicit, one-time
 * navigation signals via a shared event architecture.
 *
 * @property repository The source provider handling structural metadata aggregation for the underlying hardware.
 * @property robot The low-level Temi Robot SDK process controller reference, nullable if unbound.
 */
class SettingsViewModel(private val repository: RobotRepository, private val robot: Robot?) : ViewModel() {

    private val _aboutInfo = MutableStateFlow<RobotInfo?>(null)

    /**
     * An observable state flow holding detailed technical information about the robot device.
     * Emits `null` when the modal or view displaying this information is closed.
     */
    val aboutInfo: StateFlow<RobotInfo?> = _aboutInfo.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SettingsNavigationEvent>()

    /**
     * A hot event stream emitting exact, singular destination commands to coordinate screen navigation changes.
     */
    val navigationEvent: SharedFlow<SettingsNavigationEvent> = _navigationEvent.asSharedFlow()

    /**
     * Processes input selection choices from the settings list options, routing actions into data queries
     * or screen transitions.
     *
     * @param item The specific entry item selected by the user.
     */
    fun onSettingsItemClick(item: SettingsItem) {
        when (item) {
            SettingsItem.Display -> {
                viewModelScope.launch {
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToDisplay)
                }
            }

            SettingsItem.About -> {
                _aboutInfo.value = repository.getFullDeviceInfo(robot)
            }

            SettingsItem.Language -> {
                viewModelScope.launch {
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToLanguage)
                }
            }

            SettingsItem.Battery -> {
                viewModelScope.launch {
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToBattery)
                }
            }

            SettingsItem.AdminPanel -> {
                viewModelScope.launch {
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToAdminPanel)
                }
            }

            SettingsItem.Photobox -> {
                viewModelScope.launch {
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToPhotobox)
                }
            }
        }
    }

    /**
     * Clears the current device info data state, signaling that the 'About' detail overlay should be dismissed.
     */
    fun dismissAbout() {
        _aboutInfo.value = null
    }
}

/**
 * Defines the set of possible deterministic routing navigation instructions originating from the settings sub-features.
 */
sealed class SettingsNavigationEvent {
    /** Navigation instruction routing to the Display custom configurations interface. */
    data object NavigateToDisplay : SettingsNavigationEvent()

    /** Navigation instruction routing to the Language localization panel. */
    data object NavigateToLanguage : SettingsNavigationEvent()

    /** Navigation instruction routing to the Battery health and power telemetry tracker. */
    data object NavigateToBattery : SettingsNavigationEvent()

    /** Navigation instruction routing to the restricted Administrator configuration console. */
    data object NavigateToAdminPanel : SettingsNavigationEvent()

    /** Navigation instruction routing to the Photobox media frame and backup automation layout. */
    data object NavigateToPhotobox : SettingsNavigationEvent()
}
