package hka.awp.cgi.temi.app.koin

import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.data.repository.RobotRepository
import hka.awp.cgi.temi.app.feature.controller.BluetoothControllerManager
import hka.awp.cgi.temi.app.feature.controller.ControllerViewModel
import hka.awp.cgi.temi.app.feature.hideandseek.HideAndSeekViewModel
import hka.awp.cgi.temi.app.feature.hideandseek.HidingSpotRepository
import hka.awp.cgi.temi.app.feature.navigation.NavigationViewModel
import hka.awp.cgi.temi.app.feature.patrol.PatrolCameraStreamManager
import hka.awp.cgi.temi.app.feature.patrol.PatrolManager
import hka.awp.cgi.temi.app.feature.patrol.overlay.PatrolOverlayViewModel
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxViewModel
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCameraManager
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxPendingUploadStore
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadQueue
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadRepository
import hka.awp.cgi.temi.app.feature.settings.SettingsViewModel
import hka.awp.cgi.temi.app.feature.settings.adminPanel.AdminPanelViewModel
import hka.awp.cgi.temi.app.feature.settings.battery.BatteryViewModel
import hka.awp.cgi.temi.app.feature.settings.display.DisplayViewModel
import hka.awp.cgi.temi.app.feature.settings.language.LanguageViewModel
import hka.awp.cgi.temi.app.feature.settings.photobox.PhotoboxSettingsViewModel
import hka.awp.cgi.temi.app.feature.webserver.WebserverViewModel
import hka.awp.cgi.temi.app.ui.shell.AppViewModel
import hka.awp.cgi.temi.app.utils.NetworkManager
import hka.awp.cgi.temi.app.utils.TemiBatteryMonitor
import hka.awp.cgi.temi.app.utils.TemiMovementController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Main Koin module for the application.
 */
val appModule = module {
    single<NetworkManager> { NetworkManager(androidContext()) }

    single { RobotRepository() }

    single<Clock> { Clock.systemDefaultZone() }

    single<DateTimeFormatter> {
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    }

    single<Robot?> {
        try {
            Robot.getInstance()
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Timber.e(e, "Temi SDK not available, probably running locally")
            null
        }
    }

    single<TemiBatteryMonitor> {
        TemiBatteryMonitor(
            robot = get(),
            mqttManager = get()
        )
    }

    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    single {
        TemiMovementController(
            robot = get(),
            scope = get(),
        )
    }
    viewModel {
        AppViewModel(
            networkManager = get(),
            clock = get(),
            datetimeFormatter = get(),
            temiBatteryMonitor = get(),
        )
    }

    viewModel {
        SettingsViewModel(get(), robot = get())
    }

    viewModel {
        DisplayViewModel(androidApplication(), get())
    }

    viewModel {
        LanguageViewModel()
    }

    viewModel {
        NavigationViewModel(get(), get(), get(), get())
    }

    viewModel {
        AdminPanelViewModel(
            appConfigRepository = get(),
            mqttManager = get(),
            voiceProfileRepository = get(),
            voiceRecognitionViewModel = get(),
            robot = get(),
            hidingSpotRepository = get(),
            patrolCameraStreamManager = get(),
            patrolManager = get()
        )
    }

    single { HidingSpotRepository(androidContext()) }

    viewModel { HideAndSeekViewModel(robot = get(), hidingSpotRepository = get()) }

    single { PhotoboxCameraManager(androidContext()) }

    single {
        PhotoboxUploadRepository(
            context = androidContext(),
            client = get(),
            appConfigRepository = get()
        )
    }

    single { PhotoboxPendingUploadStore(androidContext()) }

    single { PhotoboxUploadQueue(context = androidContext(), pendingUploadStore = get()) }

    viewModel {
        PhotoboxViewModel(
            cameraManager = get(),
            appConfigRepository = get(),
            uploadRepository = get(),
            uploadQueue = get()
        )
    }

    viewModel { PhotoboxSettingsViewModel(appConfigRepository = get()) }

    viewModel<WebserverViewModel> { WebserverViewModel(get()) }

    viewModel {
        BatteryViewModel(get())
    }

    single {
        BluetoothControllerManager(
            context = androidContext(),
        )
    }

    viewModel {
        ControllerViewModel(
            movementController = get(),
            bluetoothControllerManager = get()
        )
    }

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
