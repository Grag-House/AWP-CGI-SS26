import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import java.util.Locale

@Composable
@Suppress("MaximumLineLength")
fun LanguageContent(
    selectedLocale: Locale,
    supportedLocales: List<Locale>,
    onLocaleChange: (Locale) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        SettingsHeader(
            title = stringResource(R.string.settings_languages_title),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingsCard {
            Column {
                supportedLocales.forEach { locale ->
                    val isSelected = locale == selectedLocale
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme
                                        .primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable { onLocaleChange(locale) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            text = locale.displayLanguage,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
