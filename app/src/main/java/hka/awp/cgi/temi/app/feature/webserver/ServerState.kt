package hka.awp.cgi.temi.app.feature.webserver

/**
 * Represents the current state of the web server, including its network identity
 * and connectivity status.
 *
 * @property ipAddress The IP address of the server, or null if the address has not been determined.
 */
data class ServerState(val ipAddress: String? = null, val isReachable: Boolean = false)
