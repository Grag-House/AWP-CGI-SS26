package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PhotoboxSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: PhotoboxSettingsViewModel = koinViewModel()
) {
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()

    PhotoboxSettingsContent(
        onBackClick = onBackClick,
        overlayEnabled = overlayEnabled,
        onOverlayEnabledChange = viewModel::setOverlayEnabled
    )
}
