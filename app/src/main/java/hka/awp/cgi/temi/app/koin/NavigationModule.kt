package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.navigation.NavigationViewModel
import org.koin.android.ext.koin.androidApplication
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
            defaultMapName = androidApplication().getString(R.string.default_map_name),
            temiVoiceListener = get()
        )
    }
}
