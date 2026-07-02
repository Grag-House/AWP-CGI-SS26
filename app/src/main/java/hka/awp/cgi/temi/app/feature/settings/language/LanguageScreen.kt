package hka.awp.cgi.temi.app.feature.settings.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point screen for the Language configuration feature.
 *
 * This composable acts as the bridge between dependency injection frameworks and presentation layers.
 * It accesses the [LanguageViewModel] instance via Koin, observes active user locale changes reactively,
 * and passes the platform's execution context into event handlers when updating the application's global locale.
 *
 * @param viewModel The state management and localization logic provider, injected via Koin by default.
 * @param onBackClick Executed when the user interacts with the navigation back button.
 */
@Composable
fun LanguageScreen(
    viewModel: LanguageViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedLocale by viewModel.selectedLocale.collectAsState()

    LanguageContent(
        selectedLocale = selectedLocale,
        supportedLocales = viewModel.supportedLocales,
        onLocaleChange = { newLocale ->
            viewModel.updateLocale(newLocale.language, context)
        },
        onBackClick = onBackClick
    )
}
