package hka.awp.cgi.temi.app.feature.settings.display

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.data.repository.RobotRepository
import hka.awp.cgi.temi.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DisplayViewModel(
    application: Application,
    private val repository: RobotRepository
                      ) : AndroidViewModel(application) {

    private val _brightness = MutableStateFlow(50f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    val timeoutOptions = listOf(
        getApplication<Application>().getString(
            R.string.timeout_30_seconds
                                               ) to 30000,

        getApplication<Application>().getString(
            R.string.timeout_1_minute
                                               ) to 60000,

        getApplication<Application>().getString(
            R.string.timeout_2_minutes
                                               ) to 120000,

        getApplication<Application>().getString(
            R.string.timeout_5_minutes
                                               ) to 300000,

        getApplication<Application>().getString(
            R.string.timeout_10_minutes
                                               ) to 600000,

        getApplication<Application>().getString(
            R.string.timeout_15_minutes
                                               ) to 900000
                               )
    private val _screenTimeout = MutableStateFlow(timeoutOptions[2]) // Standard: 1 Min
    val screenTimeout = _screenTimeout.asStateFlow()
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    init {
        viewModelScope.launch {
            _isDarkMode.value =
                repository.getDarkMode(getApplication())
        }
    }

    fun updateBrightness(newValue: Float) {
        _brightness.value = newValue

        val brightnessPercent = (newValue * 100).toInt()

        repository.setBrightness(
            brightnessPercent,
            getApplication()
                                )
    }

    fun updateTimeout(
        option: Pair<String, Int>
                     ) {
        _screenTimeout.value = option

        repository.setScreenTimeout(
            option.second,
            getApplication()
                                   )
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled

        repository.saveDarkMode(
            enabled,
            getApplication()
                               )
    }
}
