package hka.awp.cgi.temi.app.feature.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.ExpandableSettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow
import java.util.Locale
import kotlin.math.roundToInt

@Suppress("LongParameterList", "LongMethod")
@Composable
fun NotificationContent(
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    availableLocales: List<Locale>,
    selectedLocale: Locale,
    onLocaleSelect: (Locale) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        SettingsHeader(
            title = stringResource(
                R.string.notifications_voice
            ),
            onBackClick = onBackClick
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.Notifications,
                title = stringResource(
                    R.string.settings_notifications_subtitle
                ),
                subtitle =
                if (isEnabled) {
                    stringResource(
                        R.string.enabled
                    )
                } else {
                    stringResource(
                        R.string.muted
                    )
                },
                action = {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onEnabledChange
                    )
                }
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        SettingsCard {
            Text(
                text = stringResource(
                    R.string.volume
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = volume.toFloat(),
                onValueChange = {
                    onVolumeChange(it.toInt())
                },
                valueRange = 0f..10f,
                steps = 9
                  )

            Text(
                text = "$volume / 10",
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.bodyMedium
                )

        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        ExpandableSettingsCard(
            icon = Icons.Rounded.RecordVoiceOver,
            title = stringResource(
                R.string.select_speaker
            ),
            subtitle = stringResource(
                R.string.current_language,
                selectedLocale.displayName
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.surface
                            .copy(alpha = 0.5f)
                    )
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableLocales) { locale ->

                        val isSelected =
                            locale == selectedLocale

                        SettingsRow(
                            icon =
                            if (isSelected) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.Language
                            },
                            title = locale.displayName,
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onLocaleSelect(locale)
                                }
                                .padding(vertical = 4.dp),
                            action = {
                                if (isSelected) {
                                    Icon(
                                        imageVector =
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                    )
                                }
                            }
                        )

                        if (locale != availableLocales.last()) {
                            HorizontalDivider(
                                modifier =
                                Modifier.padding(
                                    horizontal = 48.dp
                                ),
                                thickness = 0.5.dp,
                                color =
                                MaterialTheme
                                    .colorScheme
                                    .outlineVariant
                                    .copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
