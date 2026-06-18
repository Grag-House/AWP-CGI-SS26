package hka.awp.cgi.temi.app.feature.photobox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R

private const val DURATION_SHORT_S = 3
private const val DURATION_MEDIUM_S = 5
private const val DURATION_LONG_S = 10
private const val STRIP_DELAY_SHORT_S = 5
private const val STRIP_DELAY_MEDIUM_S = 10
private const val STRIP_DELAY_LONG_S = 15

@Composable
internal fun IdleOverlay(
    uiState: PhotoboxUiState,
    onDurationSelect: (Int) -> Unit,
    onStripDelaySelect: (Int) -> Unit,
    onBackToModeSelect: () -> Unit,
    onStart: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BottomBar(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            TextButton(onClick = onBackToModeSelect) {
                Text(text = stringResource(R.string.photobox_change_mode_button))
            }
            DurationSelectorRow(
                label = stringResource(R.string.photobox_countdown_label),
                options = listOf(DURATION_SHORT_S, DURATION_MEDIUM_S, DURATION_LONG_S),
                selected = uiState.selectedDuration,
                onSelect = onDurationSelect
            )
            if (uiState.mode == PhotoboxMode.STRIP) {
                DurationSelectorRow(
                    label = stringResource(R.string.photobox_strip_delay_label),
                    options = listOf(STRIP_DELAY_SHORT_S, STRIP_DELAY_MEDIUM_S, STRIP_DELAY_LONG_S),
                    selected = uiState.stripDelaySeconds,
                    onSelect = onStripDelaySelect
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.photobox_start_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DurationSelectorRow(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.weight(1f))
        options.forEach { seconds ->
            val isSelected = selected == seconds
            Surface(
                onClick = { onSelect(seconds) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.photobox_duration_seconds, seconds),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
