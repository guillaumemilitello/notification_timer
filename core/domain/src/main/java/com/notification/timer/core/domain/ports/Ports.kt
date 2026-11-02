package com.notification.timer.core.domain.ports

import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.HistoryEntry
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerConfig
import kotlinx.coroutines.flow.Flow

interface TimeSource {
    fun elapsedRealtimeMs(): Long

    fun currentTimeMillis(): Long
}

interface TimerStore {
    suspend fun saveTimers(timers: List<Timer>)

    suspend fun loadTimers(): List<Timer>

    suspend fun saveRuntimeState(
        timers: List<Timer>,
        nextTimerId: Int,
    ) {
        saveTimers(timers)
    }

    suspend fun loadNextTimerId(): Int = 1000
}

interface HistoryRepository {
    val history: Flow<List<HistoryEntry>>

    val maxHistorySize: Flow<Int>

    suspend fun recordTimerUsed(
        config: TimerConfig,
        nowEpochMs: Long,
    )

    suspend fun updateTimerConfig(config: TimerConfig)

    suspend fun toggleStar(timerId: String)

    suspend fun setMaxHistorySize(maxSize: Int)

    suspend fun getMaxHistorySize(): Int
}

/**
 * User-controlled display preferences. Values only change on explicit user action;
 * the app never toggles them on its own.
 */
interface DisplayPreferences {
    suspend fun loadKeepScreenOn(): Boolean

    suspend fun saveKeepScreenOn(enabled: Boolean)
}

interface WakeupSchedulerPort {
    fun scheduleWakeup(eventTimeMs: Long)

    fun cancelWakeup()
}

interface AlertPlayerPort {
    fun playAlert(alert: Alert)
}

interface TimerNotifierPort {
    fun reconcileRestoredNotifications(
        timers: List<Timer>,
        newlyCompletedIds: Set<Int> = emptySet(),
    ) {
        updateNotifications(timers)
    }

    fun updateNotifications(timers: List<Timer>)
}
