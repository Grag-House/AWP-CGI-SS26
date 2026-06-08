package hka.awp.cgi.temi.app.feature.settings.display

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.data.repository.RobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
class DisplayViewModel(
    private val context: Application,
    private val repository: RobotRepository
) : ViewModel() {

    private val _brightness = MutableStateFlow(50f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    val timeoutOptions = listOf(
        context.getString(R.string.timeout_30_seconds) to 30000,
        context.getString(R.string.timeout_1_minute) to 60000,
        context.getString(R.string.timeout_2_minutes) to 120000,
        context.getString(R.string.timeout_5_minutes) to 300000,
        context.getString(R.string.timeout_10_minutes) to 600000,
        context.getString(R.string.timeout_15_minutes) to 900000
    )

    private val _screenTimeout = MutableStateFlow(timeoutOptions[2])
    val screenTimeout = _screenTimeout.asStateFlow()
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    init {
        viewModelScope.launch {
            _isDarkMode.value = repository.getDarkMode(context)
        }
    }

    fun updateBrightness(newValue: Float) {
        _brightness.value = newValue

        val brightnessPercent = (newValue * 100).toInt()

        repository.setBrightness(brightnessPercent, context)
    }

    fun updateTimeout(option: Pair<String, Int>) {
        _screenTimeout.value = option

        repository.setScreenTimeout(option.second, context)
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled

        repository.saveDarkMode(enabled, context)
    }
}
