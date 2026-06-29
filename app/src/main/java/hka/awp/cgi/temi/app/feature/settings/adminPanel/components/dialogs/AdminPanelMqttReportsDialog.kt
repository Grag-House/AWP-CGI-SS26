package hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.mqtt.MqttTrafficDirection
import hka.awp.cgi.temi.app.feature.mqtt.MqttTrafficEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val reportTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * Displays a diagnostic modal dialog detailing MQTT broker communication logs and tracked topics.
 *
 * This dialog renders a scrollable view combining a list of monitored subscription topics with
 * a chronological feed of inbound and outbound [MqttTrafficEvent] telemetry occurrences. It provides
 * actions to clear the active log cache or dismiss the display window.
 *
 * @param monitoredTopics A distinct set of unique MQTT channel paths currently being analyzed by the app.
 * @param events A history collection of logged messaging actions
 * containing direction, source channel, and raw contents.
 * @param onClear Callback triggered when the user requests flushing the active traffic events trace buffer.
 * @param onDismiss Callback triggered when closing the overlay interface or clicking out of its layout boundaries.
 */
@Composable
fun MqttReportsDialog(
    monitoredTopics: Set<String>,
    events: List<MqttTrafficEvent>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.admin_panel_mqtt_reports))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.admin_panel_mqtt_reports_topics_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                monitoredTopics.sorted().forEach { topic ->
                    Text(
                        text = stringResource(R.string.admin_panel_mqtt_reports_topic_item, topic),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.admin_panel_mqtt_reports_messages_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (events.isEmpty()) {
                    Text(
                        text = stringResource(R.string.admin_panel_mqtt_reports_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    events.asReversed().forEach { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatReportTimestamp(event.timestampEpochMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (event.direction == MqttTrafficDirection.INBOUND) {
                                    stringResource(R.string.admin_panel_mqtt_reports_direction_in)
                                } else {
                                    stringResource(R.string.admin_panel_mqtt_reports_direction_out)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = event.topic,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = event.payload,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear) {
                    Text(stringResource(R.string.admin_panel_mqtt_reports_clear))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.admin_panel_close))
                }
            }
        }
    )
}

private fun formatReportTimestamp(timestampEpochMillis: Long): String {
    return Instant.ofEpochMilli(timestampEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(reportTimeFormatter)
}
