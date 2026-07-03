package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.settings.SettingsViewModel
import hka.awp.cgi.temi.app.feature.settings.adminPanel.AdminPanelViewModel
import hka.awp.cgi.temi.app.feature.settings.battery.BatteryViewModel
import hka.awp.cgi.temi.app.feature.settings.display.DisplayViewModel
import hka.awp.cgi.temi.app.feature.settings.language.LanguageViewModel
import hka.awp.cgi.temi.app.feature.settings.photobox.PhotoboxSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for settings-related view models.
 */
val settingsModule = module {
    viewModel { SettingsViewModel(get(), robot = get()) }
    viewModel { DisplayViewModel(get()) }
    viewModel { LanguageViewModel(get()) }
    viewModel {
        AdminPanelViewModel(
            generalConfigRepository = get(),
            patrolConfigRepository = get(),
            securityConfigRepository = get(),
            mqttManager = get(),
            voiceProfileRepository = get(),
            voiceRecognitionViewModel = get(),
            robot = get(),
            hidingSpotRepository = get(),
            patrolManager = get(),
            patrolCameraStreamManager = get()
        )
    }
    viewModel { BatteryViewModel(get()) }
    viewModel { PhotoboxSettingsViewModel(get()) }
}
