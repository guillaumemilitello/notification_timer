package com.notification.timer.core.domain.engine

import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.model.TimerConfig
import com.notification.timer.core.domain.model.TimerState
import com.notification.timer.core.domain.ports.AlertPlayerPort
import com.notification.timer.core.domain.ports.TimeSource
import com.notification.timer.core.domain.ports.TimerNotifierPort
import com.notification.timer.core.domain.ports.TimerStore
import com.notification.timer.core.domain.ports.WakeupSchedulerPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class TimerEngine(
    private val timeSource: TimeSource,
    private val timerStore: TimerStore,
    private val wakeupScheduler: WakeupSchedulerPort,
    private val alertPlayer: AlertPlayerPort,
    private val timerNotifier: TimerNotifierPort,
    private val eventLog: EventLog? = null,
) {
    companion object {
        const val NOTIFICATION_REFRESH_INTERVAL_MS = 2_000L
        const val MIN_TIMER_ID = 1000

        fun refreshIntervalMsForRunningCount(runningCount: Int): Long = maxOf(NOTIFICATION_REFRESH_INTERVAL_MS, runningCount * 500L)
    }

    private val _timers = MutableStateFlow<List<Timer>>(emptyList())
    val timers: StateFlow<List<Timer>> = _timers.asStateFlow()

    private val commandChannel = Channel<TimerCommand>(Channel.UNLIMITED)
    private var loopJob: Job? = null
    private var nextTimerId = MIN_TIMER_ID
    private var lastNotificationRefreshElapsedMs: Long = 0L

    fun start(scope: CoroutineScope) {
        loopJob =
            scope.launch {
                val nowElapsedMs = timeSource.elapsedRealtimeMs()

                // Restore and reconcile persisted timers and monotonic ID counter on process start
                val restored = timerStore.loadTimers()
                val persistedNextId = timerStore.loadNextTimerId()
                val maxExistingId = restored.maxOfOrNull { it.id } ?: (MIN_TIMER_ID - 1)
                nextTimerId = maxOf(MIN_TIMER_ID, persistedNextId, maxExistingId + 1)
                eventLog?.i(
                    "TimerEngine",
                    "Engine started: restored ${restored.size} timers, nextTimerId=$nextTimerId at nowMs=$nowElapsedMs",
                )

                if (restored.isNotEmpty()) {
                    var stateChanged = false
                    val newlyCompletedIds = mutableSetOf<Int>()
                    val reconciled =
                        restored.map { timer ->
                            if (timer.state is TimerState.Running) {
                                val (updatedTimer, alerts) = TimerStateMachine.checkAlertsAndEnd(timer, nowElapsedMs)
                                if (updatedTimer != timer) {
                                    stateChanged = true
                                    if (updatedTimer.state !is TimerState.Running && alerts.isNotEmpty()) {
                                        newlyCompletedIds.add(updatedTimer.id)
                                    }
                                }
                                for (alert in alerts) {
                                    alertPlayer.playAlert(alert)
                                }
                                updatedTimer
                            } else {
                                timer
                            }
                        }

                    _timers.value = reconciled

                    if (stateChanged) {
                        timerStore.saveRuntimeState(reconciled, nextTimerId)
                    }
                    scheduleNextWakeup(reconciled, nowElapsedMs)
                    timerNotifier.reconcileRestoredNotifications(reconciled, newlyCompletedIds)
                    lastNotificationRefreshElapsedMs = nowElapsedMs
                } else {
                    timerNotifier.reconcileRestoredNotifications(emptyList(), emptySet())
                }

                // Command processing loop
                launch {
                    for (command in commandChannel) {
                        processCommand(command)
                    }
                }

                // Ticker & event loop
                while (true) {
                    val nowMs = timeSource.elapsedRealtimeMs()
                    tick(nowMs)
                    val nextEventMs = EventPlanner.findEarliestNextEventMs(_timers.value, nowMs)
                    val delayMs =
                        if (nextEventMs != null) {
                            (nextEventMs - nowMs).coerceIn(10L, 500L)
                        } else {
                            500L
                        }
                    delay(delayMs.milliseconds)
                }
            }
    }

    fun submitCommand(command: TimerCommand) {
        eventLog?.log("TimerEngine", "submitCommand: $command")
        commandChannel.trySend(command)
    }

    private suspend fun processCommand(command: TimerCommand) {
        val nowElapsedMs = timeSource.elapsedRealtimeMs()
        eventLog?.log("TimerEngine", "processCommand: $command at nowMs=$nowElapsedMs")
        val currentList = _timers.value.toMutableList()

        when (command) {
            is TimerCommand.Create -> {
                var newTimer = createTimerFromConfig(command.config, currentList)
                if (command.startImmediately) {
                    newTimer = TimerStateMachine.start(newTimer, nowElapsedMs)
                }
                currentList.add(0, newTimer)
                eventLog?.log(
                    "TimerEngine",
                    "Created timer ${newTimer.id} ('${newTimer.label}'): state=${newTimer.state}",
                )
            }

            is TimerCommand.Start -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    val prev = currentList[index]
                    currentList[index] = TimerStateMachine.start(currentList[index], nowElapsedMs)
                    eventLog?.log(
                        "TimerEngine",
                        "Started timer ${command.timerId} ('${prev.label}'): ${prev.state} -> ${currentList[index].state}",
                    )
                }
            }

            is TimerCommand.Pause -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    val prev = currentList[index]
                    currentList[index] = TimerStateMachine.pause(currentList[index], nowElapsedMs)
                    eventLog?.log(
                        "TimerEngine",
                        "Paused timer ${command.timerId} ('${prev.label}'): ${prev.state} -> ${currentList[index].state}",
                    )
                }
            }

            is TimerCommand.Resume -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    val prev = currentList[index]
                    currentList[index] = TimerStateMachine.resume(currentList[index], nowElapsedMs)
                    eventLog?.log(
                        "TimerEngine",
                        "Resumed timer ${command.timerId} ('${prev.label}'): ${prev.state} -> ${currentList[index].state}",
                    )
                }
            }

            is TimerCommand.AdjustTime -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.adjustTime(currentList[index], command.deltaSec, nowElapsedMs)
                    eventLog?.d("TimerEngine", "Adjusted time for timer ${command.timerId} by ${command.deltaSec}s")
                }
            }

            is TimerCommand.StepTime -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.adjustTime(currentList[index], command.deltaSec, nowElapsedMs)
                    eventLog?.d("TimerEngine", "Stepped time for timer ${command.timerId} by ${command.deltaSec}s")
                }
            }

            is TimerCommand.NextSet -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.nextSet(currentList[index])
                    eventLog?.d("TimerEngine", "Advanced to next set for timer ${command.timerId}")
                }
            }

            is TimerCommand.StartNextSet -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.startNextSet(currentList[index], nowElapsedMs)
                    eventLog?.d("TimerEngine", "Started next set for timer ${command.timerId}")
                }
            }

            is TimerCommand.Stop -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.stop(currentList[index])
                    eventLog?.d("TimerEngine", "Stopped timer ${command.timerId}")
                }
            }

            is TimerCommand.ExtraSet -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.extraSet(currentList[index], nowElapsedMs)
                    eventLog?.d("TimerEngine", "Triggered extra set for timer ${command.timerId}")
                }
            }

            is TimerCommand.Reset -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.reset(currentList[index])
                    eventLog?.d("TimerEngine", "Reset timer ${command.timerId}")
                }
            }

            is TimerCommand.Clear -> {
                currentList.removeAll { it.id == command.timerId }
                eventLog?.d("TimerEngine", "Cleared timer ${command.timerId}")
            }

            is TimerCommand.DismissNotification -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.dismissNotification(currentList[index])
                    eventLog?.d("TimerEngine", "Dismissed notification for timer ${command.timerId}")
                }
            }

            is TimerCommand.CloseNotification -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.closeNotification(currentList[index])
                    eventLog?.d("TimerEngine", "Closed notification for timer ${command.timerId}")
                }
            }

            is TimerCommand.Rename -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.rename(currentList[index], command.newName)
                    eventLog?.d("TimerEngine", "Renamed timer ${command.timerId} to '${command.newName}'")
                }
            }

            is TimerCommand.UpdateColor -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.updateColor(currentList[index], command.colorHex)
                    eventLog?.d("TimerEngine", "Updated color for timer ${command.timerId} to ${command.colorHex}")
                }
            }

            is TimerCommand.UpdateSets -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] = TimerStateMachine.updateSets(currentList[index], command.sets)
                    eventLog?.d("TimerEngine", "Updated sets for timer ${command.timerId} to ${command.sets}")
                }
            }

            is TimerCommand.UpdateAlertSettings -> {
                val index = currentList.indexOfFirst { it.id == command.timerId }
                if (index != -1) {
                    currentList[index] =
                        TimerStateMachine.updateAlertSettings(
                            timer = currentList[index],
                            soundEnabled = command.soundEnabled,
                            soundUri = command.soundUri,
                            vibrate = command.vibrate,
                        )
                    eventLog?.d(
                        "TimerEngine",
                        "Updated alert settings for timer ${command.timerId} (soundEnabled=${command.soundEnabled}, vibrate=${command.vibrate})",
                    )
                }
            }

            is TimerCommand.Reorder -> {
                if (command.fromIndex in currentList.indices &&
                    command.toIndex in currentList.indices &&
                    command.fromIndex != command.toIndex
                ) {
                    val moved = currentList.removeAt(command.fromIndex)
                    currentList.add(command.toIndex, moved)
                    eventLog?.d("TimerEngine", "Reordered timer from ${command.fromIndex} to ${command.toIndex}")
                }
            }
        }

        _timers.value = currentList
        timerStore.saveRuntimeState(currentList, nextTimerId)
        scheduleNextWakeup(currentList, nowElapsedMs)
        timerNotifier.updateNotifications(currentList)
    }

    private suspend fun tick(nowElapsedMs: Long) {
        val currentList = _timers.value.toMutableList()
        var stateChanged = false
        var runningCount = 0

        for (i in currentList.indices) {
            val timer = currentList[i]
            if (timer.state is TimerState.Running) {
                val (updatedTimer, firedAlerts) = TimerStateMachine.checkAlertsAndEnd(timer, nowElapsedMs)
                if (updatedTimer != timer) {
                    currentList[i] = updatedTimer
                    stateChanged = true
                    eventLog?.log(
                        "TimerEngine",
                        "Timer ${updatedTimer.id} ('${updatedTimer.label}') state changed on tick to ${updatedTimer.state}" +
                            if (firedAlerts.isNotEmpty()) ", alerts=${firedAlerts.map { it.name }}" else "",
                    )
                    for (alert in firedAlerts) {
                        alertPlayer.playAlert(alert)
                    }
                }
                if (currentList[i].state is TimerState.Running) {
                    runningCount++
                }
            }
        }

        if (stateChanged) {
            _timers.value = currentList
            timerStore.saveRuntimeState(currentList, nextTimerId)
            scheduleNextWakeup(currentList, nowElapsedMs)
            timerNotifier.updateNotifications(currentList)
            lastNotificationRefreshElapsedMs = nowElapsedMs
        } else if (runningCount > 0 &&
            nowElapsedMs - lastNotificationRefreshElapsedMs >= refreshIntervalMsForRunningCount(runningCount)
        ) {
            // Refresh running notification progress bars without persisting state on ticks
            timerNotifier.updateNotifications(currentList)
            lastNotificationRefreshElapsedMs = nowElapsedMs
        }
    }

    private fun scheduleNextWakeup(
        timers: List<Timer>,
        nowElapsedMs: Long,
    ) {
        val earliestEventMs = EventPlanner.findEarliestNextEventMs(timers, nowElapsedMs)
        if (earliestEventMs != null) {
            wakeupScheduler.scheduleWakeup(earliestEventMs)
        } else {
            wakeupScheduler.cancelWakeup()
        }
    }

    private fun createTimerFromConfig(
        config: TimerConfig,
        existingTimers: List<Timer>,
    ): Timer {
        val id = nextTimerId++
        val existingLabels = existingTimers.map { it.label }.toSet()
        val label =
            if (config.name !in existingLabels) {
                config.name
            } else {
                var suffix = 2
                while ("${config.name} $suffix" in existingLabels) {
                    suffix++
                }
                "${config.name} $suffix"
            }

        return Timer(
            id = id,
            configId = config.id,
            config = config,
            label = label,
        )
    }
}
