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
    // 1. Daten vom ViewModel abonnieren
    val aboutInfo by viewModel.aboutInfo.collectAsState()

    // 2. Den Content aufrufen und mit Logik füttern
    SettingsContent(
        modifier = modifier,
        aboutInfo = aboutInfo,
        onItemClick = { item ->
            // Hier entscheidest du, was bei welchem Item passiert
            viewModel.onSettingsItemClick(item)
        },
        onDismissAbout = {
            viewModel.dismissAbout()
        }
    )
}