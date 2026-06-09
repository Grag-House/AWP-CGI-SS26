package hka.awp.cgi.temi.app.utils

import androidx.core.net.toUri
import hka.awp.cgi.temi.app.BuildConfig
import timber.log.Timber
import java.net.URI

const val ALLOWED_IP = BuildConfig.HTTP_ALLOWED_IP

/**
 * Checks if a given URL is blocked based on its scheme and host.
 * - If the URL is null, it is considered blocked.
 * - If the host of the URL matches the allowed IP, it is not blocked.
 * - If the URL uses the "http" scheme, it is considered blocked.
 * - All other URLs are not blocked.
 *
 * @param checkUrl The URL to check for blocking.
 * @return True if the URL is blocked, false otherwise.
 */
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

/**
 * Check for the host of a URL and return it. If the URL is malformed, return the input string itself.
 */
fun extractHostSafely(input: String): String {
    val trimmedInput = input.trim()

    val urlToParse = if (!trimmedInput.startsWith("http://") && !trimmedInput.startsWith("https://")) {
        "https://$trimmedInput"
    } else {
        trimmedInput
    }

    return runCatching { URI(urlToParse).host }.getOrNull() ?: trimmedInput
}
