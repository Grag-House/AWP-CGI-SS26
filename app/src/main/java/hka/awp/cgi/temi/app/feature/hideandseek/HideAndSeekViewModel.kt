package hka.awp.cgi.temi.app.feature.hideandseek

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val HIDING_COUNTDOWN_SECONDS = 20
private const val DEFAULT_SEARCH_MINUTES = 3
private const val MIN_SEARCH_MINUTES = 1
private const val MAX_SEARCH_MINUTES = 10
private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60

enum class GameState { SETUP, HIDING, WAITING, WON, LOST }

data class HideAndSeekUiState(
    val gameState: GameState = GameState.SETUP,
    val searchTimeMinutes: Int = DEFAULT_SEARCH_MINUTES,
    val hidingSecondsRemaining: Int = HIDING_COUNTDOWN_SECONDS,
    val searchSecondsRemaining: Int = 0,
    val elapsedSeconds: Int = 0,
    val hidingSpotName: String = ""
)

class HideAndSeekViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HideAndSeekUiState())
    val uiState: StateFlow<HideAndSeekUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun increaseSearchTime() {
        _uiState.update {
            it.copy(searchTimeMinutes = (it.searchTimeMinutes + 1).coerceAtMost(MAX_SEARCH_MINUTES))
        }
    }

    fun decreaseSearchTime() {
        _uiState.update {
            it.copy(searchTimeMinutes = (it.searchTimeMinutes - 1).coerceAtLeast(MIN_SEARCH_MINUTES))
        }
    }

    fun startGame() {
        _uiState.update {
            it.copy(
                gameState = GameState.HIDING,
                hidingSecondsRemaining = HIDING_COUNTDOWN_SECONDS
            )
        }
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
            if (isActive) transitionToWaiting()
        }
    }

    // Called by the backend once the robot has reached its hiding spot.
    // The frontend triggers this automatically after HIDING_COUNTDOWN_SECONDS as a placeholder.
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
        _uiState.update { state ->
            HideAndSeekUiState(searchTimeMinutes = state.searchTimeMinutes)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
