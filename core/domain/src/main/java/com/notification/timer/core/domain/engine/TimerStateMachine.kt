package com.notification.timer.core.domain.engine

import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerAction
import com.notification.timer.core.domain.model.TimerState

object TimerStateMachine {
    const val MAX_DURATION_SEC = 359999 // 99:59:59 in seconds

    fun actionsFor(timer: Timer): Set<TimerAction> {
        val actions = mutableSetOf<TimerAction>()
        when (val state = timer.state) {
            is TimerState.Ready -> {
                actions.add(TimerAction.START)
                if (timer.currentSet > 1 || state.isCompleted) actions.add(TimerAction.RESET)
                actions.add(TimerAction.CLEAR)
                actions.add(TimerAction.RENAME)
            }

            is TimerState.Running -> {
                actions.add(TimerAction.PAUSE)
                actions.add(TimerAction.ADJUST_TIME)
                actions.add(TimerAction.STEP_TIME)
                actions.add(TimerAction.NEXT_SET)
                actions.add(TimerAction.CLEAR)
                actions.add(TimerAction.RENAME)
            }

            is TimerState.Paused -> {
                actions.add(TimerAction.RESUME)
                actions.add(TimerAction.ADJUST_TIME)
                actions.add(TimerAction.NEXT_SET)
                actions.add(TimerAction.CLEAR)
                actions.add(TimerAction.RENAME)
            }

            is TimerState.SetDone -> {
                actions.add(TimerAction.START_NEXT_SET)
                actions.add(TimerAction.STOP)
                actions.add(TimerAction.CLEAR)
                actions.add(TimerAction.RENAME)
            }

            is TimerState.AllSetsDone -> {
                actions.add(TimerAction.EXTRA_SET)
                actions.add(TimerAction.RESET)
                actions.add(TimerAction.CLEAR)
                actions.add(TimerAction.RENAME)
            }
        }
        return actions
    }

    fun computePhaseColor(
        timer: Timer,
        nowElapsedMs: Long,
    ): String =
        when (val state = timer.state) {
            is TimerState.Ready -> if (state.isCompleted) timer.config.done.color else timer.config.runningColor
            is TimerState.SetDone, is TimerState.AllSetsDone -> timer.config.done.color
            is TimerState.Running, is TimerState.Paused -> {
                val remainingSec = ((timer.remainingMs(nowElapsedMs) + 999) / 1000).toInt()
                // Sort alerts by time before end descending (earliest alerts first)
                val sortedAlerts =
                    timer.config.alerts
                        .filter { it.beforeEndSec < timer.config.durationSec }
                        .sortedByDescending { it.beforeEndSec }

                val lastFiredAlert = sortedAlerts.firstOrNull { remainingSec <= it.beforeEndSec }
                lastFiredAlert?.color ?: timer.config.runningColor
            }
        }

    fun start(
        timer: Timer,
        nowElapsedMs: Long,
    ): Timer {
        if (timer.state !is TimerState.Ready) return timer
        val durationMs = timer.config.durationSec * 1000L
        return timer.copy(
            state =
                TimerState.Running(
                    endsAtElapsedMs = nowElapsedMs + durationMs,
                    firedAlertNames = emptySet(),
                ),
            isNotificationVisible = true,
        )
    }

    fun pause(
        timer: Timer,
        nowElapsedMs: Long,
    ): Timer {
        val state = timer.state as? TimerState.Running ?: return timer
        val remainingMs = (state.endsAtElapsedMs - nowElapsedMs).coerceAtLeast(0L)
        return timer.copy(
            state =
                TimerState.Paused(
                    remainingMs = remainingMs,
                    firedAlertNames = state.firedAlertNames,
                ),
            isNotificationVisible = true,
        )
    }

    fun resume(
        timer: Timer,
        nowElapsedMs: Long,
    ): Timer {
        val state = timer.state as? TimerState.Paused ?: return timer
        return timer.copy(
            state =
                TimerState.Running(
                    endsAtElapsedMs = nowElapsedMs + state.remainingMs,
                    firedAlertNames = state.firedAlertNames,
                ),
            isNotificationVisible = true,
        )
    }

