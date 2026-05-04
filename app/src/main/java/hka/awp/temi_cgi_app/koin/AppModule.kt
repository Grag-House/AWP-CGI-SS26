package hka.awp.temi_cgi_app.koin

import hka.awp.temi_cgi_app.data.repository.RobotRepository
import hka.awp.temi_cgi_app.feature.settings.SettingsViewModel
import hka.awp.temi_cgi_app.feature.settings.display.DisplayViewModel
import hka.awp.temi_cgi_app.ui.shell.AppViewModel
import hka.awp.temi_cgi_app.utils.NetworkManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin module for the application.
 */
val appModule = module {
    single { NetworkManager(androidContext()) }
    single { RobotRepository() }

    viewModel {
        AppViewModel(networkManager = get())
    }
    viewModel {
        SettingsViewModel(get())
    }
    viewModel {
        DisplayViewModel(get())
    }
}