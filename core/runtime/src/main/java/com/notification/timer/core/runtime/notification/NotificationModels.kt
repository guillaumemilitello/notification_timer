package com.notification.timer.core.runtime.notification

sealed interface NotificationColorTheme {
    /**
     * Used for Running, Paused, and Ready (stopped) timers.
     * Carries the timer's active phase color (runningColor or pre-end Alert.color).
     */
    data class Active(
        val colorInt: Int,
    ) : NotificationColorTheme

    /**
     * Used when the timer is Done (Ready(isCompleted=true), SetDone, AllSetsDone).
     * Uses system default notification colors.
     */
    data object SystemDefault : NotificationColorTheme
}

data class TimerNotificationModel(
    val timerId: Int,
    val title: String,
    val subtitle: String,
    val remainingMs: Long,
    val durationMs: Long,
    val isRunning: Boolean,
    val showProgress: Boolean = isRunning,
    val showNext: Boolean = true,
    val isAlerting: Boolean = false,
    val isOngoing: Boolean = true,
    val chronometerBaseElapsedMs: Long? = null,
    val colorTheme: NotificationColorTheme = NotificationColorTheme.SystemDefault,
    val postedAtWallMs: Long = System.currentTimeMillis(),
)
