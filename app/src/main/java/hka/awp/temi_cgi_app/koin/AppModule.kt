package hka.awp.temi_cgi_app.koin

import com.robotemi.sdk.Robot
import hka.awp.temi_cgi_app.feature.navigation.NavigationViewModel
import hka.awp.temi_cgi_app.feature.settings.SettingsViewModel
import hka.awp.temi_cgi_app.feature.webserver.WebserverViewModel
import hka.awp.temi_cgi_app.ui.shell.AppViewModel
import hka.awp.temi_cgi_app.utils.NetworkManager
import hka.awp.temi_cgi_app.utils.TemiBatteryMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Main Koin module for the application.
 */
val appModule = module {
    single<NetworkManager> { NetworkManager(androidContext()) }

    single<Clock> { Clock.systemDefaultZone() }

    single<DateTimeFormatter> {
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    }

    single<Robot?> {
        try {
            Robot.getInstance()
        } catch (e: Exception) {
            Timber.e(e, "Temi SDK not available, probably running locally")
            null
        }
    }

    single<TemiBatteryMonitor> { TemiBatteryMonitor(robot = get()) }

    viewModel<AppViewModel> {
        AppViewModel(
            networkManager = get(),
            clock = get(),
            datetimeFormatter = get(),
            temiBatteryMonitor = get(),
        )
    }

    viewModel<SettingsViewModel> {
        SettingsViewModel()
    }

    viewModel<NavigationViewModel> { NavigationViewModel() }

    viewModel<WebserverViewModel> { WebserverViewModel() }
}
