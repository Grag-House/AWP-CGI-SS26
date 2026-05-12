package hka.awp.cgi.temi.app.feature.webserver

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.cgi.temi.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress

class WebserverViewModel : ViewModel() {
    private val _serverState = MutableStateFlow(ServerState())
    val serverState = _serverState.asStateFlow()
    private val hostname = BuildConfig.WEBVIEW_URL.toUri().host

    companion object {
        private const val POLLING_INTERVALL: Long = 10000

        private const val TIMEOUT: Int = 2000
    }

    private fun startMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // DNS Query
                    val address = InetAddress.getByName(hostname)
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
        Timber.d("hostname: $hostname")
        startMonitoring()
    }
}
