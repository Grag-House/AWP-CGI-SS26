package hka.awp.cgi.temi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import hka.awp.cgi.temi.app.ui.shell.MainShell
import hka.awp.cgi.temi.app.ui.theme.CgiTheme
import hka.awp.cgi.temi.app.utils.hideTopBar

/**
 * The main entry point of the application.
 *
 * This activity is responsible for:
 * - Configuring system UI visibility, such as hiding the status bar for a full-screen experience.
 * - Setting up the Jetpack Compose UI layout within the [CgiTheme].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // this will hide the android topBar and only show if in case the user swipes down
        hideTopBar(window)

        enableEdgeToEdge()

        setContent {
            CgiTheme {
                MainShell()
            }
        }
    }
}
