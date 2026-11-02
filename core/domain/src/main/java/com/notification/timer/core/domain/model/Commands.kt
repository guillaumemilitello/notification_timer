package com.notification.timer.core.domain.model

sealed interface TimerCommand {
    data class Create(
        val config: TimerConfig,
        val startImmediately: Boolean = false,
    ) : TimerCommand

    data class Start(
        val timerId: Int,
    ) : TimerCommand

    data class Pause(
        val timerId: Int,
    ) : TimerCommand

    data class Resume(
        val timerId: Int,
    ) : TimerCommand

    data class AdjustTime(
        val timerId: Int,
        val deltaSec: Int,
    ) : TimerCommand

    data class StepTime(
        val timerId: Int,
        val deltaSec: Int,
    ) : TimerCommand

    data class NextSet(
        val timerId: Int,
    ) : TimerCommand

    data class StartNextSet(
        val timerId: Int,
    ) : TimerCommand

    data class Stop(
        val timerId: Int,
    ) : TimerCommand

    data class ExtraSet(
        val timerId: Int,
    ) : TimerCommand

    data class Reset(
        val timerId: Int,
    ) : TimerCommand

    data class Clear(
        val timerId: Int,
    ) : TimerCommand

    data class Rename(
        val timerId: Int,
        val newName: String,
    ) : TimerCommand

    data class UpdateColor(
        val timerId: Int,
        val colorHex: String,
    ) : TimerCommand

    data class UpdateSets(
        val timerId: Int,
        val sets: Int?,
    ) : TimerCommand

    data class UpdateAlertSettings(
        val timerId: Int,
        val soundEnabled: Boolean,
        val soundUri: String?,
        val vibrate: Boolean,
    ) : TimerCommand

    data class Reorder(
        val fromIndex: Int,
        val toIndex: Int,
    ) : TimerCommand

    data class DismissNotification(
        val timerId: Int,
    ) : TimerCommand

    data class CloseNotification(
        val timerId: Int,
    ) : TimerCommand

    data object Wakeup : TimerCommand
}

enum class TimerAction {
    START,
    PAUSE,
    RESUME,
    ADJUST_TIME,
    STEP_TIME,
    NEXT_SET,
    START_NEXT_SET,
    STOP,
    EXTRA_SET,
    RESET,
    CLEAR,
    RENAME,
}
