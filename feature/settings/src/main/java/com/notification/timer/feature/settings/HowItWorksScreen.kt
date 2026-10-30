@file:Suppress("ktlint:standard:function-naming")

package com.notification.timer.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HowItWorksScreen(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "How NotificationTimer Works",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Presets & Live Timers",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                "Presets are saved templates. Tapping a preset creates a live timer on its own tab. " +
                    "You can run up to 5 timers concurrently.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Notification Controls",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                "Each running timer displays its own interactive notification on the lock screen " +
                    "with Play/Pause, ±Time, and Clear/Reset buttons.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Alerts & Phase Colors",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                "Each timer can have up to 5 alerts before the end. When an alert triggers, " +
                    "its phase color updates and the sound plays once.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
