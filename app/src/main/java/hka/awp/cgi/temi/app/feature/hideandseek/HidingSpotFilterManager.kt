package hka.awp.cgi.temi.app.feature.hideandseek

import com.robotemi.sdk.Robot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HidingSpotFilterManager(
    private val robot: Robot?,
    private val repository: HidingSpotRepository
) {
    private val _filterState = MutableStateFlow(HidingSpotFilterState())
    val filterState: StateFlow<HidingSpotFilterState> = _filterState.asStateFlow()

    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()

    private val _hasActiveFilter = MutableStateFlow(false)
    val hasActiveFilter: StateFlow<Boolean> = _hasActiveFilter.asStateFlow()

    var savedEnabledSpots: Set<String>? = null
        private set

    init {
        savedEnabledSpots = repository.loadEnabledSpots()
        _hasActiveFilter.value = savedEnabledSpots != null
    }

    fun open() {
        val locations = robot?.locations?.sorted() ?: emptyList()
        val enabled = savedEnabledSpots ?: locations.toSet()
        _filterState.value = HidingSpotFilterState(allLocations = locations, enabledSpots = enabled)
        _isOpen.value = true
    }

    fun dismiss() {
        _isOpen.value = false
    }

    fun toggle(name: String) {
        _filterState.update { state ->
            val updated = if (name in state.enabledSpots) state.enabledSpots - name else state.enabledSpots + name
            state.copy(enabledSpots = updated)
        }
    }

    fun selectAll() {
        _filterState.update { it.copy(enabledSpots = it.allLocations.toSet()) }
    }

    fun deselectAll() {
        _filterState.update { it.copy(enabledSpots = emptySet()) }
    }

    fun save() {
        val state = _filterState.value
        val allEnabled = state.allLocations.isNotEmpty() && state.enabledSpots.containsAll(state.allLocations)
        savedEnabledSpots = if (allEnabled) null else state.enabledSpots.toSet()
        repository.saveEnabledSpots(savedEnabledSpots)
        _hasActiveFilter.value = savedEnabledSpots != null
        _isOpen.value = false
    }
}
