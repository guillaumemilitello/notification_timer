package com.notification.timer.core.domain.engine

import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerState

object EventPlanner {
    /**
     * Calculates the earliest elapsedRealtimeMs when the next alert or set end will fire across all timers.
     * Returns null if no running timers exist or no upcoming events are scheduled.
     */
    fun findEarliestNextEventMs(
        timers: List<Timer>,
        nowElapsedMs: Long,
    ): Long? {
        var earliestMs: Long? = null

        for (timer in timers) {
            val state = timer.state as? TimerState.Running ?: continue
            val remainingMs = state.endsAtElapsedMs - nowElapsedMs
            if (remainingMs <= 0) {
                // Event is due immediately
                return nowElapsedMs
            }

            // Check set end
            val timerEndMs = state.endsAtElapsedMs
            earliestMs = minOfNull(earliestMs, timerEndMs)

            // Check 1s notification freeze point before end (to swap Chronometer with static 00:00)
            val freezeMs = state.endsAtElapsedMs - 1000L
            if (freezeMs > nowElapsedMs) {
                earliestMs = minOfNull(earliestMs, freezeMs)
            }

            // Check unfired alerts
            val remainingSec = ((remainingMs + 999) / 1000).toInt()
            for (alert in timer.config.alerts) {
                if (alert.beforeEndSec >= timer.config.durationSec) continue
                if (alert.name !in state.firedAlertNames && remainingSec > alert.beforeEndSec) {
                    val alertFireElapsedMs = state.endsAtElapsedMs - alert.beforeEndSec * 1000L
                    earliestMs = minOfNull(earliestMs, alertFireElapsedMs)
                }
            }
        }

        return earliestMs
    }

    private fun minOfNull(
        a: Long?,
        b: Long,
    ): Long = if (a == null) b else kotlin.math.min(a, b)
}
