package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DialogPatrolMode { RANDOM, FIXED }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatrolSettingsDialog(
    initialIsEnabled: Boolean = false,
    initialMode: DialogPatrolMode = DialogPatrolMode.RANDOM,
    initialMinMinutes: Int = 40,
    initialMaxMinutes: Int = 60,
    initialHours: Set<Int> = emptySet(),
    onTriggerPatrol: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (isEnabled: Boolean, mode: DialogPatrolMode, minMin: Int, maxMin: Int, hours: Set<Int>) -> Unit
) {
    var isEnabled by remember { mutableStateOf(initialIsEnabled) }
    var selectedMode by remember { mutableStateOf(initialMode) }
    var minMinutes by remember { mutableFloatStateOf(initialMinMinutes.toFloat()) }
    var maxMinutes by remember { mutableFloatStateOf(initialMaxMinutes.toFloat()) }
    var selectedHours by remember { mutableStateOf(initialHours) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Kontrollfahrten konfigurieren",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Wähle aus, wie die automatischen Rundgänge zur Überprüfung des Wohlbefindens geplant werden sollen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEnabled = !isEnabled },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Automatische Fahrten aktivieren",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                if (isEnabled) {
                    Button(
                        onClick = {
                            onTriggerPatrol()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Jetzt eine Kontrollfahrt starten")
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Planungs-Modus", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = DialogPatrolMode.RANDOM },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == DialogPatrolMode.RANDOM,
                                onClick = { selectedMode = DialogPatrolMode.RANDOM }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Zufalls-Intervall (z.B. alle 40-60 Min)")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = DialogPatrolMode.FIXED },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == DialogPatrolMode.FIXED,
                                onClick = { selectedMode = DialogPatrolMode.FIXED }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Feste Stunden (jede gewählte Stunde)")
                        }
                    }

                    if (selectedMode == DialogPatrolMode.RANDOM) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Intervall-Grenzen (Minuten)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                            Text("Minimaler Abstand: ${minMinutes.toInt()} Min.")
                            Slider(
                                value = minMinutes,
                                onValueChange = { minMinutes = it.coerceAtMost(maxMinutes) },
                                valueRange = 10f..120f
                            )

                            Text("Maximaler Abstand: ${maxMinutes.toInt()} Min.")
                            Slider(
                                value = maxMinutes,
                                onValueChange = { maxMinutes = it.coerceAtLeast(minMinutes) },
                                valueRange = 10f..120f
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Uhrzeiten auswählen (Ganze Stunden)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (hour in 0..23) {
                                    val isSelected = hour in selectedHours
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedHours = if (isSelected) selectedHours - hour else selectedHours + hour
                                        },
                                        label = { Text(text = String.format("%02d:00", hour)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(isEnabled, selectedMode, minMinutes.toInt(), maxMinutes.toInt(), selectedHours)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
