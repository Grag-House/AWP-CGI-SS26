package hka.awp.temi_cgi_app.ui.shell

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hka.awp.temi_cgi_app.feature.dashboard.MainContent
import hka.awp.temi_cgi_app.feature.settings.SettingsNavigationEvent
import hka.awp.temi_cgi_app.feature.settings.SettingsViewModel
import hka.awp.temi_cgi_app.feature.settings.about.SettingsScreen
import hka.awp.temi_cgi_app.feature.settings.battery.BatteryScreen
import hka.awp.temi_cgi_app.feature.settings.display.DisplayScreen
import hka.awp.temi_cgi_app.feature.settings.notifications.NotificationScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Die primäre UI-Shell der Anwendung.
 */
@Composable
fun MainShell(
    appViewModel: AppViewModel = koinViewModel()
) {
    val wifiLevel by appViewModel.wifiLevel.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopStatusBar(wifiLevel = wifiLevel) }
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

            when (appViewModel.selectedRoute) {
                Screen.Dashboard.route -> {
                    MainContent(
                        modifier = Modifier.weight(1f),
                        selectedRoute = appViewModel.selectedRoute,
                        onClick = { screen ->
                            appViewModel.onRouteSelect(screen)
                            Log.d("MainShell", "Dashboard navigation to $screen")
                        }
                    )
                }

                Screen.Settings.route -> {
                    val settingsViewModel: SettingsViewModel = koinViewModel()

                    LaunchedEffect(Unit) {
                        settingsViewModel.navigationEvent.collect { event ->
                            when (event) {
                                is SettingsNavigationEvent.NavigateToDisplay -> {
                                    appViewModel.onRouteSelect(Screen.DisplaySettings)
                                }
                                is SettingsNavigationEvent.NavigateToNotifications -> {
                                    appViewModel.onRouteSelect(Screen.NotificationSettings)
                                }
                                is SettingsNavigationEvent.NavigateToBattery -> {
                                    appViewModel.onRouteSelect(Screen.BatterySettings)
                                }
                            }
                        }
                    }

                    SettingsScreen(
                        modifier = Modifier.weight(1f),
                        viewModel = settingsViewModel
                    )
                }

                Screen.DisplaySettings.route -> {
                    DisplayScreen(
                        onBackClick = {
                            appViewModel.onRouteSelect(Screen.Settings)
                        }
                    )
                }

                Screen.NotificationSettings.route -> {
                    NotificationScreen(
                        onBackClick = {
                            appViewModel.onRouteSelect(Screen.Settings)
                        }
                    )
                }

                Screen.BatterySettings.route -> {
                    BatteryScreen(
                        onBackClick = {
                            appViewModel.onRouteSelect(Screen.Settings)
                        }
                    )
                }


                else -> {
                    MainContent(modifier = Modifier.weight(1f), selectedRoute = appViewModel.selectedRoute)
                }
            }
        }
    }
}