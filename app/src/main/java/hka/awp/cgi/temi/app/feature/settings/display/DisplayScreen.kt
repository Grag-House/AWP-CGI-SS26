package hka.awp.cgi.temi.app.feature.settings.display

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import org.koin.compose.viewmodel.koinViewModel

@Suppress("UNUSED_VALUE")
@Composable
fun DisplayScreen(
    onBackClick: () -> Unit,
    viewModel: DisplayViewModel = koinViewModel()
) {
    val brightness by viewModel.brightness.collectAsState()
    val timeoutState by viewModel.screenTimeout.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    DisplayContent(
        brightness = brightness,
        onBrightnessChange = viewModel::updateBrightness,
        currentTimeoutLabel = timeoutState.first,
        onTimeoutClick = { showDialog = true },
        onBackClick = onBackClick,
        isDarkMode = isDarkMode,
        onDarkModeChange = viewModel::toggleDarkMode
    )

    if (showDialog) {
        TimeoutSelectionDialog(
            options = viewModel.timeoutOptions,
            currentSelection = timeoutState.first,
            onOptionSelected = { selectedOption ->
                viewModel.updateTimeout(selectedOption)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Suppress("LongMethod")
@Composable
fun TimeoutSelectionDialog(
    options: List<Pair<String, Int>>,
    currentSelection: String,
    onOptionSelected: (Pair<String, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        title = {
            Text(
                text = stringResource(
                    R.string.display_timeout_dialog_title
                ),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected =
                        option.first == currentSelection

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onOptionSelected(option)
                            },
                        color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                            verticalAlignment =
                            Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.first,
                                style =
                                MaterialTheme.typography.bodyLarge,
                                fontWeight =
                                if (isSelected) {
                                    androidx.compose.ui.text.font.FontWeight.Bold
                                } else {
                                    androidx.compose.ui.text.font.FontWeight.Normal
                                },
                                color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )

                            if (isSelected) {
                                Spacer(
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector =
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint =
                                    MaterialTheme.colorScheme.primary,
                                    modifier =
                                    Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            modifier =
                            Modifier.padding(
                                horizontal = 16.dp
                            ),
                            thickness = 0.5.dp,
                            color =
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(
                        R.string.close
                    ),
                    color =
                    MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
