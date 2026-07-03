package hka.awp.cgi.temi.app.feature.settings.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point screen for the Display settings feature.
 *
 * This composable connects the presentation layer with dependency injection. It retrieves
 * the [DisplayViewModel] via Koin, collects its reactive UI state flows into Compose-aware state,
 * and routes events (such as toggling dark mode) back into the ViewModel handlers.
 *
 * @param onBackClick Executed when the user requests navigation back from this screen.
 * @param viewModel The state provider and interaction coordinator for display settings, injected via Koin by default.
 */
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
