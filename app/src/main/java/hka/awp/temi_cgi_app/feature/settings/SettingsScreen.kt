package hka.awp.temi_cgi_app.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val aboutInfo by viewModel.aboutInfo.collectAsState()

    SettingsContent(
        modifier = modifier,
        aboutInfo = aboutInfo,
        onItemClick = { item ->
            viewModel.onSettingsItemClick(item)
        },
        onDismissAbout = {
            viewModel.dismissAbout()
        }
    )
}