    fun adjustTime(
        timer: Timer,
        deltaSec: Int,
        nowElapsedMs: Long,
    ): Timer {
        val deltaMs = deltaSec * 1000L
        val maxDurationMs = MAX_DURATION_SEC * 1000L

        return when (val state = timer.state) {
            is TimerState.Running -> {
                val currentRemainingMs = (state.endsAtElapsedMs - nowElapsedMs).coerceAtLeast(0L)
                val newRemainingMs = (currentRemainingMs + deltaMs).coerceIn(0L, maxDurationMs)
                if (newRemainingMs <= 0) {
                    finishSet(timer)
                } else {
                    timer.copy(
                        state =
                            state.copy(
                                endsAtElapsedMs = nowElapsedMs + newRemainingMs,
                            ),
                    )
                }
            }

            is TimerState.Paused -> {
                val newRemainingMs = (state.remainingMs + deltaMs).coerceIn(0L, maxDurationMs)
                if (newRemainingMs <= 0) {
                    finishSet(timer)
                } else {
                    timer.copy(
                        state =
                            state.copy(
                                remainingMs = newRemainingMs,
                            ),
                    )
                }
            }

            else -> timer
        }
    }

    fun nextSet(timer: Timer): Timer =
        timer.copy(
            currentSet = timer.currentSet + 1,
            state = TimerState.Ready(isCompleted = false),
        )

    fun startNextSet(
        timer: Timer,
        nowElapsedMs: Long,
    ): Timer {
        val next = nextSet(timer)
        return start(next, nowElapsedMs)
    }

    fun stop(timer: Timer): Timer =
        timer.copy(
            state = TimerState.Ready(isCompleted = false),
            isNotificationVisible = false,
        )

    fun extraSet(
        timer: Timer,
        nowElapsedMs: Long,
    ): Timer {
        val next =
            timer.copy(
                currentSet = timer.currentSet + 1,
                state = TimerState.Ready(isCompleted = false),
            )
        return start(next, nowElapsedMs)
    }

    fun reset(timer: Timer): Timer =
        timer.copy(
            currentSet = 1,
            state = TimerState.Ready(isCompleted = false),
            isNotificationVisible = false,
        )

    fun dismissNotification(timer: Timer): Timer = timer.copy(isNotificationVisible = false)

    fun closeNotification(timer: Timer): Timer =
        when (timer.state) {
            is TimerState.Running, is TimerState.Paused ->
                timer.copy(
                    state = TimerState.Ready(isCompleted = false),
                    isNotificationVisible = false,
                )

            else -> timer.copy(isNotificationVisible = false)
        }

    fun rename(
        timer: Timer,
        newName: String,
    ): Timer = timer.copy(label = newName)

    fun updateColor(
        timer: Timer,
        colorHex: String,
    ): Timer =
        timer.copy(
            config = timer.config.copy(runningColor = colorHex),
        )

    fun checkAlertsAndEnd(
        timer: Timer,
        nowElapsedMs: Long,
    ): Pair<Timer, List<Alert>> {
        val state = timer.state as? TimerState.Running ?: return Pair(timer, emptyList())
        val remainingMs = state.endsAtElapsedMs - nowElapsedMs

        if (remainingMs <= 0) {
            val finishedTimer = finishSet(timer)
            val lateMs = nowElapsedMs - state.endsAtElapsedMs
            val firedAlerts = if (lateMs < 60_000L) listOf(timer.config.done) else emptyList()
            return Pair(finishedTimer, firedAlerts)
        }

        val remainingSec = ((remainingMs + 999) / 1000).toInt()
        val firedAlerts = mutableListOf<Alert>()
        val updatedFiredNames = state.firedAlertNames.toMutableSet()

        for (alert in timer.config.alerts) {
            // Ignore alerts at or beyond duration
            if (alert.beforeEndSec >= timer.config.durationSec) continue

            if (remainingSec <= alert.beforeEndSec && alert.name !in updatedFiredNames) {
                val alertDueElapsedMs = state.endsAtElapsedMs - (alert.beforeEndSec * 1000L)
                val lateMs = nowElapsedMs - alertDueElapsedMs
                if (lateMs < 60_000L) {
                    firedAlerts.add(alert)
                }
                updatedFiredNames.add(alert.name)
            }
        }

        if (firedAlerts.isEmpty() && updatedFiredNames == state.firedAlertNames) return Pair(timer, emptyList())

        val updatedTimer =
            timer.copy(
                state =
                    state.copy(
                        firedAlertNames = updatedFiredNames,
                    ),
                isNotificationVisible = timer.isNotificationVisible || firedAlerts.isNotEmpty(),
            )
        return Pair(updatedTimer, firedAlerts)
    }

    private fun finishSet(timer: Timer): Timer {
        val totalSets = timer.config.sets
        val isLastSet = totalSets != null && timer.currentSet >= totalSets

        return if (isLastSet) {
            timer.copy(
                currentSet = 1,
                state = TimerState.Ready(isCompleted = true),
                isNotificationVisible = true,
            )
        } else {
            timer.copy(
                currentSet = timer.currentSet + 1,
                state = TimerState.Ready(isCompleted = true),
                isNotificationVisible = true,
            )
        }
    }
}
