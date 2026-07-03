package hka.awp.cgi.temi.app.feature.settings.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow

/**
 * Renders the stateless layout content for the Display settings screen.
 *
 * This component provides a dedicated user interface for adjusting visual theme preferences,
 * featuring a standalone settings row with a dynamic icon, descriptive status labels,
 * and an interactive theme switch toggle for dark mode configurations.
 *
 * @param onBackClick Executed when the user interacts with the back navigation button in the header.
 * @param isDarkMode Boolean flag specifying whether the dark theme layout mode is currently active.
 * @param onDarkModeChange Callback triggered when the state switch is toggled,
 * supplying the new preferred boolean value.
 */
@Suppress("LongParameterList", "MagicNumber")
@Composable
fun DisplayContent(
    onBackClick: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        SettingsHeader(
            title = stringResource(R.string.settings_display_subtitle),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard {
            SettingsRow(
                icon = if (isDarkMode) {
                    Icons.Rounded.DarkMode
                } else {
                    Icons.Rounded.LightMode
                },

                title = stringResource(R.string.display_dark_mode),

                subtitle =
                if (isDarkMode) {
                    stringResource(R.string.display_enabled)
                } else {
                    stringResource(R.string.display_disabled)
                },

                action = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange
                    )
                }
            )
        }
    }
}
