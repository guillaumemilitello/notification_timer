package com.notification.timer.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Alert(
    val name: String,
    val beforeEndSec: Int,
    val color: String,
    val sound: String? = null,
    val vibrate: Boolean = true,
)

@Serializable
data class Preset(
    val id: String,
    val name: String,
    val durationSec: Int,
    val sets: Int? = null, // null means infinite
    val runningColor: String = "#3F51B5",
    val chipShows: String = "time", // "time" or "name"
    val alerts: List<Alert> = emptyList(),
    val done: Alert =
        Alert(
            name = "Time out!",
            beforeEndSec = 0,
            color = "#4CAF50",
            vibrate = true,
        ),
)

@Serializable
sealed interface TimerState {
    @Serializable
    data class Ready(
        val isCompleted: Boolean = false,
    ) : TimerState

    @Serializable
    data class Running(
        val endsAtElapsedMs: Long,
        val firedAlertNames: Set<String> = emptySet(),
    ) : TimerState

    @Serializable
    data class Paused(
        val remainingMs: Long,
        val firedAlertNames: Set<String> = emptySet(),
    ) : TimerState

    @Serializable
    data class SetDone(
        val finishedAtElapsedMs: Long,
    ) : TimerState

    @Serializable
    data class AllSetsDone(
        val finishedAtElapsedMs: Long,
    ) : TimerState
}

@Serializable
data class Timer(
    val id: Int,
    val presetId: String? = null,
    val config: Preset,
    val label: String,
    val currentSet: Int = 1,
    val state: TimerState = TimerState.Ready(),
    val isNotificationVisible: Boolean = false,
) {
    fun remainingMs(nowElapsedMs: Long): Long =
        when (state) {
            is TimerState.Ready -> config.durationSec * 1000L
            is TimerState.Running -> (state.endsAtElapsedMs - nowElapsedMs).coerceAtLeast(0L)
            is TimerState.Paused -> state.remainingMs.coerceAtLeast(0L)
            is TimerState.SetDone -> config.durationSec * 1000L
            is TimerState.AllSetsDone -> config.durationSec * 1000L
        }
}
