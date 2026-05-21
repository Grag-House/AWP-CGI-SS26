package hka.awp.cgi.temi.app.feature.settings.notifications

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
    val availableLocales by viewModel.availableLocales.collectAsState()
    val selectedLocale by viewModel.selectedLocale.collectAsState()

    NotificationContent(
        volume = volume,
        onVolumeChange = { viewModel.updateVolume(it) },
        isEnabled = isEnabled,
        onEnabledChange = { viewModel.toggleNotifications(it) },
        availableLocales = availableLocales,
        selectedLocale = selectedLocale,
        onLocaleSelect = { viewModel.setLocale(it) },
        onBackClick = onBackClick
                       )
}
