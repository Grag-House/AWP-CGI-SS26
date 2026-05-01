package hka.awp.temi_cgi_app

import android.app.Application
import hka.awp.temi_cgi_app.koin.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

/**
 * Base [Application] class for the Temi CGI application.
 *
 * This class is responsible for global application state and the initialization of
 * the Koin dependency injection framework, providing the [appModule] to the
 * application context.
 */
class TemiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalContext.startKoin {
            androidContext(this@TemiApp)
            modules(appModule)
        }
    }
}