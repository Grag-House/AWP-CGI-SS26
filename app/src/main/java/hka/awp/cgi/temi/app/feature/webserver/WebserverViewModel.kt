package hka.awp.cgi.temi.app.feature.webserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.BuildConfig
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

class WebserverViewModel(webserverRepository: WebserverRepository) : ViewModel() {
    private val _serverState = MutableStateFlow(ServerState())
    val serverState = _serverState.asStateFlow()
    val urlState: StateFlow<String> = webserverRepository.currentUrl.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
        BuildConfig.WEBVIEW_URL
    )

    companion object {
        // The interval at which the server status is checked (in milliseconds)
        private const val POLLING_INTERVALL: Long = 10000

        // The timeout for the server ping operation (in milliseconds)
        private const val TIMEOUT: Int = 2000

        // The time to wait after the last subscriber disappeared before stopping the upstream flow (in milliseconds)
        private const val SUBSCRIPTION_TIMEOUT = 5000L
    }

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
