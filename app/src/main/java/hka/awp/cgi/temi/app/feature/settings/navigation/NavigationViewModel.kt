package hka.awp.cgi.temi.app.feature.settings.navigation

import androidx.lifecycle.ViewModel
import com.robotemi.sdk.Robot
import com.robotemi.sdk.navigation.model.SpeedLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class NavigationViewModel(private val robot: Robot?) : ViewModel() {

    private val _goToSpeed = MutableStateFlow(SpeedLevel.MEDIUM)
    val goToSpeed: StateFlow<SpeedLevel> = _goToSpeed.asStateFlow()

    init {
        _goToSpeed.value = robot?.goToSpeed ?: SpeedLevel.MEDIUM
    }

    fun updateGoToSpeed(newSpeed: SpeedLevel) {
        robot?.goToSpeed = newSpeed
        Timber.d("Old speed = ${robot?.goToSpeed}")
        _goToSpeed.update { newSpeed }
        Timber.d("Mid speed = ${robot?.goToSpeed}")
        robot?.goToSpeed = SpeedLevel.HIGH

        Timber.d("New speed = ${robot?.goToSpeed}")
    }
}
