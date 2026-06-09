package hka.awp.cgi.temi.app.feature.settings.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NavigationScreen(
    onBackClick: () -> Unit,
    viewModel: NavigationViewModel = koinViewModel()
) {
    val goToSpeedState = viewModel.goToSpeed.collectAsStateWithLifecycle()

    NavigationContent(
        currentGoToSpeed = goToSpeedState.value,
        onGoToSpeedChange = { newSpeed -> viewModel.updateGoToSpeed(newSpeed) },
        onBackClick = onBackClick
    )
}
