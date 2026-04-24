package hka.awp.temi_cgi_app.koin

import hka.awp.temi_cgi_app.feature.settings.SettingsViewModel
import hka.awp.temi_cgi_app.ui.shell.AppViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel {
        AppViewModel()
    }
    viewModel {
        SettingsViewModel()
    }
}