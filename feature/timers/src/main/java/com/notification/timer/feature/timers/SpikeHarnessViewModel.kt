package com.notification.timer.feature.timers

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.diagnostics.LogEntry
import com.notification.timer.core.runtime.notification.NotificationRenderer
import com.notification.timer.core.runtime.service.TimerForegroundService
import com.notification.timer.core.runtime.wakeup.WakeupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FakeTimer(
    val id: Int,
    val name: String,
    val durationMs: Long = 60000L,
    var remainingMs: Long = 60000L,
    var isRunning: Boolean = false,
    var phaseColor: Int = Color.parseColor("#7a82b7"),
)

@HiltViewModel
class SpikeHarnessViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val notificationRenderer: NotificationRenderer,
        private val wakeupScheduler: WakeupScheduler,
        private val eventLog: EventLog,
    ) : ViewModel() {
        private val _timers = MutableStateFlow<List<FakeTimer>>(emptyList())
        val timers: StateFlow<List<FakeTimer>> = _timers.asStateFlow()

        private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
        val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()

        init {
            refreshLogs()
        }

        fun addTimer() {
            val currentList = _timers.value
            if (currentList.size >= 5) {
                eventLog.log("SpikeHarness", "Soft limit of 5 timers reached")
                refreshLogs()
                return
            }

            val newId = 1000 + currentList.size + 1
            val newTimer =
                FakeTimer(
                    id = newId,
                    name = "Timer $newId",
                    durationMs = 60000L,
                    remainingMs = 60000L,
                    phaseColor =
                        when (currentList.size % 4) {
                            0 -> Color.parseColor("#7a82b7") // Indigo
                            1 -> Color.parseColor("#ffab40") // Orange alert
                            2 -> Color.parseColor("#00fa76") // Green done
                            else -> Color.parseColor("#424242") // Charcoal
                        },
                )
            _timers.value = currentList + newTimer
            eventLog.log("SpikeHarness", "Added FakeTimer $newId")
            refreshLogs()
        }

        fun toggleStartPause(timerId: Int) {
            _timers.value =
                _timers.value.map { timer ->
                    if (timer.id == timerId) {
                        val updatedRunning = !timer.isRunning
                        timer.copy(isRunning = updatedRunning)
                    } else {
                        timer
                    }
                }

            val targetTimer = _timers.value.firstOrNull { it.id == timerId } ?: return
            val notif =
                notificationRenderer.renderTimerNotification(
                    timerId = targetTimer.id,
                    title = targetTimer.name,
                    subtitle = if (targetTimer.isRunning) "Running" else "Paused",
                    remainingMs = targetTimer.remainingMs,
                    durationMs = targetTimer.durationMs,
                    isRunning = targetTimer.isRunning,
                    phaseColor = targetTimer.phaseColor,
                )
            notificationRenderer.postNotification(timerId, notif)

            if (_timers.value.any { it.isRunning }) {
                TimerForegroundService.start(context, timerId)
            } else {
                TimerForegroundService.stop(context)
            }
            refreshLogs()
        }

        fun triggerWakeupAlarm(secondsFromNow: Int) {
            val targetMs = System.currentTimeMillis() + secondsFromNow * 1000L
            wakeupScheduler.scheduleWakeup(targetMs)
            refreshLogs()
        }

        fun clearTimers() {
            _timers.value.forEach { timer ->
                notificationRenderer.cancelNotification(timer.id)
            }
            _timers.value = emptyList()
            TimerForegroundService.stop(context)
            eventLog.log("SpikeHarness", "Cleared all fake timers")
            refreshLogs()
        }

        fun refreshLogs() {
            viewModelScope.launch {
                _logEntries.value = eventLog.getEntries().reversed()
            }
        }

        fun clearLogs() {
            eventLog.clear()
            refreshLogs()
        }
    }
