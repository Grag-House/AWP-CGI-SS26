package hka.awp.cgi.temi.app.feature.weatherscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong

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
    private val fetchSequence = AtomicLong(0)

    init {
        startWeatherRefreshPipeline()
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

    private fun refreshTriggerFlow(): Flow<Unit> = flow {
        // emit something here so startWeatherRefreshPipeline will recognize this
        emit(Unit)
        while (currentCoroutineContext().isActive) {
            delay(delayUntilNextFullHourMillis())
            emit(Unit)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startWeatherRefreshPipeline() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(appConfigRepository.location.latitude, appConfigRepository.location.longitude) { lat, lon ->
                lat to lon
            }
                .distinctUntilChanged()
                .flatMapLatest { coordinates ->
                    refreshTriggerFlow().map { coordinates }
                }
                .collectLatest { (lat, lon) -> fetchWeather(lat, lon) }
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double) {
        val requestId = fetchSequence.incrementAndGet()
        _uiState.update { it.copy(isLoading = true, error = null) }

        repository.getWeatherData(lat, lon)
            .onSuccess { newState ->
                if (requestId == fetchSequence.get()) {
                    _uiState.value = newState.copy(isLoading = false)
                }
            }
            .onFailure { error ->
                if (requestId == fetchSequence.get()) {
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            }
    }

    companion object {
        private const val FETCH_OFFSET_SECONDS = 3L
    }
}
