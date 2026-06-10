package hka.awp.cgi.temi.app.ui.shell

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.feature.controller.ControllerScreen
import hka.awp.cgi.temi.app.feature.dashboard.MainContent
import hka.awp.cgi.temi.app.feature.navigation.NavigationContent
import hka.awp.cgi.temi.app.feature.navigation.NavigationViewModel
import hka.awp.cgi.temi.app.feature.settings.SettingsNavigationEvent
import hka.awp.cgi.temi.app.feature.settings.SettingsViewModel
import hka.awp.cgi.temi.app.feature.settings.about.SettingsScreen
import hka.awp.cgi.temi.app.feature.settings.battery.BatteryScreen
import hka.awp.cgi.temi.app.feature.settings.display.DisplayScreen
import hka.awp.cgi.temi.app.feature.settings.language.LanguageScreen
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherContent
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherViewModel
import hka.awp.cgi.temi.app.feature.webserver.ServerState
import hka.awp.cgi.temi.app.feature.webserver.WebViewScreen
import hka.awp.cgi.temi.app.feature.webserver.WebserverViewModel
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber

/**
 * The primary UI shell of the application.
 *
 * This component acts as the root container, managing the top-level layout
 * which includes the [TopStatusBar], the [Sidebar] for navigation,
 * and the main content area that switches between different screens
 * based on the current route.
 *
 * @param appViewModel The global ViewModel managing the app's state,
 * navigation routes, and sidebar visibility.
 * @param settingsViewModel The ViewModel handling logic and interactions
 * specific to the settings screen.
 */

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MainShell(
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    navigationViewModel: NavigationViewModel = koinViewModel(),
    webserverViewModel: WebserverViewModel = koinViewModel(),
    weatherViewModel: WeatherViewModel = koinViewModel()
) {
    val wifiLevel by appViewModel.wifiLevel.collectAsStateWithLifecycle()
    val currentTime by appViewModel.currentTime.collectAsStateWithLifecycle()
    val batteryLevel by appViewModel.batteryLevel.collectAsStateWithLifecycle()
    val isCharging by appViewModel.isCharging.collectAsStateWithLifecycle()
    val serverState by webserverViewModel.serverState.collectAsStateWithLifecycle()
    val currentTemperatureState by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val currentTemperature = currentTemperatureState.hourlyForecast
        .firstOrNull()
        ?.temp
        ?.toIntOrNull() ?: 0
    val webserverUrlState by webserverViewModel.urlState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopStatusBar(
                wifiLevel = wifiLevel,
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                isCharging = isCharging
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Sidebar(
                isExpanded = appViewModel.isSidebarExpanded,
                selectedRoute = appViewModel.selectedRoute,
                onRouteSelected = { screen -> appViewModel.onRouteSelect(screen) },
                onSidebarToggle = { appViewModel.onSideBarToggle() },
                modifier = Modifier.width(260.dp)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Timber.d("Selected route: %s", appViewModel.selectedRoute)

                ShellRouteContent(
                    selectedRoute = appViewModel.selectedRoute,
                    onRouteSelect = appViewModel::onRouteSelect,
                    settingsViewModel = settingsViewModel,
                    navigationViewModel = navigationViewModel,
                    weatherViewModel = weatherViewModel,
                    serverState = serverState,
                    currentTemperature = currentTemperature,
                    webserverUrl = webserverUrlState,
                )
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod", "LongParameterList")
@Composable
private fun ShellRouteContent(
    selectedRoute: String,
    onRouteSelect: (Screen) -> Unit,
    settingsViewModel: SettingsViewModel,
    navigationViewModel: NavigationViewModel,
    weatherViewModel: WeatherViewModel,
    serverState: ServerState,
    currentTemperature: Int,
    webserverUrl: String,
) {
    when (selectedRoute) {
        Screen.Dashboard.route -> MainContent(
            modifier = Modifier.fillMaxSize(),
            onClick = onRouteSelect,
            serverState = serverState,
            currentTemperature,
        )

        Screen.Webserver.route -> WebViewScreen(webserverUrl)

        Screen.Navigation.route -> NavigationContent(
            modifier = Modifier.fillMaxSize(),
            viewModel = navigationViewModel,
        )

        Screen.Controller.route -> ControllerScreen(
            modifier = Modifier.fillMaxSize(),
        )

        Screen.Settings.route -> SettingsRouteContent(
            settingsViewModel = settingsViewModel,
            onRouteSelect = onRouteSelect,
        )

        Screen.DisplaySettings.route -> DisplayScreen(
            onBackClick = {
                onRouteSelect(Screen.Settings)
            },
        )

        Screen.BatterySettings.route -> BatteryScreen(
            onBackClick = {
                onRouteSelect(Screen.Settings)
            },
        )

        Screen.Weather.route -> WeatherContent(
            viewModel = weatherViewModel,
        )

        Screen.Documentation.route -> {
            WebViewScreen("file:///android_asset/html/index.html")
        }

        else -> MainContent(
            modifier = Modifier.fillMaxSize(),
            onClick = onRouteSelect,
            serverState = serverState,
            currentTemperature,
        )
    }
}

@Composable
private fun SettingsRouteContent(
    settingsViewModel: SettingsViewModel,
    onRouteSelect: (Screen) -> Unit,
) {
    LaunchedEffect(Unit) {
        settingsViewModel.navigationEvent.collect { event ->
            when (event) {
                is SettingsNavigationEvent.NavigateToDisplay ->
                    onRouteSelect(Screen.DisplaySettings)

                is SettingsNavigationEvent.NavigateToNotifications ->
                    onRouteSelect(Screen.NotificationSettings)

                is SettingsNavigationEvent.NavigateToBattery ->
                    onRouteSelect(Screen.BatterySettings)
            }
        }
    }

    SettingsScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = settingsViewModel,
    )
}
