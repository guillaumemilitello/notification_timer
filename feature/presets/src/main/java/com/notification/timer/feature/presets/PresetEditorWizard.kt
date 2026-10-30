@file:Suppress("ktlint:standard:function-naming")

package com.notification.timer.feature.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.Preset
import java.util.UUID

@Composable
fun PresetEditorWizard(
    initialPreset: Preset? = null,
    onSave: (Preset) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(1) }

    var name by remember { mutableStateOf(initialPreset?.name ?: "New Preset") }
    var durationSec by remember { mutableIntStateOf(initialPreset?.durationSec ?: 60) }
    var sets by remember { mutableStateOf(initialPreset?.sets) }
    var runningColor by remember { mutableStateOf(initialPreset?.runningColor ?: "#3F51B5") }
    var chipShows by remember { mutableStateOf(initialPreset?.chipShows ?: "time") }

    var alertsCount by remember { mutableIntStateOf(initialPreset?.alerts?.size ?: 0) }
    var alerts by remember { mutableStateOf(initialPreset?.alerts ?: emptyList()) }
    var doneAlert by remember {
        mutableStateOf(
            initialPreset?.done ?: Alert(
                name = "Time out!",
                beforeEndSec = 0,
                color = "#4CAF50",
                vibrate = true,
            ),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Preset Editor - Step $step of 6",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (step) {
            1 -> {
                Text("Step 1: Duration & Sets", style = MaterialTheme.typography.titleMedium)
                NumpadView(
                    onConfirm = { dur, st ->
                        durationSec = dur
                        sets = st
                        step = 2
                    },
                )
            }

            2 -> {
                Text("Step 2: Number of Alerts (0 to 5)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                for (count in 0..5) {
                    Button(
                        onClick = {
                            alertsCount = count
                            step = 3
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                    ) {
                        Text("$count Alerts")
                    }
                }
            }

            3 -> {
                Text("Step 3: Alert Configuration", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { step = 4 },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Next to Done Alert")
                }
            }

            4 -> {
                Text("Step 4: Done Alert", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = doneAlert.name,
                    onValueChange = { doneAlert = doneAlert.copy(name = it) },
                    label = { Text("Done Alert Name") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { step = 5 },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Next to Summary")
                }
            }

            5 -> {
                Text("Step 5: Summary & Save", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Name: $name")
                Text("Duration: ${durationSec}s")
                Text("Sets: ${sets ?: "Infinite"}")
                Text("Alerts: ${alerts.size}")

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val finalPreset =
                                Preset(
                                    id = initialPreset?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    durationSec = durationSec,
                                    sets = sets,
                                    runningColor = runningColor,
                                    chipShows = chipShows,
                                    alerts = alerts,
                                    done = doneAlert,
                                )
                            onSave(finalPreset)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save Preset")
                    }
                }
            }
        }
    }
}
