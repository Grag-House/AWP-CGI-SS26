package hka.awp.cgi.temi.app.feature.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robotemi.sdk.navigation.model.Position
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.NavigationCard
import hka.awp.cgi.temi.app.ui.theme.CgiRed
import androidx.compose.foundation.lazy.grid.items as gridItems

private const val GRIDCELL_COUNT = 3

/**
 * The main content view for the Navigation screen.
 *
 * This component displays the current location status and a grid of destination options.
 * It is designed to match the Figma prototype, featuring vertical cards, CGI Red accents,
 * and a fixed layout that fits on a single screen without scrolling.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 */
@Composable
fun NavigationContent(
    modifier: Modifier = Modifier,
    viewModel: NavigationViewModel
) {
    val locationState by viewModel.currentLocation.collectAsStateWithLifecycle()
    val isMapLoading by viewModel.isMapLoading.collectAsStateWithLifecycle()
    val hasMapError by viewModel.hasMapError.collectAsStateWithLifecycle()
    val mapLocations by viewModel.mapLocations.collectAsStateWithLifecycle()
    val robotPosition by viewModel.robotPosition.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()

    val currentLocation = when (val state = locationState) {
        is LocationState.Resource -> stringResource(state.resId)
        is LocationState.Custom -> state.name
    }

    if (isMapLoading || hasMapError || mapLocations.isNotEmpty()) {
        MapDialog(
            state = MapDialogState(
                isLoading = isMapLoading,
                hasError = hasMapError,
                locations = mapLocations,
                savedLocations = savedLocations,
                robotPosition = robotPosition
            ),
            onDismiss = viewModel::dismissMap,
            onRetry = viewModel::showMap,
            onNavigateTo = { name ->
                viewModel.goToLocation(name)
                viewModel.dismissMap()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        NavigationHeader()

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel(text = stringResource(R.string.navigation_status_label))
        Spacer(modifier = Modifier.height(4.dp))

        CurrentLocationStatus(
            currentLocation = currentLocation,
            onMapClick = viewModel::showMap
        )

        Spacer(modifier = Modifier.height(4.dp))

        SectionLabel(text = stringResource(R.string.select_destination))
        Spacer(modifier = Modifier.height(8.dp))

        DestinationsGrid(
            destinations = DestinationItems.all,
            onDestinationClick = { destination -> viewModel.goToLocation(destination.systemName) }
        )
    }
}

private data class MapDialogState(
    val isLoading: Boolean,
    val hasError: Boolean,
    val locations: List<LocationMarker>,
    val savedLocations: List<String>,
    val robotPosition: Position?
)

@Composable
private fun MapDialog(
    state: MapDialogState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = { Text(stringResource(R.string.show_map)) },
        text = {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CgiRed)
                }
                state.hasError -> Text(
                    text = stringResource(R.string.show_map_failed),
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> Column {
                    Text(
                        text = stringResource(R.string.saved_locations, state.locations.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(state.locations) { marker ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = marker.name,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = { onNavigateTo(marker.name) }) {
                                        Text(stringResource(R.string.go_to))
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                    state.robotPosition?.let { pos ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "x=%.2f, y=%.2f".format(pos.x, pos.y),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    if (state.savedLocations.isNotEmpty() && state.locations.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.savedLocations.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !state.isLoading) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = if (state.hasError) {
            {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.map_retry))
                }
            }
        } else {
            null
        }
    )
}

@Composable
private fun NavigationHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.NearMe,
            contentDescription = null,
            tint = CgiRed,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.navigation),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.LightGray,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun CurrentLocationStatus(
    currentLocation: String,
    onMapClick: () -> Unit
) {
    val prefix = stringResource(R.string.current_location_prefix)
    val annotatedLocation = remember(currentLocation, prefix) {
        buildAnnotatedString {
            append(prefix)
            append(" ")
            withStyle(
                style = SpanStyle(
                    color = CgiRed,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(currentLocation)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(color = 0xFFEEEEEE)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = annotatedLocation,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )

            MapButton(onClick = onMapClick)
        }
    }
}

@Composable
private fun MapButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(color = 0xFFF3F5F7)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.Map,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.show_map),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp,
                    fontSize = 9.sp
                )
            )
        }
    }
}

@Composable
private fun DestinationsGrid(
    destinations: List<DestinationItems>,
    onDestinationClick: (DestinationItems) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRIDCELL_COUNT),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 120.dp)
    ) {
        gridItems(destinations) { destination ->
            NavigationCard(
                label = stringResource(destination.stringResource),
                icon = destination.icon,
                onClick = { onDestinationClick(destination) }
            )
        }
    }
}
