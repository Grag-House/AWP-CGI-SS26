package hka.awp.cgi.temi.app.ui.shell

import androidx.compose.foundation.layout.Box
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
import hka.awp.cgi.temi.app.feature.hideandseek.HideAndSeekScreen
import hka.awp.cgi.temi.app.feature.hideandseek.HideAndSeekViewModel
import hka.awp.cgi.temi.app.feature.navigation.NavigationContent
import hka.awp.cgi.temi.app.feature.navigation.NavigationViewModel
import hka.awp.cgi.temi.app.feature.settings.SettingsNavigationEvent
import hka.awp.cgi.temi.app.feature.settings.SettingsViewModel
import hka.awp.cgi.temi.app.feature.settings.about.SettingsScreen
import hka.awp.cgi.temi.app.feature.settings.adminPanel.AdminPanelScreen
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolOverlayViewModel
import hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol.PatrolStreamOverlay
import hka.awp.cgi.temi.app.feature.settings.battery.BatteryScreen
import hka.awp.cgi.temi.app.feature.settings.display.DisplayScreen
import hka.awp.cgi.temi.app.feature.settings.language.LanguageScreen
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

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MainShell(
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    navigationViewModel: NavigationViewModel = koinViewModel(),
    webserverViewModel: WebserverViewModel = koinViewModel(),
    weatherViewModel: WeatherViewModel = koinViewModel(),
    hideAndSeekViewModel: HideAndSeekViewModel = koinViewModel(),
    patrolOverlayViewModel: PatrolOverlayViewModel = koinViewModel()
) {
    val wifiLevel by appViewModel.wifiLevel.collectAsStateWithLifecycle()
    val currentTime by appViewModel.currentTime.collectAsStateWithLifecycle()
    val batteryLevel by appViewModel.batteryLevel.collectAsStateWithLifecycle()
    val isCharging by appViewModel.isCharging.collectAsStateWithLifecycle()
    val serverState by webserverViewModel.serverState.collectAsStateWithLifecycle()
    val currentTemperatureState by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val webserverUrlState by webserverViewModel.urlState.collectAsStateWithLifecycle()
    val videoFrame by patrolOverlayViewModel.videoFrame.collectAsStateWithLifecycle()
    val isPatrolRunning by patrolOverlayViewModel.isRunning.collectAsStateWithLifecycle()
    val isOverlayVisible by patrolOverlayViewModel.isOverlayVisible.collectAsStateWithLifecycle()

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
        Box(
            modifier = Modifier.fillMaxSize()
           ) {
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
                    RenderSelectedRoute(
                        selectedRoute = appViewModel.selectedRoute,
                        routeDeps = MainShellRouteDeps(
                            appViewModel = appViewModel,
                            settingsViewModel = settingsViewModel,
                            navigationViewModel = navigationViewModel,
                            weatherViewModel = weatherViewModel,
                            hideAndSeekViewModel = hideAndSeekViewModel,
                            serverState = serverState,
                            currentTemperatureState = currentTemperatureState,
                            webserverUrlState = webserverUrlState
                                                      ),
                        modifier = Modifier.weight(1f)
                                       )
                }
            }

            if (isPatrolRunning) {
                PatrolStreamOverlay(
                    videoFrame = videoFrame,
                    onBackClick = patrolOverlayViewModel::hideOverlay,
                    onStopPatrol = patrolOverlayViewModel::stopPatrol
                )
            }
        }
    }
}

@Composable
private fun RenderSelectedRoute(
    selectedRoute: String,
    routeDeps: MainShellRouteDeps,
    modifier: Modifier
) {
    Timber.d("Selected route: %s", selectedRoute)

    when (selectedRoute) {
        Screen.Dashboard.route -> DashboardRouteContent(
            modifier = modifier,
            appViewModel = routeDeps.appViewModel,
            serverState = routeDeps.serverState,
            currentTemperatureState = routeDeps.currentTemperatureState
        )

        Screen.Webserver.route -> WebViewScreen(routeDeps.webserverUrlState)

        Screen.Navigation.route -> NavigationContent(
            modifier = modifier,
            viewModel = routeDeps.navigationViewModel
        )

        Screen.Controller.route -> ControllerScreen(
            modifier = Modifier.fillMaxSize(),
        )

        Screen.Settings.route -> {
            HandleSettingsNavigationEvents(
                settingsViewModel = routeDeps.settingsViewModel,
                onNavigate = routeDeps.appViewModel::onRouteSelect
            )
            SettingsScreen(modifier = modifier, viewModel = routeDeps.settingsViewModel)
        }

        Screen.DisplaySettings.route -> DisplayScreen(
            onBackClick = { routeDeps.appViewModel.onRouteSelect(Screen.Settings) }
        )

        Screen.BatterySettings.route -> BatteryScreen(
            onBackClick = { routeDeps.appViewModel.onRouteSelect(Screen.Settings) }
        )

        Screen.AdminPanel.route -> AdminPanelScreen(
            onBackClick = { routeDeps.appViewModel.onRouteSelect(Screen.Settings) }
        )

        Screen.LanguageSettings.route -> LanguageScreen(
            onBackClick = { routeDeps.appViewModel.onRouteSelect(Screen.Settings) }
        )

        Screen.Weather.route -> WeatherContent(viewModel = routeDeps.weatherViewModel)

        Screen.HideAndSeek.route -> HideAndSeekScreen(
            modifier = modifier,
            viewModel = routeDeps.hideAndSeekViewModel,
            onNavigateToDashboard = { routeDeps.appViewModel.onRouteSelect(Screen.Dashboard) }
        )

        Screen.Documentation.route -> WebViewScreen("file:///android_asset/html/index.html")

        // Keep fallback behavior identical to the dashboard card content.
        else -> DashboardRouteContent(
            modifier = modifier,
            appViewModel = routeDeps.appViewModel,
            serverState = routeDeps.serverState,
            currentTemperatureState = routeDeps.currentTemperatureState
        )
    }
}

private data class MainShellRouteDeps(
    val appViewModel: AppViewModel,
    val settingsViewModel: SettingsViewModel,
    val navigationViewModel: NavigationViewModel,
    val weatherViewModel: WeatherViewModel,
    val hideAndSeekViewModel: HideAndSeekViewModel,
    val serverState: ServerState,
    val currentTemperatureState: WeatherState,
    val webserverUrlState: String
)

@Composable
private fun HandleSettingsNavigationEvents(
    settingsViewModel: SettingsViewModel,
    onNavigate: (Screen) -> Unit
) {
    LaunchedEffect(settingsViewModel) {
        settingsViewModel.navigationEvent.collect { event ->
            when (event) {
                is SettingsNavigationEvent.NavigateToDisplay -> onNavigate(Screen.DisplaySettings)
                is SettingsNavigationEvent.NavigateToBattery -> onNavigate(Screen.BatterySettings)
                is SettingsNavigationEvent.NavigateToAdminPanel -> onNavigate(Screen.AdminPanel)
                is SettingsNavigationEvent.NavigateToLanguage -> onNavigate(Screen.LanguageSettings)
            }
        }
    }
}

@Composable
private fun DashboardRouteContent(
    modifier: Modifier,
    appViewModel: AppViewModel,
    serverState: ServerState,
    currentTemperatureState: WeatherState
) {
    MainContent(
        modifier = modifier,
        onClick = { screen ->
            appViewModel.onRouteSelect(screen)
        },
        serverState = serverState,
        // TODO add utility method or catch the exception
        Integer.parseInt(currentTemperatureState.hourlyForecast[0].temp)
    )
}
