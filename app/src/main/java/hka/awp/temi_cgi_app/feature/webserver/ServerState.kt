package hka.awp.temi_cgi_app.feature.webserver

data class ServerState(
    val ipAddress: String? = null,
    val isReachable: Boolean = false
)