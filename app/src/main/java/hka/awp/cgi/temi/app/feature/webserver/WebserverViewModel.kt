package hka.awp.cgi.temi.app.feature.webserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import hka.awp.cgi.temi.app.utils.extractHostSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress

/**
 * Architecture [ViewModel] responsible for monitoring the background web server status
 * and exposing relevant configuration configurations to the UI layer.
 * * It coordinates with the [AppConfigRepository] to stream server verification states,
 * credentials, and target network URLs, while periodically executing lightweight background pings
 * to evaluate server reachability.
 *
 * @param appConfigRepository The central application configuration repository containing the data streams.
 */
class WebserverViewModel(appConfigRepository: AppConfigRepository) : ViewModel() {
    private val _serverState = MutableStateFlow(ServerState())

    /** Public read-only stream representing the current connection details and reachability status of the server. */
    val serverState = _serverState.asStateFlow()

    /** StateFlow emitting the active Webview URL string, mapped and kept active via the UI scope. */
    val urlState: StateFlow<String> = appConfigRepository.webview.currentUrl.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
        BuildConfig.WEBVIEW_URL
    )

    /** StateFlow indicating whether credential verification is actively required to connect to the server. */
    val isVerificationEnabled: StateFlow<Boolean> =
        appConfigRepository.webserver.isWebserverVerificationEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
        false
    )

    /** StateFlow emitting the currently configured unencrypted webserver username string. */
    val webserverUser: StateFlow<String> = appConfigRepository.webserver.webserverUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
        ""
    )

    /** StateFlow emitting the currently configured unencrypted webserver password string. */
    val webserverPassword: StateFlow<String> = appConfigRepository.webserver.webserverPassword.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
        ""
    )

    companion object {
        /** The interval at which the server status is checked (in milliseconds). */
        private const val POLLING_INTERVALL: Long = 10000

        /** The timeout threshold for the server ping/reachability operation (in milliseconds). */
        private const val TIMEOUT: Int = 2000

        /** The time to wait after the last subscriber leaves before pausing the upstream state flows (in milliseconds).
         */
        private const val SUBSCRIPTION_TIMEOUT = 5000L
    }

    /**
     * Spawns a background coroutine bound to [Dispatchers.IO] to continually poll the web server.
     * Extracts the host address, resolves the DNS structure, checks reachability, and publishes updates
     * to [_serverState].
     */
    private fun startMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // DNS Query
                    val address = InetAddress.getByName(extractHostSafely(urlState.value))
                    val ip = address.hostAddress

                    // ping
                    val reachable = address.isReachable(TIMEOUT)

                    if (ip == null) {
                        _serverState.value = ServerState(isReachable = reachable)
                    } else {
                        _serverState.value = ServerState(ipAddress = ip, isReachable = reachable)
                    }
                } catch (_: Exception) {
                    _serverState.value = ServerState()
                }
                delay(POLLING_INTERVALL)
            }
        }
    }

    init {
        Timber.d("hostname: %s", extractHostSafely(urlState.value))
        startMonitoring()
    }
}
