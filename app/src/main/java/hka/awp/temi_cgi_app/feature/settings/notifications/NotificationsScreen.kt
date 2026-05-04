package hka.awp.temi_cgi_app.feature.settings.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationViewModel = koinViewModel()
) {
    val volume by viewModel.volume.collectAsState()
    val isEnabled by viewModel.notificationsEnabled.collectAsState()

    NotificationContent(
        volume = volume,
        onVolumeChange = { viewModel.updateVolume(it) },
        isEnabled = isEnabled,
        onEnabledChange = { viewModel.toggleNotifications(it) },
        onBackClick = onBackClick
    )
}