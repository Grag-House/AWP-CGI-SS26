package hka.awp.cgi.temi.app.feature.webserver

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import hka.awp.cgi.temi.app.utils.isUrlBlocked

/**
 * A custom [WebViewClient] implementation for the Temi application.
 *
 * This client intercepts URL loading requests within a [WebView] to enforce
 * navigation restrictions by checking requested URLs against a blocklist
 * via [isUrlBlocked].
 */
class TemiWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val requestUrl = request?.url?.toString()
        return isUrlBlocked(requestUrl)
    }
}
