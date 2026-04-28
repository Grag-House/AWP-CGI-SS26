package hka.awp.temi_cgi_app.temi

import kotlinx.coroutines.flow.StateFlow

interface TemiStatusService {
    val batteryLevel: StateFlow<Int?>
    val isCharging: StateFlow<Boolean>
    val isTemiAvailable: StateFlow<Boolean>

    fun start()
    fun stop()
}