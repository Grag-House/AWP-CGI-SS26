package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.webserver.WebserverViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module definition for the Webserver feature.
 */
val webserverModule = module {
    single<DataStore<Preferences>> { androidContext().dataStore }

    single<AppConfigRepository> {
        AppConfigRepository(
            context = androidContext(),
            dataStore = get()
        )
    }

    viewModel<WebserverViewModel> { WebserverViewModel(appConfigRepository = get()) }
    viewModel<WebserverViewModel> { WebserverViewModel(generalConfigRepository = get()) }
}
