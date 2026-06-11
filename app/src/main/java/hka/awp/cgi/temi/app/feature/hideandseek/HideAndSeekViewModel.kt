package hka.awp.cgi.temi.app.feature.hideandseek

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToLocationChangedListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

private const val HIDING_COUNTDOWN_SECONDS = 40
private const val DEFAULT_SEARCH_MINUTES = 3
private const val MIN_SEARCH_MINUTES = 1
private const val MAX_SEARCH_MINUTES = 10
private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60
private const val MIN_HIDING_DISTANCE_METERS = 4f

enum class GameState { SETUP, HIDING, WAITING, WON, LOST }

data class HideAndSeekUiState(
    val gameState: GameState = GameState.SETUP,
    val searchTimeMinutes: Int = DEFAULT_SEARCH_MINUTES,
    val hidingSecondsRemaining: Int = HIDING_COUNTDOWN_SECONDS,
    val searchSecondsRemaining: Int = 0,
    val elapsedSeconds: Int = 0,
    val hidingSpotName: String = "",
    val errorMessage: String? = null
)

@Suppress("TooManyFunctions")
class HideAndSeekViewModel(
    private val robot: Robot?,
    hidingSpotRepository: HidingSpotRepository
) : ViewModel(),
    OnGoToLocationStatusChangedListener,
    OnDistanceToLocationChangedListener {

    private val _uiState = MutableStateFlow(HideAndSeekUiState())
    val uiState: StateFlow<HideAndSeekUiState> = _uiState.asStateFlow()

    val filterManager = HidingSpotFilterManager(robot, hidingSpotRepository)

    private val navigator = HideAndSeekNavigator(robot, viewModelScope)
    private var timerJob: Job? = null
    private var distancesToLocations: Map<String, Float> = emptyMap()

    init {
        robot?.addOnGoToLocationStatusChangedListener(this)
        robot?.addOnDistanceToLocationChangedListener(this)
    }

    override fun onCleared() {
        timerJob?.cancel()
        navigator.release()
        robot?.removeOnGoToLocationStatusChangedListener(this)
        robot?.removeOnDistanceToLocationChangedListener(this)
        super.onCleared()
    }

    override fun onDistanceToLocationChanged(distances: Map<String, Float>) {
        distancesToLocations = distances
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        if (location != _uiState.value.hidingSpotName) return
        when (status) {
            OnGoToLocationStatusChangedListener.COMPLETE ->
                Timber.d("Arrived at hiding spot: %s", location)

            OnGoToLocationStatusChangedListener.ABORT -> {
                Timber.w("Navigation to hiding spot aborted: %s", location)
                handleNavigationFailure()
            }
        }
    }

    private fun handleNavigationFailure() {
        timerJob?.cancel()
        robot?.stopMovement()
        _uiState.update {
            it.copy(
                gameState = GameState.SETUP,
                errorMessage = "Navigation zum Versteck fehlgeschlagen.",
                hidingSpotName = ""
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun adjustSearchTime(delta: Int) {
        _uiState.update {
            it.copy(searchTimeMinutes = (it.searchTimeMinutes + delta).coerceIn(MIN_SEARCH_MINUTES, MAX_SEARCH_MINUTES))
        }
    }

    fun startGame() {
        clearError()
        val hidingSpot = selectHidingSpot(robot, distancesToLocations, filterManager.savedEnabledSpots)
        _uiState.update {
            it.copy(
                gameState = GameState.HIDING,
                hidingSecondsRemaining = HIDING_COUNTDOWN_SECONDS,
                hidingSpotName = hidingSpot ?: ""
            )
        }
        if (hidingSpot != null) navigator.navigateTo(hidingSpot)
        startHidingCountdown()
    }

    private fun startHidingCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = HIDING_COUNTDOWN_SECONDS
            while (remaining > 0 && isActive) {
                _uiState.update { it.copy(hidingSecondsRemaining = remaining) }
                delay(MILLIS_PER_SECOND)
                remaining--
            }
            if (!isActive) return@launch
            _uiState.update { it.copy(hidingSecondsRemaining = 0) }
            transitionToWaiting()
        }
    }

    fun transitionToWaiting() {
        timerJob?.cancel()
        val totalSearch = _uiState.value.searchTimeMinutes * SECONDS_PER_MINUTE
        _uiState.update {
            it.copy(
                gameState = GameState.WAITING,
                searchSecondsRemaining = totalSearch,
                elapsedSeconds = 0
            )
        }
        startSearchTimer()
    }

    private fun startSearchTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(MILLIS_PER_SECOND)
                _uiState.update { state ->
                    if (state.gameState != GameState.WAITING) return@update state
                    val newRemaining = state.searchSecondsRemaining - 1
                    val newElapsed = state.elapsedSeconds + 1
                    if (newRemaining <= 0) {
                        state.copy(gameState = GameState.LOST, searchSecondsRemaining = 0, elapsedSeconds = newElapsed)
                    } else {
                        state.copy(searchSecondsRemaining = newRemaining, elapsedSeconds = newElapsed)
                    }
                }
                if (_uiState.value.gameState == GameState.LOST) break
            }
        }
    }

    fun onPlayerFound() {
        timerJob?.cancel()
        _uiState.update { it.copy(gameState = GameState.WON) }
    }

    fun cancelGame() {
        timerJob?.cancel()
        if (_uiState.value.gameState == GameState.HIDING) {
            robot?.stopMovement()
        }
        _uiState.update { state ->
            HideAndSeekUiState(searchTimeMinutes = state.searchTimeMinutes)
        }
    }
}

private fun selectHidingSpot(
    robot: Robot?,
    distancesToLocations: Map<String, Float>,
    allowedSpots: Set<String>?
): String? {
    val allLocations = robot?.locations ?: return null
    val locations = if (allowedSpots != null) allLocations.filter { it in allowedSpots } else allLocations
    if (locations.isEmpty()) return null

    val nearestLocation = distancesToLocations.minByOrNull { it.value }?.key
    val candidates = locations.filter { location ->
        val distance = distancesToLocations[location]
        distance == null || distance >= MIN_HIDING_DISTANCE_METERS
    }

    return if (candidates.isNotEmpty()) {
        candidates.random()
    } else {
        locations.filter { it != nearestLocation }.randomOrNull() ?: locations.random()
    }
}
