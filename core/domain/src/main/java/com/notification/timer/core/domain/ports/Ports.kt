package com.notification.timer.core.domain.ports

import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.Preset
import com.notification.timer.core.domain.model.Timer

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

interface LibraryRepository {
    suspend fun savePresets(presets: List<Preset>)

    suspend fun loadPresets(): List<Preset>
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
