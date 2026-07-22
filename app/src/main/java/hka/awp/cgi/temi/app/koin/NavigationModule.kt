package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.navigation.NavigationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for navigation-related dependencies.
 */
val navigationModule = module {
    viewModel {
        NavigationViewModel(
            robot = get(),
            mqttManager = get(),
            temiVoiceListener = get()
        )
    }
}
