package hka.awp.cgi.temi.app.feature.weatherscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.feature.webserver.AppConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * ViewModel for the weather screen that manages the UI state and handles periodic data fetching.
 *
 * This ViewModel interacts with the [WeatherRepository] to retrieve weather data every full hour.
 * It exposes the current state via a [StateFlow]
 * of [WeatherState], which includes loading status, weather data, and error messages.
 *
 * @property repository The repository used to fetch weather data from a data source.
 * @property appConfigRepository The repository used to fetch location coordinates.
 */
class WeatherViewModel(
    private val repository: WeatherRepository,
    private val appConfigRepository: AppConfigRepository,
    private val clock: Clock,
) :
    ViewModel() {

    private val _uiState = MutableStateFlow(WeatherState())
    val uiState: StateFlow<WeatherState> = _uiState.asStateFlow()

    init {
        startFetching()
    }

    private fun delayUntilNextFullHourMillis(): Long {
        val now = clock.instant().atZone(clock.zone)
        val nextHourWithOffset = now.truncatedTo(ChronoUnit.HOURS)
            .plusHours(1)
            .plusSeconds(FETCH_OFFSET_SECONDS)

        return Duration.between(now, nextHourWithOffset)
            .toMillis()
            .coerceAtLeast(0L)
    }

    private fun startFetching() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                _uiState.update { it.copy(isLoading = true) }

                val lat = appConfigRepository.latitude.first()
                val lon = appConfigRepository.longitude.first()

                repository.getWeatherData(lat, lon)
                    .onSuccess { newState ->
                        _uiState.value = newState.copy(isLoading = false)
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }

                delay(delayUntilNextFullHourMillis())
            }
        }
    }

    fun oneTimeFetch() {
        viewModelScope.launch(Dispatchers.IO) {
            val lat = appConfigRepository.latitude.first()
            val lon = appConfigRepository.longitude.first()

            repository.getWeatherData(lat, lon)
                .onSuccess { newState ->
                    _uiState.value = newState.copy(isLoading = false)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    companion object {
        private const val FETCH_OFFSET_SECONDS = 3L
    }
}
