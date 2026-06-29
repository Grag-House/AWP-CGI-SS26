package hka.awp.cgi.temi.app.feature.settings.display

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.data.repository.RobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing and persisting the application's visual theme preferences.
 *
 * It acts as an intermediary layer that reads the initial dark mode preference configuration
 * from the [RobotRepository] on startup and exposes it reactively to the UI. It also processes
 * user interaction events to toggle the theme state and ensures changes are saved asynchronously.
 *
 * @property context The application-level context required for reading and writing preferences.
 * @property repository The data source handler managing persistence layers for the robot's configuration.
 */
class DisplayViewModel(
    private val context: Application,
    private val repository: RobotRepository
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)

    /**
     * An observable stream representing whether the dark mode UI theme should be applied.
     */
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        viewModelScope.launch {
            _isDarkMode.value = repository.getDarkMode(context)
        }
    }

    /**
     * Toggles the local theme state and saves the updated configuration choice to the persistent repository storage.
     *
     * @param enabled Set to `true` to activate dark mode, or `false` for light mode.
     */
    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        repository.saveDarkMode(enabled, context)
    }
}
