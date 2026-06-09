package hka.awp.cgi.temi.app.feature.settings.display

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DisplayScreen(
    onBackClick: () -> Unit,
    viewModel: DisplayViewModel = koinViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    DisplayContent(
        onBackClick = onBackClick,
        isDarkMode = isDarkMode,
        onDarkModeChange = viewModel::toggleDarkMode,
    )
}
