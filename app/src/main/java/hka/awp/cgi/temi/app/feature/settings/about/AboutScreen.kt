package hka.awp.cgi.temi.app.feature.settings.about


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.feature.settings.AboutContent
import hka.awp.cgi.temi.app.feature.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val aboutInfo by viewModel.aboutInfo.collectAsStateWithLifecycle()

    AboutContent(
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
