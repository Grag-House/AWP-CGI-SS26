
package hka.awp.temi_cgi_app.ui.shell

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hka.awp.temi_cgi_app.feature.dashboard.MainContent
import hka.awp.temi_cgi_app.feature.settings.SettingsContent
import hka.awp.temi_cgi_app.feature.settings.SettingsScreen
import hka.awp.temi_cgi_app.feature.settings.SettingsViewModel
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
    appViewModel: AppViewModel = koinViewModel()
) {
    // Wir beobachten nur noch globale App-Zustände (wie Wifi)
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

            // Der Content-Bereich entscheidet nur noch, WELCHEN Screen er anzeigt
            when (appViewModel.selectedRoute) {
                Screen.Dashboard.route -> {
                    // Falls du für Dashboard auch einen Wrapper baust: DashboardScreen()
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
                    // Hier rufen wir den neuen Wrapper auf.
                    // Er kümmert sich selbst um sein SettingsViewModel.
                    SettingsScreen(
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    // Fallback auf Dashboard
                    MainContent(modifier = Modifier.weight(1f), selectedRoute = appViewModel.selectedRoute)
                }
            }
        }
    }
}