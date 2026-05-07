package hka.awp.temi_cgi_app.feature.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import hka.awp.temi_cgi_app.R
import hka.awp.temi_cgi_app.ui.theme.CgiRed
import hka.awp.temi_cgi_app.ui.theme.CgiTheme

/**
 * The main content view for the Navigation screen.
 *
 * Designed to match the Figma prototype with vertical cards and CGI Red accents.
 * Fits entirely on one screen without scrolling.
 */
@Composable
fun NavigationContent(
    modifier: Modifier = Modifier,
    currentLocation: String = stringResource(R.string.location_office),
    onDestinationClick: (Int) -> Unit = {},
    onShowMapClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
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

        // Status Card Section
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

                // "Karte anzeigen" Button
                Surface(
                    onClick = onShowMapClick,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3F5F7)
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
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Destination Selection Label
        Text(
            text = stringResource(R.string.select_destination),
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        val destinations = DestinationItems.all

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            userScrollEnabled = false, // Keep it fixed on one page
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 120.dp)
        ) {
            items(destinations) { destination ->
                NavigationCard(
                    destination = destination,
                    onClick = { onDestinationClick(destination.stringResource) }
                )
            }
        }
    }
}

@Composable
private fun NavigationCard(
    destination: DestinationItems,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.95f), // Vertical square-like proportions from the image
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
                modifier = Modifier.size(66.dp),
                tint = CgiRed
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(destination.stringResource),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun NavigationContentPreview() {
    CgiTheme {
        NavigationContent()
    }
}
