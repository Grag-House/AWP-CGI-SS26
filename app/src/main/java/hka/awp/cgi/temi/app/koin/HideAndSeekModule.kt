package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.hideandseek.HideAndSeekViewModel
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val hideAndSeekModule = module {
    single { HidingSpotRepository(androidContext()) }
    viewModel { HideAndSeekViewModel(robot = get(), hidingSpotRepository = get()) }
}
