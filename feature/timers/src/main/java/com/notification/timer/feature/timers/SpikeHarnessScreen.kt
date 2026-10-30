@file:Suppress("ktlint:standard:function-naming")

package com.notification.timer.feature.timers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SpikeHarnessScreen(
    viewModel: SpikeHarnessViewModel,
    modifier: Modifier = Modifier,
) {
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            text = "Phase 1 Notification & Wakeup Spike Harness",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { viewModel.addTimer() }) {
                Text("Add Timer (${timers.size}/5)")
            }

            OutlinedButton(onClick = { viewModel.clearTimers() }) {
                Text("Clear Timers")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { viewModel.triggerWakeupAlarm(10) }) {
                Text("Trigger Alarm (10s)")
            }

            OutlinedButton(onClick = { viewModel.clearLogs() }) {
                Text("Clear Log")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Active Fake Timers",
            style = MaterialTheme.typography.titleSmall,
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(timers, key = { it.id }) { timer ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${timer.name} - ${if (timer.isRunning) "Running" else "Paused"}")
                        Button(onClick = { viewModel.toggleStartPause(timer.id) }) {
                            Text(if (timer.isRunning) "Pause" else "Start")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Diagnostics Log (Last 1000 entries)",
            style = MaterialTheme.typography.titleSmall,
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(logEntries) { entry ->
                Text(
                    text = "[${entry.tag}] ${entry.message}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
