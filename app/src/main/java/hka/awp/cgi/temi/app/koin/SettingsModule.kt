package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.settings.SettingsViewModel
import hka.awp.cgi.temi.app.feature.settings.adminPanel.AdminPanelViewModel
import hka.awp.cgi.temi.app.feature.settings.battery.BatteryViewModel
import hka.awp.cgi.temi.app.feature.settings.display.DisplayViewModel
import hka.awp.cgi.temi.app.feature.settings.language.LanguageViewModel
import hka.awp.cgi.temi.app.feature.settings.photobox.PhotoboxSettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get(), robot = get()) }
    viewModel { DisplayViewModel(androidApplication(), get()) }
    viewModel { LanguageViewModel() }
    viewModel {
        AdminPanelViewModel(
            appConfigRepository = get(),
            mqttManager = get(),
            voiceProfileRepository = get(),
            voiceRecognitionViewModel = get(),
            robot = get(),
            hidingSpotRepository = get(),
            patrolCameraStreamManager = get(),
            patrolManager = get()
        )
    }
    viewModel { BatteryViewModel(get()) }
    viewModel { PhotoboxSettingsViewModel(appConfigRepository = get()) }
}
