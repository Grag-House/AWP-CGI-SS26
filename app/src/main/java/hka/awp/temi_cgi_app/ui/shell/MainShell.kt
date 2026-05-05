package hka.awp.temi_cgi_app.ui.shell

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.temi_cgi_app.BuildConfig
import hka.awp.temi_cgi_app.feature.dashboard.MainContent
import hka.awp.temi_cgi_app.feature.navigation.DestinationItems
import hka.awp.temi_cgi_app.feature.navigation.NavigationContent
import hka.awp.temi_cgi_app.feature.navigation.NavigationViewModel
import hka.awp.temi_cgi_app.feature.settings.SettingsContent
import hka.awp.temi_cgi_app.feature.settings.SettingsViewModel
import hka.awp.temi_cgi_app.feature.webserver.WebViewScreen
import hka.awp.temi_cgi_app.feature.webserver.WebserverViewModel
import org.koin.compose.viewmodel.koinViewModel

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
@Composable
fun MainShell(
    appViewModel: AppViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    navigationViewModel: NavigationViewModel = koinViewModel(),
    webserverViewModel: WebserverViewModel = koinViewModel()
) {
    val wifiLevel by appViewModel.wifiLevel.collectAsStateWithLifecycle()
    val currentTime by appViewModel.currentTime.collectAsStateWithLifecycle()
    val batteryLevel by appViewModel.batteryLevel.collectAsStateWithLifecycle()
    val isCharging by appViewModel.isCharging.collectAsStateWithLifecycle()
    val serverState by webserverViewModel.serverState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopStatusBar(
                wifiLevel = wifiLevel,
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                isCharging = isCharging
            )
        }) { paddingValues ->
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

            when (appViewModel.selectedRoute) {

                Screen.Dashboard.route -> MainContent(
                    modifier = Modifier.weight(1f),
                    selectedRoute = appViewModel.selectedRoute,
                    onClick = { screen ->
                        appViewModel.onRouteSelect(screen)
                    },
                    serverState = serverState
                )

                Screen.Webserver.route -> WebViewScreen(BuildConfig.WEBVIEW_URL)

                Screen.Navigation.route -> NavigationContent(
                    modifier = Modifier.weight(1f), currentLocation = stringResource(
                        DestinationItems.Office.stringResource,
                    ), onDestinationClick = navigationViewModel::onNavigationClick
                )

                Screen.Settings.route -> SettingsContent(
                    onItemClick = settingsViewModel::onSettingsItemClick
                )

                //redundancy
                else -> {
                    MainContent(
                        modifier = Modifier.weight(1f),
                        selectedRoute = appViewModel.selectedRoute,
                        onClick = { screen ->
                            appViewModel.onRouteSelect(screen)
                        },
                        serverState = serverState
                    )
                }
            }
        }
    }
}