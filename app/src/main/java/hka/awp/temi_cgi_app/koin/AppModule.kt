package hka.awp.temi_cgi_app.koin

import hka.awp.temi_cgi_app.feature.settings.SettingsViewModel
import hka.awp.temi_cgi_app.ui.shell.AppViewModel
import hka.awp.temi_cgi_app.utils.NetworkManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
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

    viewModel {
        AppViewModel(networkManager = get(), clock = get(), datetimeFormatter = get())
    }

    viewModel {
        SettingsViewModel()
    }
}