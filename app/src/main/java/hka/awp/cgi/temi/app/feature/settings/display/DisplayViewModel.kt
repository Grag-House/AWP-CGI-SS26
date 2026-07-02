package hka.awp.cgi.temi.app.feature.settings.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.data.repository.GeneralConfigRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing and persisting the application's visual theme preferences.
 *
 * It acts as an intermediary layer that reads the initial dark mode preference configuration
 * from the [GeneralConfigRepository] on startup and exposes it reactively to the UI. It also processes
 * user interaction events to toggle the theme state and ensures changes are saved asynchronously.
 *
 * @property repository The data source handler managing persistence layers for the robot's configuration.
 */
class DisplayViewModel(
    private val repository: GeneralConfigRepository
) : ViewModel() {

    /**
     * An observable stream representing whether the dark mode UI theme should be applied.
     */
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    /**
     * Toggles the local theme state and saves the updated configuration choice to the persistent repository storage.
     *
     * @param enabled Set to `true` to activate dark mode, or `false` for light mode.
     */
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleDarkMode(enabled)
        }
    }
}
