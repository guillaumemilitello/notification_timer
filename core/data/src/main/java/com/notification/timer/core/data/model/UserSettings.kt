package com.notification.timer.core.data.model

import com.notification.timer.core.domain.model.TimerConfig
import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val timeIncrementSec: Int = 20,
    val timeDecrementSec: Int = 20,
    val stepTimeSec: Int = -20,
    val runningColor: String = "#3F51B5",
    val defaultAlertEnabled: Boolean = false,
    val defaultAlertBeforeEndSec: Int = 15,
    val defaultAlertColor: String = "#FF9800",
    val doneAlertColor: String = "#4CAF50",
    val defaultSets: Int? = null,
    val showSetCounter: Boolean = true,
    val showTimerName: Boolean = true,
    val coloredNotifications: Boolean = true,
    val theme: String = "system",
    val askBeforeClearingRunning: Boolean = true,
    val keepScreenOn: Boolean = false,
    val maxHistorySize: Int = 10,
)

@Serializable
data class HistoryRecord(
    val timerId: String,
    val useCount: Int = 1,
    val lastUsedAtMs: Long,
    val isStarred: Boolean = false,
)

@Serializable
data class LibraryData(
    val schemaVersion: Int = 2,
    val settings: UserSettings = UserSettings(),
    val timersById: Map<String, TimerConfig> = emptyMap(),
    val history: List<HistoryRecord> = emptyList(),
    // Legacy field for automatic migration during app upgrade
    val presets: List<TimerConfig> = emptyList(),
)

@Serializable
data class RuntimeData(
    val schemaVersion: Int = 1,
    val nextTimerId: Int = 1000,
    val timers: List<com.notification.timer.core.domain.model.Timer> = emptyList(),
)

@Serializable
data class ExportData(
    val schemaVersion: Int = 2,
    val appVersion: String = "2.0.0",
    val exportedAt: String,
    val settings: UserSettings = UserSettings(),
    val timersById: Map<String, TimerConfig> = emptyMap(),
    val history: List<HistoryRecord> = emptyList(),
    // Optional legacy list for importing older v1 backups
    val presets: List<TimerConfig> = emptyList(),
)
