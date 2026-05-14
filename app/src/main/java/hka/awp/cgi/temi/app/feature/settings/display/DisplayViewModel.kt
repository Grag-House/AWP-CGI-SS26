package hka.awp.cgi.temi.app.feature.settings.display

import android.content.Context
import androidx.lifecycle.ViewModel
import hka.awp.cgi.temi.app.data.repository.RobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisplayViewModel(private val repository: RobotRepository) : ViewModel() {

    private val _brightness = MutableStateFlow(50f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()
    val timeoutOptions = listOf(
        "30 Sekunden" to 30000,
        "1 Minute" to 60000,
        "2 Minute" to 120000,
        "5 Minuten" to 300000,
        "10 Minute" to 600000,
        "15 Minuten" to 900000
    )
    private val _screenTimeout = MutableStateFlow(timeoutOptions[2]) // Standard: 1 Min
    val screenTimeout = _screenTimeout.asStateFlow()
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    fun updateBrightness(newValue: Float, context: Context) {
        _brightness.value = newValue
        val brightnessPercent = (newValue * 100).toInt()
        repository.setBrightness( brightnessPercent,context)
    }

    fun updateTimeout(option: Pair<String, Int>, context: Context) {
        _screenTimeout.value = option
        repository.setScreenTimeout(option.second, context)
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        // TODO Safe for ever
    }
}
