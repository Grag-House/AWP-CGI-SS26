package hka.awp.temi_cgi_app

import android.app.Application
import hka.awp.temi_cgi_app.koin.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class TemiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalContext.startKoin {
            androidContext(this@TemiApp)
            modules(appModule)
        }
    }
}