package hka.awp.temi_cgi_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import hka.awp.temi_cgi_app.feature.dashboard.TemiDashboardScreen
import hka.awp.temi_cgi_app.koin.appModule
import hka.awp.temi_cgi_app.ui.shell.SidebarViewModel
import hka.awp.temi_cgi_app.ui.theme.CgiTheme
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.context.startKoin


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //TODO use timber for logging instead of default android logging later on

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        enableEdgeToEdge()
        setContent {
            CgiTheme {
                TemiDashboardScreen(viewModel = getViewModel<SidebarViewModel>())
            }
        }
    }
}
