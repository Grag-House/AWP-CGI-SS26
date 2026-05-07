package hka.awp.temi_cgi_app.feature.webserver

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import hka.awp.temi_cgi_app.utils.isUrlBlocked

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
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient = TemiWebViewClient()
            settings.domStorageEnabled = true

            if (!isUrlBlocked(url)) {
                loadUrl(url)
            } else {
                loadDataWithBaseURL(
                    null, styledError, "text/html", "UTF-8", null
                )
            }
        }
    }

    AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
}

private const val styledError = """
    <!DOCTYPE html>
    <html lang="de">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            /* Grundlegende CSS-Zurücksetzung und volle Höhe */
            html, body {
                height: 100%;
                margin: 0;
                padding: 0;
            }

            body { 
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; 
                display: flex; 
                align-items: center; /* Vertikale Zentrierung */
                justify-content: center; /* Horizontale Zentrierung */
                height: 100%; /* Nutzt die dynamische Viewport-Höhe */
                background-color: #f8f9fa; 
                color: #343a40; 
            }

            .card {
                background: white;
                padding: 2.5rem;
                border-radius: 16px;
                box-shadow: 0 10px 30px rgba(0,0,0,0.08);
                text-align: center;
                width: 85%;
                max-width: 450px;
            }

            h1 { color: #dc3545; font-size: 1.6rem; margin-bottom: 1rem; }
            p { line-height: 1.5; color: #6c757d; font-size: 1.1rem; }
        </style>
    </head>
    <body>
        <div class="card">
            <h1>Sicherheitsblockade</h1>
            <p>Der Zugriff auf die angeforderte Seite wurde aus Sicherheitsgründen verweigert!</p>
        </div>
    </body>
    </html>
"""

