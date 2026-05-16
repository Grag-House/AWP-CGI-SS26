package hka.awp.temi_cgi_app.feature.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robotemi.sdk.navigation.model.Position
import org.koin.compose.viewmodel.koinViewModel
import hka.awp.temi_cgi_app.R
import hka.awp.temi_cgi_app.ui.theme.CgiRed

/** Hauptansicht der Navigation mit Standortanzeige, Zielkästchen und optionalem Kartendialog. */
@Composable
fun NavigationContent(
    modifier: Modifier = Modifier
) {
    val viewModel: NavigationViewModel = koinViewModel()
    val currentLocationState by viewModel.currentLocation.collectAsStateWithLifecycle()
    val mapLocations by viewModel.mapLocations.collectAsStateWithLifecycle()
    val isMapLoading by viewModel.isMapLoading.collectAsStateWithLifecycle()
    val hasMapError by viewModel.hasMapError.collectAsStateWithLifecycle()
    val robotPosition by viewModel.robotPosition.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()
    val currentLocation = when (val loc = currentLocationState) {
        is LocationState.Resource -> stringResource(loc.resId)
        is LocationState.Custom -> loc.name
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.navigation_status_label),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.current_location_prefix))
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
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        onClick = { viewModel.showMap() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF3F5F7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.select_destination),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp)
            ) {
                items(DestinationItems.all) { destination ->
                    NavigationCard(
                        destination = destination,
                        onClick = { viewModel.goToLocationByResId(destination.stringResource) }
                    )
                }
            }
        }

        if (isMapLoading || mapLocations.isNotEmpty() || hasMapError) {
            Dialog(
                onDismissRequest = { viewModel.dismissMap() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.show_map),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.dismissMap() }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isMapLoading -> CircularProgressIndicator(color = CgiRed)
                                hasMapError -> LocationFallbackList(
                                    locations = savedLocations,
                                    onGoToLocation = { name ->
                                        viewModel.goToLocation(name)
                                        viewModel.dismissMap()
                                    }
                                )
                                mapLocations.isNotEmpty() -> LocationMapCanvas(
                                    locations = mapLocations,
                                    robotPosition = robotPosition
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Zeigt eine scrollbare Liste der gespeicherten Orte als Fallback, wenn die Karte nicht geladen werden konnte. */
@Composable
private fun LocationFallbackList(
    locations: List<String>,
    onGoToLocation: (String) -> Unit
) {
    if (locations.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = CgiRed,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.show_map_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.saved_locations, locations.size),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(locations) { name ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Place,
                            contentDescription = null,
                            tint = CgiRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onGoToLocation(name) }) {
                            Text(
                                text = stringResource(R.string.go_to),
                                color = CgiRed,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Zeichnet eine interaktive Karte mit allen Ortsmarkierungen und der aktuellen Roboterposition (zoom- und verschiebbar). */
@Composable
private fun LocationMapCanvas(
    locations: List<LocationMarker>,
    robotPosition: Position? = null
) {
    val minX = remember(locations) { locations.minOf { it.x } }
    val maxX = remember(locations) { locations.maxOf { it.x } }
    val minY = remember(locations) { locations.minOf { it.y } }
    val maxY = remember(locations) { locations.maxOf { it.y } }

    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(50, 50, 50)
            textSize = 24f
            isAntiAlias = true
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.3f, 12f)
                    offset += pan
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            val pad = 0.12f
            val rangeX = (maxX - minX).coerceAtLeast(0.1f)
            val rangeY = (maxY - minY).coerceAtLeast(0.1f)
            val usableW = size.width * (1f - 2f * pad)
            val usableH = size.height * (1f - 2f * pad)

            fun toX(x: Float) = (x - minX) / rangeX * usableW + size.width * pad
            fun toY(y: Float) = (1f - (y - minY) / rangeY) * usableH + size.height * pad

            locations.forEach { loc ->
                val cx = toX(loc.x)
                val cy = toY(loc.y)
                drawCircle(CgiRed, radius = 14f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = 6f, center = Offset(cx, cy))
                drawContext.canvas.nativeCanvas.drawText(loc.name, cx + 18f, cy + 8f, labelPaint)
            }

            robotPosition?.let { pos ->
                val cx = toX(pos.x)
                val cy = toY(pos.y)
                drawCircle(Color.Blue, radius = 18f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = 7f, center = Offset(cx, cy))
            }
        }
    }
}

/** Einzelne Kachel für einen Zielort mit Icon und Bezeichnung. */
@Composable
private fun NavigationCard(
    destination: DestinationItems,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                destination.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = CgiRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(destination.stringResource),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
