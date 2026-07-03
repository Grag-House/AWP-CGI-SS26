package hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R

/**
 * Displays an alert dialog for configuring and assembling custom patrol routes.
 *
 * This modal maps out a vertically scrollable list of pre-saved location waypoints. It initializes
 * an internal mutable state track utilizing `mutableStateListOf` to dynamically add or eliminate
 * specific targets during checkpoint selection before committing changes back to persistent storage layers.
 *
 * @param savedLocations The master collection list containing all discoverable or recorded device location tags.
 * @param initialRoute The snapshot list configuration indicating which
 * location steps are active upon launching the view.
 * @param onDismiss Callback triggered when terminating the dialog overlay container layout.
 * @param onSave Event hook fired upon submission, supplying the newly compiled collection sequence of waypoint tags.
 */
@Composable
fun AdminPanelRouteDialog(
    savedLocations: List<String>,
    initialRoute: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val selectedRoute = remember(initialRoute) {
        mutableStateListOf<String>().apply {
            addAll(initialRoute)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.patrol_route_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.patrol_route_dialog_subtitle))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(savedLocations) { location ->
                        val isSelected = location in selectedRoute

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedRoute.add(location)
                                        } else {
                                            selectedRoute.remove(location)
                                        }
                                    }
                                )

                                Text(
                                    text = location,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedRoute.toList())
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
        }
    )
}
