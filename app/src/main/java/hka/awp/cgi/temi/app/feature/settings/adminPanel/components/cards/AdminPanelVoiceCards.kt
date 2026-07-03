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

/**
 * Renders a configuration card for monitoring and modifying the speaker verification confidence threshold.
 *
 * Displays the current sensitivity value as a formatted decimal and triggers a modification dialog
 * or inline editor upon interaction.
 *
 * @param threshold The mathematical verification boundary score required to confirm a biometric voice match.
 * @param onEdit Executed when the user requests a change to the verification threshold value.
 */
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

/**
 * Renders an administration toggle card to enable or disable biometric speaker verification functionality.
 *
 * When deactivated, the application processes voice triggers globally without restricting actions
 * to recognized administrator speech profiles.
 *
 * @param enabled Specifies whether voice-print matching barriers are currently enforced.
 * @param onToggle Callback triggered when the master hardware/subsystem state switch is flipped.
 */
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

/**
 * Renders a comprehensive profile management layout handling biometric voice token registrations.
 *
 * This component visually reflects dynamic enrollment states (e.g., actively learning a new voice signature),
 * prints summary statistics of current entries, and renders a localized sub-list for single-entry profile deletions.
 *
 * @param voiceProfiles A mapped directory collection of registered
 * user names paired with their underlying [SpeakerVector] biometric signatures.
 * @param isEnrollmentActive Flag indicating whether the system recording
 * pipeline is capturing audio for a registration process.
 * @param onLearnClick Intercepts clicks to either spin up or explicitly
 * halt an active enrollment audio sequence.
 * @param onDeleteClick Triggered when removing an existing profile
 * signature identifier from active database configurations.
 */
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
