package hka.awp.temi_cgi_app.feature.dashboard

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hka.awp.temi_cgi_app.feature.settings.SettingsContent
import hka.awp.temi_cgi_app.feature.settings.SettingsViewModel
import hka.awp.temi_cgi_app.ui.shell.AppViewModel
import hka.awp.temi_cgi_app.ui.shell.Screen
import hka.awp.temi_cgi_app.ui.shell.Sidebar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TemiDashboardScreen(
    appViewModel: AppViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background
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

                Screen.Dashboard.route -> MainContent(
                    modifier = Modifier.weight(1f),
                    selectedRoute = appViewModel.selectedRoute,
                    onClick = { screen ->
                        appViewModel.onRouteSelect(screen)
                        Log.d(this.javaClass.simpleName, "Dashboard button pressed!")
                    })

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
                            Log.d(this.javaClass.simpleName, "Dashboard button pressed!")
                        })
                }
            }
        }
    }
}