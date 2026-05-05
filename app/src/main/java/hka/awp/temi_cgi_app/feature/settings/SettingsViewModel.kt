package hka.awp.temi_cgi_app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.temi_cgi_app.data.repository.RobotRepository
import hka.awp.temi_cgi_app.data.repository.RobotInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: RobotRepository) : ViewModel() {

    private val _aboutInfo = MutableStateFlow<RobotInfo?>(null)
    val aboutInfo: StateFlow<RobotInfo?> = _aboutInfo.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SettingsNavigationEvent>()
    val navigationEvent: SharedFlow<SettingsNavigationEvent> = _navigationEvent.asSharedFlow()

    fun onSettingsItemClick(item: SettingsItem) {
        when (item) {
            SettingsItem.Display -> {
                viewModelScope.launch {
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToDisplay)
                }
            }
            SettingsItem.About -> {
                _aboutInfo.value = repository.getFullDeviceInfo()
            }
            SettingsItem.Notifications -> {
                viewModelScope.launch {
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToNotifications)
                }
            }
            SettingsItem.Battery -> {
                viewModelScope.launch {
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToBattery)
                }
            }
            SettingsItem.Location -> { /* TODO */ }
        }
    }

    fun dismissAbout() {
        _aboutInfo.value = null
    }
}

sealed class SettingsNavigationEvent {
    data object NavigateToDisplay : SettingsNavigationEvent()
    data object NavigateToNotifications : SettingsNavigationEvent()
    data object NavigateToBattery : SettingsNavigationEvent()
}