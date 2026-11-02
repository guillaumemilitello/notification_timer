package com.notification.timer.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Alert(
    val name: String,
    val beforeEndSec: Int,
    val color: String,
    val sound: String? = null,
    val soundEnabled: Boolean = true,
    val vibrate: Boolean = true,
)

@Serializable
data class TimerConfig(
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
            sound = null,
            soundEnabled = true,
            vibrate = true,
        ),
) {
    /**
     * Stable fingerprint representing the timer's functional parameters.
     * Two timers with the same name, duration, sets, color, alerts, sound, and vibration have the same fingerprint.
     */
    fun fingerprint(): String =
        buildString {
            append(name.trim().lowercase())
            append('|').append(durationSec)
            append('|').append(sets ?: 0)
            append('|').append(runningColor.trim().uppercase())
            for (alert in alerts) {
                append('|')
                    .append(alert.name)
                    .append(':')
                    .append(alert.beforeEndSec)
                    .append(':')
                    .append(alert.color)
                    .append(':')
                    .append(alert.sound ?: "")
                    .append(':')
                    .append(alert.soundEnabled)
                    .append(':')
                    .append(alert.vibrate)
            }
            append('|')
                .append(done.name)
                .append(':')
                .append(done.beforeEndSec)
                .append(':')
                .append(done.color)
                .append(':')
                .append(done.sound ?: "")
                .append(':')
                .append(done.soundEnabled)
                .append(':')
                .append(done.vibrate)
        }
}

data class HistoryEntry(
    val timerId: String,
    val config: TimerConfig,
    val useCount: Int,
    val lastUsedAtMs: Long,
    val isStarred: Boolean,
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
    @SerialName("configId")
    val configId: String? = null,
    val config: TimerConfig,
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
