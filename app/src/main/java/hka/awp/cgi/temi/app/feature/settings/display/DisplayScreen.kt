package hka.awp.cgi.temi.app.feature.settings.display

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DisplayScreen(
    onBackClick: () -> Unit,
    viewModel: DisplayViewModel = koinViewModel()
) {
    val brightness by viewModel.brightness.collectAsState()
    val timeoutState by viewModel.screenTimeout.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    DisplayContent(
        brightness = brightness,
        onBrightnessChange = { viewModel.updateBrightness(it, context) },
        currentTimeoutLabel = timeoutState.first,
        onTimeoutClick = { showDialog = true },
        onBackClick = onBackClick,
        isDarkMode = isDarkMode,
        onDarkModeChange = { viewModel.toggleDarkMode(it) }
    )

    if (showDialog) {
        TimeoutSelectionDialog(
            options = viewModel.timeoutOptions,
            currentSelection = timeoutState.first,
            onOptionSelected = { selectedOption ->
                viewModel.updateTimeout(selectedOption, context)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

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
                text = "Inaktivitätszeit wählen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = option.first == currentSelection

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOptionSelected(option) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.first,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
