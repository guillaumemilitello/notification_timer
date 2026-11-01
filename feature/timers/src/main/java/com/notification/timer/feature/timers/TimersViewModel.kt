package com.notification.timer.feature.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.engine.TimerEngine
import com.notification.timer.core.domain.model.Preset
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.ports.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimersViewModel
    @Inject
    constructor(
        private val timerEngine: TimerEngine,
        private val libraryRepository: LibraryRepository,
        private val eventLog: EventLog? = null,
    ) : ViewModel() {
        val timers: StateFlow<List<Timer>> =
            timerEngine.timers.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        private val _presets = MutableStateFlow<List<Preset>>(emptyList())
        val presets: StateFlow<List<Preset>> = _presets

        init {
            viewModelScope.launch {
                _presets.value = libraryRepository.loadPresets()
            }
        }

        fun startTimerFromPreset(preset: Preset) {
            eventLog?.log("TimersViewModel", "startTimerFromPreset: ${preset.name} (${preset.durationSec}s)")
            timerEngine.submitCommand(TimerCommand.Create(preset, startImmediately = true))
        }

        fun handleCommand(command: TimerCommand) {
            eventLog?.log("TimersViewModel", "handleCommand: $command")
            timerEngine.submitCommand(command)
        }
    }
