package hka.awp.cgi.temi.app

import android.app.Application
import android.util.Log
import hka.awp.cgi.temi.app.koin.appModule
import hka.awp.cgi.temi.app.koin.navigationModule
import hka.awp.cgi.temi.app.koin.temiVoiceRecognitionModule
import hka.awp.cgi.temi.app.koin.weatherModule
import hka.awp.cgi.temi.app.koin.webserverModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import timber.log.Timber

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
            modules(appModule, weatherModule, navigationModule, webserverModule, temiVoiceRecognitionModule)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Timber initialised: Debug-Logging is enabled!")
            val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

            Thread.setDefaultUncaughtExceptionHandler { thread, e ->
                // Use timber to log all uncaught exceptions
                Timber.e(e, "App crashed in thread: %s", thread.name)

                // return to the old handler
                oldHandler?.uncaughtException(thread, e)
            }
        } else {
            Timber.plant(object : Timber.DebugTree() {
                override fun isLoggable(tag: String?, priority: Int): Boolean {
                    // only log error or higher
                    return priority >= Log.ERROR
                }
            })
        }
    }
}
