package com.notification.timer.feature.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.engine.TimerEngine
import com.notification.timer.core.domain.model.HistoryEntry
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.model.TimerConfig
import com.notification.timer.core.domain.ports.DisplayPreferences
import com.notification.timer.core.domain.ports.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimersViewModel
    @Inject
    constructor(
        private val timerEngine: TimerEngine,
        private val historyRepository: HistoryRepository,
        private val displayPreferences: DisplayPreferences,
        private val eventLog: EventLog? = null,
    ) : ViewModel() {
        val timers: StateFlow<List<Timer>> = timerEngine.timers

        val history: StateFlow<List<HistoryEntry>> =
            historyRepository.history.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        private val _keepScreenOn = MutableStateFlow(false)

        /** User-controlled; only [toggleKeepScreenOn] changes it. */
        val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

        private val keepScreenOnLoad: Job =
            viewModelScope.launch {
                _keepScreenOn.value = displayPreferences.loadKeepScreenOn()
            }

        fun toggleKeepScreenOn() {
            viewModelScope.launch {
                // Wait for the persisted value so an early tap is never overwritten by the initial load.
                keepScreenOnLoad.join()
                val enabled = !_keepScreenOn.value
                _keepScreenOn.value = enabled
                eventLog?.log("TimersViewModel", "toggleKeepScreenOn: $enabled")
                displayPreferences.saveKeepScreenOn(enabled)
            }
        }

        fun startTimer(config: TimerConfig) {
            eventLog?.log("TimersViewModel", "startTimer: ${config.name} (${config.durationSec}s)")
            timerEngine.submitCommand(TimerCommand.Create(config, startImmediately = true))
            viewModelScope.launch {
                historyRepository.recordTimerUsed(config, System.currentTimeMillis())
            }
        }

        fun toggleHistoryStar(timerId: String) {
            viewModelScope.launch {
                historyRepository.toggleStar(timerId)
            }
        }

        fun updateTimerSettings(
            timerId: Int,
            name: String,
            colorHex: String,
            sets: Int?,
            soundEnabled: Boolean,
            soundUri: String?,
            vibrate: Boolean,
        ) {
            val timer = timers.value.firstOrNull { it.id == timerId }
            if (name.isNotBlank() && (timer == null || name != timer.label)) {
                handleCommand(TimerCommand.Rename(timerId, name.trim()))
            }
            if (timer == null || colorHex != timer.config.runningColor) {
                handleCommand(TimerCommand.UpdateColor(timerId, colorHex))
            }
            if (timer == null || sets != timer.config.sets) {
                handleCommand(TimerCommand.UpdateSets(timerId, sets))
            }
            if (timer == null ||
                soundEnabled != timer.config.done.soundEnabled ||
                soundUri != timer.config.done.sound ||
                vibrate != timer.config.done.vibrate
            ) {
                handleCommand(
                    TimerCommand.UpdateAlertSettings(
                        timerId = timerId,
                        soundEnabled = soundEnabled,
                        soundUri = soundUri,
                        vibrate = vibrate,
                    ),
                )
            }
            if (timer != null) {
                val updatedConfig =
                    timer.config.copy(
                        name = if (name.isNotBlank()) name.trim() else timer.config.name,
                        runningColor = colorHex,
                        sets = sets,
                        done =
                            timer.config.done.copy(
                                soundEnabled = soundEnabled,
                                sound = soundUri,
                                vibrate = vibrate,
                            ),
                    )
                viewModelScope.launch {
                    historyRepository.updateTimerConfig(updatedConfig)
                }
            }
        }

        fun updateTimerAlertSettings(
            timerId: Int,
            soundEnabled: Boolean,
            soundUri: String?,
            vibrate: Boolean,
        ) {
            val timer = timers.value.firstOrNull { it.id == timerId }
            updateTimerSettings(
                timerId = timerId,
                name = timer?.label ?: "",
                colorHex = timer?.config?.runningColor ?: "#3F51B5",
                sets = timer?.config?.sets,
                soundEnabled = soundEnabled,
                soundUri = soundUri,
                vibrate = vibrate,
            )
        }

        fun handleCommand(command: TimerCommand) {
            eventLog?.log("TimersViewModel", "handleCommand: $command")
            timerEngine.submitCommand(command)
        }
    }
