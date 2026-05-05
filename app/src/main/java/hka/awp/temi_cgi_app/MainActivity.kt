package hka.awp.temi_cgi_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import hka.awp.temi_cgi_app.feature.settings.display.DisplayViewModel
import hka.awp.temi_cgi_app.koin.appModule
import hka.awp.temi_cgi_app.ui.shell.MainShell
import hka.awp.temi_cgi_app.ui.theme.CgiTheme
import hka.awp.temi_cgi_app.utils.hideTopBar
import org.koin.android.ext.koin.androidContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin


/**
 * The main entry point of the application.
 *
 * This activity is responsible for:
 * - Configuring system UI visibility, such as hiding the status bar for a full-screen experience.
 * - Setting up the Jetpack Compose UI layout within the [CgiTheme].
 */
class MainActivity : ComponentActivity() {
    //TODO Activity oder Application?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startKoin {
            androidContext(this@MainActivity.application)
            modules(appModule)
        }

        // this will hide the android topBar and only show if in case the user swipes down
        hideTopBar(window)

        enableEdgeToEdge()

        setContent {
            val displayViewModel: DisplayViewModel = koinViewModel()
            val isDarkMode by displayViewModel.isDarkMode.collectAsState()
            CgiTheme(darkTheme = isDarkMode) {
                MainShell()
            }
        }
    }
}
