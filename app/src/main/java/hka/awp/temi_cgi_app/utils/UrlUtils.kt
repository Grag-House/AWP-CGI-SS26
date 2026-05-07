package hka.awp.temi_cgi_app.utils

import androidx.core.net.toUri
import hka.awp.temi_cgi_app.BuildConfig
import timber.log.Timber

const val allowedIP = BuildConfig.HTTP_ALLOWED_IP

fun isUrlBlocked(checkUrl: String?): Boolean {

    if (checkUrl == null) return true

    val uri = checkUrl.toUri()
    val host = uri.host

    if (host == allowedIP) {
        return false
    }

    if (uri.scheme == "http") {
        Timber.w("$checkUrl was blocked!")
        return true
    }

    return false
}