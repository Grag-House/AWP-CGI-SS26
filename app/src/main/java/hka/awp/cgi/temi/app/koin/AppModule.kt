package hka.awp.cgi.temi.app.koin

import android.app.Application
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.data.repository.RobotRepository
import hka.awp.cgi.temi.app.feature.settings.SettingsViewModel
import hka.awp.cgi.temi.app.feature.settings.adminPanel.AdminPanelViewModel
import hka.awp.cgi.temi.app.feature.settings.battery.BatteryViewModel
import hka.awp.cgi.temi.app.feature.settings.display.DisplayViewModel
import hka.awp.cgi.temi.app.feature.settings.notifications.NotificationViewModel
import hka.awp.cgi.temi.app.ui.shell.AppViewModel
import hka.awp.cgi.temi.app.utils.NetworkManager
import hka.awp.cgi.temi.app.utils.TemiBatteryMonitor
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
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            Timber.e(e, "Temi SDK not available, probably running locally")
            null
        }
    }

    single<TemiBatteryMonitor> { TemiBatteryMonitor(robot = get()) }

    viewModel<AppViewModel> {
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
        DisplayViewModel(
            application = androidContext() as Application,
            get()
        )
    }
    viewModel {
        NotificationViewModel(
            application = androidContext() as Application,
            get()
        )
    }
    viewModel { AdminPanelViewModel() }

    viewModel {
        BatteryViewModel(get())
    }
}
