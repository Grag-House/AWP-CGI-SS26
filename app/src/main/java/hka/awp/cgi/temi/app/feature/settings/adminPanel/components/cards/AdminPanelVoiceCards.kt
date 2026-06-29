package hka.awp.cgi.temi.app.feature.settings.adminPanel.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigSubtext
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.ConfigValue
import hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs.VoiceProfileList
import hka.awp.cgi.temi.app.feature.voiceRecognition.SpeakerVector
import java.util.Locale

@Composable
fun SpeakerVerificationThresholdCard(
    threshold: Double,
    onEdit: () -> Unit,
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Mic,
                contentDescription = "Speaker Verification Threshold",
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue("Voice Match Threshold")
                ConfigSubtext(String.format(Locale.US, "%.2f", threshold))
            }
            Text(
                text = stringResource(R.string.admin_panel_speaker_verification_threshold_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onEdit),
            )
        }
    }
}

@Composable
fun SpeakerVerificationCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Mic,
                contentDescription = stringResource(R.string.admin_panel_speaker_verification)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_speaker_verification))
                ConfigSubtext(stringResource(R.string.admin_panel_speaker_verification_subtitle))
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun VoiceProfilesManagementCard(
    voiceProfiles: Map<String, SpeakerVector>,
    isEnrollmentActive: Boolean,
    onLearnClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    ConfigCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConfigIconBox(Icons.Outlined.Mic, "Stimmen-Management")
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ConfigValue("Sprachprofile")
                    ConfigSubtext(
                        when {
                            isEnrollmentActive -> "Enrollment aktiv..."
                            voiceProfiles.isEmpty() -> "Keine Profile gespeichert"
                            else -> "${voiceProfiles.size} Profile gespeichert"
                        }
                    )
                }
                Text(
                    text = if (isEnrollmentActive) "Stopp" else "Lernen",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onLearnClick)
                )
            }
            if (voiceProfiles.isNotEmpty()) {
                VoiceProfileList(voiceProfiles.keys.toList(), onDeleteClick)
            }
        }
    }
}
