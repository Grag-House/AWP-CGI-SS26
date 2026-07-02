package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.patrol.PatrolCameraStreamManager
import hka.awp.cgi.temi.app.feature.patrol.PatrolManager
import hka.awp.cgi.temi.app.feature.patrol.overlay.PatrolOverlayViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val patrolModule = module {
    single {
        val ip = BuildConfig.SERVER_IP
        val port = BuildConfig.SERVER_PORT
        PatrolCameraStreamManager(
            context = androidContext(),
            serverUrl = "ws://$ip:$port"
        )
    }

    single {
        PatrolManager(
            robot = get(),
            cameraStreamManager = get(),
            mqttManager = get()
        )
    }

    viewModel {
        PatrolOverlayViewModel(
            patrolManager = get(),
            patrolCameraStreamManager = get()
        )
    }
}
