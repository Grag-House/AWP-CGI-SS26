package hka.awp.temi_cgi_app.feature.webserver

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hka.awp.temi_cgi_app.BuildConfig
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

    private fun startMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // DNS Query
                    val address = InetAddress.getByName(hostname)
                    val ip = address.hostAddress ?: "Unbekannte Host Adresse!"

                    // ping
                    val reachable = address.isReachable(2000)

                    _serverState.value = ServerState(ip, reachable)
                } catch (_: Exception) {
                    _serverState.value = ServerState()
                }
                delay(10000) // Alle 10 Sekunden prüfen
            }
        }
    }

    init {
        Timber.d("hostname: $hostname")
        startMonitoring()
    }
}
