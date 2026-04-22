package hka.awp.temi_cgi_app.feature.dashboard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hka.awp.temi_cgi_app.ui.shell.SidebarViewModel
import hka.awp.temi_cgi_app.ui.shell.Sidebar

@Composable
fun TemiDashboardScreen(viewModel: SidebarViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Sidebar(
                isExpanded = viewModel.isSidebarExpanded,
                selectedRoute = viewModel.selectedRoute,
                onRouteSelected = { screen -> viewModel.onRouteSelect(screen)},
                onSidebarToggle = { viewModel.onSideBarToggle() },
                modifier = Modifier.width(260.dp)
            )

            MainContent(
                modifier = Modifier.weight(1f)
            )
        }
    }
}