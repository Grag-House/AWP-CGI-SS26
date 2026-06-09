package hka.awp.cgi.temi.app.feature.hideandseek

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.R

private const val SECONDS_PER_MINUTE = 60
private const val URGENT_SECONDS_THRESHOLD = 30

@Composable
fun HideAndSeekScreen(
    modifier: Modifier = Modifier,
    viewModel: HideAndSeekViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterManager.filterState.collectAsStateWithLifecycle()
    val showFilter by viewModel.filterManager.isOpen.collectAsStateWithLifecycle()
    val hasActiveFilter by viewModel.filterManager.hasActiveFilter.collectAsStateWithLifecycle()

    if (showFilter) {
        Dialog(onDismissRequest = viewModel.filterManager::dismiss) {
            HidingSpotFilterContent(
                state = filterState,
                callbacks = HidingSpotFilterCallbacks(
                    onToggle = viewModel.filterManager::toggle,
                    onSelectAll = viewModel.filterManager::selectAll,
                    onDeselectAll = viewModel.filterManager::deselectAll,
                    onSave = viewModel.filterManager::save,
                    onDismiss = viewModel.filterManager::dismiss
                )
            )
        }
    }

    when (uiState.gameState) {
        GameState.SETUP -> SetupContent(
            modifier = modifier.fillMaxSize().padding(24.dp),
            uiState = uiState,
            hasActiveFilter = hasActiveFilter,
            callbacks = SetupCallbacks(
                onIncrease = { viewModel.adjustSearchTime(1) },
                onDecrease = { viewModel.adjustSearchTime(-1) },
                onStart = viewModel::startGame,
                onOpenFilter = viewModel.filterManager::open
            )
        )
        GameState.HIDING -> HidingContent(
            modifier = modifier.fillMaxSize().padding(24.dp),
            uiState = uiState,
            onCancel = viewModel::cancelGame
        )
        GameState.WAITING -> WaitingContent(
            modifier = modifier.fillMaxSize().padding(24.dp),
            uiState = uiState,
            onFound = viewModel::onPlayerFound,
            onCancel = viewModel::cancelGame
        )
        GameState.WON -> WonContent(
            modifier = modifier.fillMaxSize().padding(24.dp),
            uiState = uiState,
            onPlayAgain = viewModel::cancelGame,
            onToDashboard = onNavigateToDashboard
        )
        GameState.LOST -> LostContent(
            modifier = modifier.fillMaxSize().padding(24.dp),
            uiState = uiState,
            onPlayAgain = viewModel::cancelGame
        )
    }
}

private data class SetupCallbacks(
    val onIncrease: () -> Unit,
    val onDecrease: () -> Unit,
    val onStart: () -> Unit,
    val onOpenFilter: () -> Unit
)

@Composable
private fun SetupContent(
    modifier: Modifier = Modifier,
    uiState: HideAndSeekUiState,
    hasActiveFilter: Boolean,
    callbacks: SetupCallbacks
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SetupLeftColumn(modifier = Modifier.weight(1f).fillMaxHeight())

        SetupRightColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            uiState = uiState,
            hasActiveFilter = hasActiveFilter,
            callbacks = callbacks
        )
    }
}

@Composable
private fun SetupLeftColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.SportsEsports,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.hide_and_seek),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.temi_hiding),
            contentDescription = stringResource(R.string.hide_and_seek_image_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

@Composable
private fun SetupRightColumn(
    modifier: Modifier = Modifier,
    uiState: HideAndSeekUiState,
    hasActiveFilter: Boolean,
    callbacks: SetupCallbacks
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TimePickerCard(
            minutes = uiState.searchTimeMinutes,
            onIncrease = callbacks.onIncrease,
            onDecrease = callbacks.onDecrease
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.hide_and_seek_intro_text, uiState.searchTimeMinutes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 26.sp
            )
        }
        OutlinedButton(
            onClick = callbacks.onOpenFilter,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (hasActiveFilter) {
                    stringResource(R.string.hiding_spot_filter_button_active)
                } else {
                    stringResource(R.string.hiding_spot_filter_button)
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
        Button(
            onClick = callbacks.onStart,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.hide_and_seek_start_button),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimePickerCard(
    minutes: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.hide_and_seek_search_time_label),
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Surface(
                    onClick = onDecrease,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.hide_and_seek_search_time_value, minutes),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 100.dp),
                    textAlign = TextAlign.Center
                )
                Surface(
                    onClick = onIncrease,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun HidingContent(
    modifier: Modifier = Modifier,
    uiState: HideAndSeekUiState,
    onCancel: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left: Temi image
        Image(
            painter = painterResource(R.drawable.temi_hiding),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        )

        // Right: status + countdown + cancel
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.hide_and_seek_hiding_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.hide_and_seek_hiding_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.hide_and_seek_hiding_bystander_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.hide_and_seek_hiding_countdown_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stringResource(
                            R.string.hide_and_seek_hiding_seconds,
                            uiState.hidingSecondsRemaining
                        ),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.hide_and_seek_cancel),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun WaitingContent(
    modifier: Modifier = Modifier,
    uiState: HideAndSeekUiState,
    onFound: () -> Unit,
    onCancel: () -> Unit
) {
    val totalSeconds = (uiState.searchTimeMinutes * SECONDS_PER_MINUTE).toFloat()
    val progress = if (totalSeconds > 0) uiState.searchSecondsRemaining / totalSeconds else 0f
    val isUrgent = uiState.searchSecondsRemaining <= URGENT_SECONDS_THRESHOLD
    val timerColor = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val trackColor = if (isUrgent) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.hide_and_seek_waiting_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.hide_and_seek_cancel),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Robot behind the circle, shifted left so circle overlaps its right side
                Image(
                    painter = painterResource(R.drawable.temi_waiting),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(200.dp)
                        .offset(x = (-170).dp)
                )
                // Circle centered and rendered on top
                Box(contentAlignment = Alignment.Center) {
                    // Filled background matching screen background
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                    )
                    // Background ring
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(280.dp),
                        strokeWidth = 16.dp,
                        color = trackColor
                    )
                    // Timer ring
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(280.dp),
                        strokeWidth = 16.dp,
                        color = timerColor
                    )
                    // Time text in center
                    Text(
                        text = formatTime(uiState.searchSecondsRemaining),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = timerColor
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onFound,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(R.string.hide_and_seek_found_button),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WonContent(
    modifier: Modifier = Modifier,
    uiState: HideAndSeekUiState,
    onPlayAgain: () -> Unit,
    onToDashboard: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.hide_and_seek_won_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = stringResource(R.string.hide_and_seek_won_message, formatTime(uiState.elapsedSeconds)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onPlayAgain,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(min = 280.dp).height(60.dp)
        ) {
            Text(
                text = stringResource(R.string.hide_and_seek_play_again),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onToDashboard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(min = 280.dp).height(60.dp)
        ) {
            Text(
                text = stringResource(R.string.hide_and_seek_to_dashboard),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun LostContent(
    modifier: Modifier = Modifier,
    uiState: HideAndSeekUiState,
    onPlayAgain: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.temi_hiding),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        )
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.hide_and_seek_lost_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.hide_and_seek_lost_location_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.hidingSpotName.ifBlank {
                            stringResource(R.string.hide_and_seek_lost_location_unknown)
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onPlayAgain,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(min = 280.dp).height(60.dp)
            ) {
                Text(
                    text = stringResource(R.string.hide_and_seek_play_again),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return "%02d:%02d".format(minutes, seconds)
}
