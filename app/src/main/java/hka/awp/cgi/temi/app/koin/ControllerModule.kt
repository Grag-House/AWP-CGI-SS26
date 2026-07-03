package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.controller.BluetoothControllerManager
import hka.awp.cgi.temi.app.feature.controller.ControllerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val controllerModule = module {
    single { BluetoothControllerManager(context = androidContext()) }
    viewModel {
        ControllerViewModel(
            movementController = get(),
            bluetoothControllerManager = get()
        )
    }
}
