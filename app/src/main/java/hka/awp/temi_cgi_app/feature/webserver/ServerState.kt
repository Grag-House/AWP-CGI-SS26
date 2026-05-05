package hka.awp.temi_cgi_app.feature.webserver

data class ServerState(
    val ipAddress: String = "xxx.xxx.xxx.xxx",
    val isReachable: Boolean = false
)