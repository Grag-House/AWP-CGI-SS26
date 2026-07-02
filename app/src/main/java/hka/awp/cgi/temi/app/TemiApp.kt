package hka.awp.cgi.temi.app

import android.app.Application
import android.util.Log
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadQueue
import hka.awp.cgi.temi.app.koin.appModule
import hka.awp.cgi.temi.app.koin.controllerModule
import hka.awp.cgi.temi.app.koin.coreModule
import hka.awp.cgi.temi.app.koin.hideAndSeekModule
import hka.awp.cgi.temi.app.koin.navigationModule
import hka.awp.cgi.temi.app.koin.patrolModule
import hka.awp.cgi.temi.app.koin.photoboxModule
import hka.awp.cgi.temi.app.koin.settingsModule
import hka.awp.cgi.temi.app.koin.voiceRecognitionModule
import hka.awp.cgi.temi.app.koin.weatherModule
import hka.awp.cgi.temi.app.koin.webserverModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import timber.log.Timber

/**
 * Base [Application] class for the Temi CGI application.
 *
 * This class is responsible for global application state and the initialization of
 * the Koin dependency injection framework.
 */
class TemiApp : Application() {
    override fun onCreate() {
        super.onCreate()

        GlobalContext.startKoin {
            androidContext(this@TemiApp)
            modules(
                coreModule,
                appModule,
                weatherModule,
                navigationModule,
                webserverModule,
                voiceRecognitionModule,
                settingsModule,
                photoboxModule,
                patrolModule,
                hideAndSeekModule,
                controllerModule
            )
        }

        // Resumes any Photobox uploads that were still cached on disk when the process last
        // died (e.g. app killed mid-retry) — without this they'd sit on disk forever since
        // nothing else triggers a re-enqueue.
        GlobalContext.get().get<PhotoboxUploadQueue>().reconcileOrphans()

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
