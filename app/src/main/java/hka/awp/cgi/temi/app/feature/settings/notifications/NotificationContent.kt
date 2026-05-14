package hka.awp.cgi.temi.app.feature.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.ui.components.SettingsCard
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import hka.awp.cgi.temi.app.ui.components.SettingsRow
import hka.awp.cgi.temi.app.ui.components.ExpandableSettingsCard
import java.util.Locale

@Composable
fun NotificationContent(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
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
        // 1. Header
        SettingsHeader(
            title = "Benachrichtigungen & Stimme",
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Card
        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.Notifications,
                title = "Töne & Systemmeldungen",
                subtitle = if (isEnabled) "Eingeschaltet" else "Stummgeschaltet",
                action = {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onEnabledChange
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Card
        SettingsCard {
            Text(
                text = "Lautstärke",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                enabled = isEnabled,
                valueRange = 0f..1f
            )

            Text(
                text = "${(volume * 100).toInt()}%",
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        //4. expandable Settings card
        ExpandableSettingsCard(
            icon = Icons.Rounded.RecordVoiceOver,
            title = "Sprecher auswählen",
            subtitle = "Aktuelle Sprache: ${selectedLocale.displayName}"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(availableLocales) { locale ->
                        val isSelected = locale == selectedLocale

                        SettingsRow(
                            icon = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Language,
                            title = locale.displayName,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onLocaleSelect(locale) }
                                .padding(vertical = 4.dp), // Etwas mehr Platz zwischen den Items
                            action = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        if (locale != availableLocales.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 48.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
