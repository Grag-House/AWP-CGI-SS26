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
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.feature.dashboard.MainContent
import hka.awp.cgi.temi.app.feature.hideandseek.HideAndSeekScreen
import hka.awp.cgi.temi.app.feature.hideandseek.HideAndSeekViewModel
import hka.awp.cgi.temi.app.feature.navigation.NavigationContent
import hka.awp.cgi.temi.app.feature.navigation.NavigationViewModel
import hka.awp.cgi.temi.app.feature.settings.SettingsNavigationEvent
import hka.awp.cgi.temi.app.feature.settings.SettingsViewModel
import hka.awp.cgi.temi.app.feature.settings.about.SettingsScreen
import hka.awp.cgi.temi.app.feature.settings.battery.BatteryScreen
import hka.awp.cgi.temi.app.feature.settings.display.DisplayScreen
import hka.awp.cgi.temi.app.feature.settings.notifications.NotificationScreen
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherContent
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherState
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

@Suppress("LongMethod")
@Composable
fun MainShell(
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    navigationViewModel: NavigationViewModel = koinViewModel(),
    webserverViewModel: WebserverViewModel = koinViewModel(),
    weatherViewModel: WeatherViewModel = koinViewModel(),
    hideAndSeekViewModel: HideAndSeekViewModel = koinViewModel()
) {
    val state = observeMainShellState(appViewModel, webserverViewModel, weatherViewModel)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopStatusBar(
                wifiLevel = state.wifiLevel,
                currentTime = state.currentTime,
                batteryLevel = state.batteryLevel,
                isCharging = state.isCharging
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

                RenderContentForRoute(
                    selectedRoute = state.selectedRoute,
                    viewModels = ShellViewModels(
                        settingsViewModel = settingsViewModel,
                        navigationViewModel = navigationViewModel,
                        weatherViewModel = weatherViewModel,
                        hideAndSeekViewModel = hideAndSeekViewModel
                    ),
                    serverState = state.serverState,
                    currentTemperatureState = state.currentTemperatureState,
                    modifier = Modifier.weight(1f),
                    onRouteSelect = { screen -> appViewModel.onRouteSelect(screen) }
                )
            }
        }
    }
}

private data class MainShellState(
    val wifiLevel: Int,
    val currentTime: String,
    val batteryLevel: Int?,
    val isCharging: Boolean,
    val serverState: ServerState,
    val currentTemperatureState: WeatherState,
    val selectedRoute: String,
    val isSidebarExpanded: Boolean
)

@Composable
private fun observeMainShellState(
    appViewModel: AppViewModel,
    webserverViewModel: WebserverViewModel,
    weatherViewModel: WeatherViewModel
): MainShellState {
    val wifiLevel by appViewModel.wifiLevel.collectAsStateWithLifecycle()
    val currentTime by appViewModel.currentTime.collectAsStateWithLifecycle()
    val batteryLevel by appViewModel.batteryLevel.collectAsStateWithLifecycle()
    val isCharging by appViewModel.isCharging.collectAsStateWithLifecycle()
    val serverState by webserverViewModel.serverState.collectAsStateWithLifecycle()
    val currentTemperatureState by weatherViewModel.uiState.collectAsStateWithLifecycle()

    return MainShellState(
        wifiLevel = wifiLevel,
        currentTime = currentTime,
        batteryLevel = batteryLevel,
        isCharging = isCharging,
        serverState = serverState,
        currentTemperatureState = currentTemperatureState,
        selectedRoute = appViewModel.selectedRoute,
        isSidebarExpanded = appViewModel.isSidebarExpanded
    )
}

@Composable
private fun RenderContentForRoute(
    selectedRoute: String,
    viewModels: ShellViewModels,
    serverState: ServerState,
    currentTemperatureState: WeatherState,
    modifier: Modifier = Modifier,
    onRouteSelect: (Screen) -> Unit
) {
    when (selectedRoute) {
        Screen.Dashboard.route -> MainContent(
            modifier = modifier,
            onClick = { screen -> onRouteSelect(screen) },
            serverState = serverState,
            Integer.parseInt(currentTemperatureState.hourlyForecast[0].temp)
        )

        Screen.Webserver.route -> WebViewScreen(BuildConfig.WEBVIEW_URL)

        Screen.Navigation.route -> NavigationContent(
            modifier = modifier,
            viewModel = viewModels.navigationViewModel
        )

        Screen.Settings.route -> SettingsHost(
            settingsViewModel = viewModels.settingsViewModel,
            onNavigate = onRouteSelect,
            modifier = modifier
        )

        Screen.DisplaySettings.route -> DisplayScreen(
            onBackClick = { onRouteSelect(Screen.Settings) }
        )

        Screen.NotificationSettings.route -> NotificationScreen(
            onBackClick = { onRouteSelect(Screen.Settings) }
        )

        Screen.BatterySettings.route -> BatteryScreen(
            onBackClick = { onRouteSelect(Screen.Settings) }
        )

        Screen.Weather.route -> WeatherContent(viewModel = viewModels.weatherViewModel)

        Screen.HideAndSeek.route -> HideAndSeekScreen(
            modifier = modifier,
            viewModel = viewModels.hideAndSeekViewModel,
            onNavigateToDashboard = { onRouteSelect(Screen.Dashboard) }
        )

        Screen.Documentation.route -> WebViewScreen("file:///android_asset/html/index.html")

        else -> MainContent(
            modifier = modifier,
            onClick = { screen -> onRouteSelect(screen) },
            serverState = serverState,
            Integer.parseInt(currentTemperatureState.hourlyForecast[0].temp)
        )
    }
}

private data class ShellViewModels(
    val settingsViewModel: SettingsViewModel,
    val navigationViewModel: NavigationViewModel,
    val weatherViewModel: WeatherViewModel,
    val hideAndSeekViewModel: HideAndSeekViewModel
)

@Composable
private fun SettingsHost(
    settingsViewModel: SettingsViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        settingsViewModel.navigationEvent.collect { event ->
            when (event) {
                is SettingsNavigationEvent.NavigateToDisplay -> onNavigate(Screen.DisplaySettings)
                is SettingsNavigationEvent.NavigateToNotifications -> onNavigate(Screen.NotificationSettings)
                is SettingsNavigationEvent.NavigateToBattery -> onNavigate(Screen.BatterySettings)
            }
        }
    }

    SettingsScreen(modifier = modifier, viewModel = settingsViewModel)
}
