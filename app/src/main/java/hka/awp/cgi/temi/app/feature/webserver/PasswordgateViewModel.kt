package hka.awp.cgi.temi.app.feature.webserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.feature.webserver.PasswordGateViewModel.Companion.LOCK_TIMEOUT_MS
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages the password-gate state in front of the WebView screen.
 *
 * The correct password is read from [AppConfigRepository] — the same repository
 * used by [WebserverViewModel] for the server URL — so it can be updated at
 * runtime without redeploying the app.
 *
 * Authentication is granted when the entered password matches the stored value.
 * It is automatically revoked after [LOCK_TIMEOUT_MS] milliseconds of the screen
 * being in the background (i.e. after the user navigates away and returns).
 */
class PasswordGateViewModel(private val appConfigRepository: AppConfigRepository) : ViewModel() {

    companion object {
        private const val LOCK_TIMEOUT_MS: Long = 10 * 60 * 1000L
    }

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    /** Running countdown job that will lock the screen after the timeout. */
    private var lockJob: Job? = null

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Validates [input] using the same hash-based check as [AdminPanelViewModel]:
     * reads the stored hash from [AppConfigRepository] and delegates to
     * [AppConfigRepository.isValidPassword], so the logic is never duplicated.
     */
    fun submitPassword(input: String) {
        viewModelScope.launch {
            val currentHash = appConfigRepository.webserverPasswordHash.first()
            val isValid = appConfigRepository.isValidPassword(input, currentHash)
            if (isValid) {
                _isAuthenticated.value = true
                _errorMessage.value = null
                cancelLockTimer()
            } else {
                _errorMessage.value = "Falsches Passwort"
            }
        }
    }

    /** Clear any transient error shown in the UI. */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Called when the WebView screen becomes invisible (paused / stopped).
     * Starts the [LOCK_TIMEOUT_MS] countdown; if the user does not return in time
     * the session is invalidated.
     */
    fun onScreenHidden() {
        if (!_isAuthenticated.value) return
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            delay(LOCK_TIMEOUT_MS)
            _isAuthenticated.value = false
        }
    }

    /**
     * Called when the WebView screen becomes visible again (resumed).
     * Cancels the pending lock timer so the session is preserved.
     */
    fun onScreenVisible() {
        cancelLockTimer()
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun cancelLockTimer() {
        lockJob?.cancel()
        lockJob = null
    }
}
