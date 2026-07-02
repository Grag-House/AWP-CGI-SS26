package hka.awp.cgi.temi.app.feature.webserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.data.repository.GeneralConfigRepository
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
 * ViewModel for monitoring the web server status.
 *
 * This ViewModel periodically pings the configured WebView URL to check if the server is reachable.
 *
 * @property generalConfigRepository Repository providing the current URL configuration.
 */
class WebserverViewModel(generalConfigRepository: GeneralConfigRepository) : ViewModel() {
    private val _serverState = MutableStateFlow(ServerState())

    /** Current state of the server (reachability and IP address). */
    val serverState = _serverState.asStateFlow()

    /** Flow of the current WebView URL from settings. */
    val urlState: StateFlow<String> = generalConfigRepository.currentUrl.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
        BuildConfig.WEBVIEW_URL
    )

    companion object {
        /** The interval at which the server status is checked (in milliseconds). */
        private const val POLLING_INTERVALL: Long = 10000

        /** The timeout for the server ping operation (in milliseconds). */
        private const val TIMEOUT: Int = 2000

        /** Time to wait before stopping the flow after the last subscriber disappeared. */
        private const val SUBSCRIPTION_TIMEOUT = 5000L
    }

    private fun startMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // DNS Query
                    val address = InetAddress.getByName(extractHostSafely(urlState.value))
                    val ip = address.hostAddress

                    // Ping
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
