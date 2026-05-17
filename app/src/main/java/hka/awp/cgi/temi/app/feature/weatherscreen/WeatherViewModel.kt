package hka.awp.cgi.temi.app.feature.weatherscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherViewModel.Companion.POLLING_INTERVAL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for the weather screen that manages the UI state and handles periodic data fetching.
 *
 * This ViewModel interacts with the [WeatherRepository] to retrieve weather data at a fixed
 * interval defined by [POLLING_INTERVAL]. It exposes the current state via a [StateFlow]
 * of [WeatherState], which includes loading status, weather data, and error messages.
 *
 * @property repository The repository used to fetch weather data from a data source.
 */
class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherState())
    val uiState: StateFlow<WeatherState> = _uiState.asStateFlow()

    init {
        startFetching()
    }

    private fun startFetching() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                _uiState.update { it.copy(isLoading = true) }

                repository.getWeatherData()
                    .onSuccess { newState ->
                        _uiState.value = newState.copy(isLoading = false)
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }

                delay(POLLING_INTERVAL)
            }
        }
    }

    companion object {
        private const val POLLING_INTERVAL: Long = 3_600_000
    }
}
