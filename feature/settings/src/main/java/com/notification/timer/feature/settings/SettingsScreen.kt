@file:Suppress("ktlint:standard:function-naming")

package com.notification.timer.feature.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notification.timer.core.ui.R as UiR

@Composable
fun SettingsScreen(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onHowItWorksClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxHistorySize: Int = 10,
    onMaxHistorySizeChange: (Int) -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(UiR.string.settings),
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Timer Controls Section
        Text("Timer Controls", style = MaterialTheme.typography.titleMedium)
        Text("Default step time: 20 seconds", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // History Configuration Section
        Text(stringResource(UiR.string.history_limit), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(UiR.string.history_limit_summary, maxHistorySize),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (maxHistorySize > 5) onMaxHistorySizeChange(maxHistorySize - 5) },
                    enabled = maxHistorySize > 5,
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease history limit")
                }
                Text(
                    text = maxHistorySize.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(
                    onClick = { if (maxHistorySize < 50) onMaxHistorySizeChange(maxHistorySize + 5) },
                    enabled = maxHistorySize < 50,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase history limit")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Backup / Export Section
        Text(stringResource(UiR.string.backup), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onExportClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(UiR.string.preferences_backup))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(UiR.string.preferences_restore))
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // About & How It Works
        Button(
            onClick = onHowItWorksClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("How It Works")
        }
    }
}
