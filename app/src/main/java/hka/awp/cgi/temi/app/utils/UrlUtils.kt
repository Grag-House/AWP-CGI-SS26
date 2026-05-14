package hka.awp.cgi.temi.app.utils

import androidx.core.net.toUri
import hka.awp.cgi.temi.app.BuildConfig
import timber.log.Timber

private const val MISSING_ALLOWED_IP_MESSAGE =
    "HTTP_ALLOWED_IP is missing in BuildConfig"

private val allowedIp: String
    get() = BuildConfig.HTTP_ALLOWED_IP

fun isUrlBlocked(checkUrl: String?): Boolean {
    if (checkUrl == null) return true

    if (allowedIp.isBlank()) {
        Timber.e(MISSING_ALLOWED_IP_MESSAGE)
        return true
    }

    val uri = checkUrl.toUri()
    val host = uri.host

    if (host == allowedIp) {
        return false
    }

    if (uri.scheme == "http") {
        Timber.w("$checkUrl was blocked!")
        return true
    }

    return false
}
