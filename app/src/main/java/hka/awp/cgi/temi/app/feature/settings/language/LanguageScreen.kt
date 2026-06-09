package hka.awp.cgi.temi.app.feature.settings.language

import LanguageContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LanguageScreen(
    viewModel: LanguageViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val selectedLocale by viewModel.selectedLocale.collectAsState()

    LanguageContent(
        selectedLocale = selectedLocale,
        supportedLocales = viewModel.supportedLocales,
        onLocaleChange = { newLocale ->
            viewModel.updateLocale(newLocale)
        },
        onBackClick = onBackClick
    )
}
