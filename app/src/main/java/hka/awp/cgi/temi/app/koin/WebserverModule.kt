package hka.awp.cgi.temi.app.koin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import hka.awp.cgi.temi.app.feature.webserver.WebserverViewModel
import hka.awp.cgi.temi.app.utils.AppConfigRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val webserverModule = module {
    single<DataStore<Preferences>> { androidContext().dataStore }

    single<AppConfigRepository> {
        AppConfigRepository(
            context = androidContext(),
            dataStore = get()
        )
    }

    viewModel<WebserverViewModel> { WebserverViewModel(appConfigRepository = get()) }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "webserver_settings")
