package hka.awp.temi_cgi_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import hka.awp.temi_cgi_app.koin.appModule
import hka.awp.temi_cgi_app.ui.shell.MainShell
import hka.awp.temi_cgi_app.ui.theme.CgiTheme
import hka.awp.temi_cgi_app.utils.hideTopBar
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import hka.awp.temi_cgi_app.temi.TemiStatusService
import org.koin.android.ext.android.inject

/**
 * The main entry point of the application.
 *
 * This activity is responsible for:
 * - Initializing the Koin dependency injection framework with the application context and modules.
 * - Configuring system UI visibility, such as hiding the status bar for a full-screen experience.
 * - Setting up the Jetpack Compose UI layout within the [CgiTheme].
 */
class MainActivity : ComponentActivity() {

    private val temiStatusService: TemiStatusService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        temiStatusService.start()

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
