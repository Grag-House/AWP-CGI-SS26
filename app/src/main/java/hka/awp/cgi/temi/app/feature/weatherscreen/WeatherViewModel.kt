package hka.awp.cgi.temi.app.feature.weatherscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class WeatherViewModel : ViewModel() {

    private val _hourlyData = MutableStateFlow<List<HourlyItem>>(emptyList())
    val hourlyData: StateFlow<List<HourlyItem>> = _hourlyData

    private val _dailyData = MutableStateFlow<List<DailyItem>>(emptyList())
    val dailyData: StateFlow<List<DailyItem>> = _dailyData

    private fun startFetching() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                _hourlyData.value = WeatherCards.setHourlyWeatherCards()
                _dailyData.value = WeatherCards.setDailyWeatherCards()
                delay(POLLING_INTERVAL)
            }
        }
    }

    init {
        Timber.d("WeatherApi fetching started")
        startFetching()
    }

    companion object {
        private const val POLLING_INTERVAL: Long = 30000
    }
}
