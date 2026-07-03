package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.ui.shell.AppViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin module for the application shell.
 */
val appModule = module {
    viewModel {
        AppViewModel(
            networkManager = get(),
            clock = get(),
            datetimeFormatter = get(),
            temiBatteryMonitor = get(),
        )
    }
}
