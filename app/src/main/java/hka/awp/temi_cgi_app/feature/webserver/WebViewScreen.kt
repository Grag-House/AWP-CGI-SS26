package hka.awp.temi_cgi_app.feature.webserver

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A composable function that displays a web page within the application using an [AndroidView]
 * wrapping a standard [WebView].
 *
 * @param url The string URL of the web page to be loaded and displayed.
 */
@Composable
fun WebViewScreen(url: String) {
    val context = LocalContext.current
    val view = remember {
        WebView(context).apply {
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            //TODO check if needed (potential securtiy issue)
//          settings.javaScriptEnabled = true
            loadUrl(url)
        }
    }

    AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
}