package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.weatherscreen.GeocoderLocationNameResolver
import hka.awp.cgi.temi.app.feature.weatherscreen.LocationNameResolver
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherRepository
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin Module for weather related dependencies **/
val weatherModule = module {
    single<LocationNameResolver> { GeocoderLocationNameResolver(context = androidContext()) }

    single<WeatherRepository> {
        WeatherRepository(
            client = get(),
            hourlyFormatter = get(),
            locationNameResolver = get()
        )
    }

    viewModel<WeatherViewModel> {
        WeatherViewModel(
            repository = get(),
            appConfigRepository = get(),
            clock = get()
        )
    }
}
