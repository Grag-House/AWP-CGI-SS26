package hka.awp.cgi.temi.app.utils

import androidx.core.net.toUri
import hka.awp.cgi.temi.app.BuildConfig
import timber.log.Timber

private const val MISSING_ALLOWED_IP_MESSAGE =
    "HTTP_ALLOWED_IP is missing in BuildConfig"

private val allowedIp: String
    get() = BuildConfig.HTTP_ALLOWED_IP

fun isUrlBlocked(checkUrl: String?): Boolean {
    val uri = checkUrl?.toUri()

    return when {
        uri == null -> true

        allowedIp.isBlank() -> {
            Timber.e(MISSING_ALLOWED_IP_MESSAGE)
            true
        }

        uri.scheme == "http" -> {
            Timber.w("%s was blocked!", checkUrl)
            true
        }

        else -> false
    }
}