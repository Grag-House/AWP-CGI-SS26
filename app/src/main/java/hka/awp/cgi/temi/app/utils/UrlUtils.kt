package hka.awp.cgi.temi.app.utils

import androidx.core.net.toUri
import hka.awp.cgi.temi.app.BuildConfig
import timber.log.Timber

const val ALLOWED_IP = BuildConfig.HTTP_ALLOWED_IP

fun isUrlBlocked(checkUrl: String?): Boolean {
    if (checkUrl == null) return true

    val uri = checkUrl.toUri()
    val host = uri.host

    if (host == ALLOWED_IP) {
        return false
    }

    if (uri.scheme == "http") {
        Timber.w("%s was blocked", checkUrl)
        return true
    }

    return false
}
