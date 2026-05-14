package hka.awp.cgi.temi.app.feature.webserver

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import hka.awp.cgi.temi.app.utils.isUrlBlocked

class TemiWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val requestUrl = request?.url?.toString()
        return isUrlBlocked(requestUrl)
    }
}
