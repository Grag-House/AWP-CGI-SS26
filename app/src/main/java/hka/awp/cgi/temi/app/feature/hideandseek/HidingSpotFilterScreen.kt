package hka.awp.cgi.temi.app.feature.hideandseek

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R

data class HidingSpotFilterState(
    val allLocations: List<String> = emptyList(),
    val enabledSpots: Set<String> = emptySet()
)

data class HidingSpotFilterCallbacks(
    val onToggle: (String) -> Unit,
    val onSelectAll: () -> Unit,
    val onDeselectAll: () -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit
)

/**
 * Self-contained checklist UI for selecting which hiding spots are allowed.
 * Can be embedded in a Dialog, a full screen, or any other container.
 *
 * @param state     Current filter state with all locations and enabled spots.
 * @param callbacks All interaction callbacks bundled together.
 */
@Composable
fun HidingSpotFilterContent(
    state: HidingSpotFilterState,
    callbacks: HidingSpotFilterCallbacks,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 500.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            FilterTitleRow()
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.hiding_spot_filter_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            FilterQuickActionRow(
                onSelectAll = callbacks.onSelectAll,
                onDeselectAll = callbacks.onDeselectAll
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            FilterLocationList(state = state, onToggle = callbacks.onToggle)
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            val activeCount = state.enabledSpots.count { it in state.allLocations }
            Text(
                text = stringResource(R.string.hiding_spot_filter_count, activeCount, state.allLocations.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            FilterActionButtons(onDismiss = callbacks.onDismiss, onSave = callbacks.onSave)
        }
    }
}

@Composable
private fun FilterTitleRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.hiding_spot_filter_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FilterQuickActionRow(onSelectAll: () -> Unit, onDeselectAll: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onSelectAll,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.hiding_spot_filter_select_all))
        }
        OutlinedButton(
            onClick = onDeselectAll,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.hiding_spot_filter_deselect_all))
        }
    }
}

@Composable
private fun FilterLocationList(state: HidingSpotFilterState, onToggle: (String) -> Unit) {
    if (state.allLocations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.hiding_spot_filter_no_locations),
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
        ) {
            items(state.allLocations, key = { it }) { location ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(location) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = location in state.enabledSpots,
                        onCheckedChange = { onToggle(location) }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = location, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun FilterActionButtons(onDismiss: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.hiding_spot_filter_cancel))
        }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.hiding_spot_filter_save),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
