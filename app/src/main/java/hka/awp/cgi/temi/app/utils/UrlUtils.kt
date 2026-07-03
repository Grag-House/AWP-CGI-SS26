package hka.awp.cgi.temi.app.utils

import androidx.core.net.toUri
import hka.awp.cgi.temi.app.BuildConfig
import timber.log.Timber
import java.net.URI

/**
 * Utility functions for URL validation and manipulation.
 */

/** The IP address allowed for HTTP communication even if other HTTP URLs are blocked. */
const val ALLOWED_IP = BuildConfig.HTTP_ALLOWED_IP

/**
 * Checks if a given URL should be blocked based on security policies.
 *
 * Policies:
 * - Null URLs are blocked.
 * - URLs matching [ALLOWED_IP] are permitted.
 * - Plain "http" URLs (other than the allowed IP) are blocked to enforce HTTPS.
 *
 * @param checkUrl The URL string to validate.
 * @return True if the URL is blocked, false if it is allowed.
 */
fun isUrlBlocked(checkUrl: String?): Boolean {
    if (checkUrl == null) return true

    val uri = try {
        checkUrl.toUri()
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Timber.e(e, "Failed to parse URL: %s", checkUrl)
        return true
    }

    val host = uri.host
    val scheme = uri.scheme

    return when {
        // Explicitly allow communication with the internal/configured server IP
        host == ALLOWED_IP -> false
        // Block non-secure HTTP connections
        scheme == "http" -> {
            Timber.w("URL blocked due to non-secure scheme: %s", checkUrl)
            true
        }
        else -> false
    }
}

/**
 * Safely extracts the host from a potentially malformed URL string.
 * If the string doesn't have a scheme, "https://" is prefixed for parsing.
 *
 * @param input The raw input string.
 * @return The extracted host name, or the original input if parsing fails.
 */
fun extractHostSafely(input: String): String {
    val trimmedInput = input.trim()
    if (trimmedInput.isEmpty()) return ""

    val urlToParse = if (!trimmedInput.startsWith("http://") && !trimmedInput.startsWith("https://")) {
        "https://$trimmedInput"
    } else {
        trimmedInput
    }

    return runCatching { URI(urlToParse).host }
        .onFailure { Timber.e(it, "Could not parse host from: %s", input) }
        .getOrNull() ?: trimmedInput
}
