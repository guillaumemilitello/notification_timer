package com.notification.timer.core.runtime.notification

import android.content.Context
import android.graphics.Color
import com.notification.timer.core.domain.engine.TimerEngine
import com.notification.timer.core.domain.engine.TimerStateMachine
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerAction
import com.notification.timer.core.domain.model.TimerState
import com.notification.timer.core.domain.ports.TimeSource
import com.notification.timer.core.domain.ports.TimerNotifierPort
import com.notification.timer.core.runtime.service.TimerForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

internal data class TimerNotificationSpec(
    val timerId: Int,
    val title: String,
    val subtitle: String,
    val durationMs: Long,
    val isRunning: Boolean,
    val showProgress: Boolean,
    val progressPermille: Int,
    val frozenTimeText: String?,
    val chronometerBaseElapsedMs: Long?,
    val showNext: Boolean,
    val isAlerting: Boolean,
    val isOngoing: Boolean,
    val colorTheme: NotificationColorTheme,
) {
    fun hasStructuralChangeFrom(other: TimerNotificationSpec): Boolean = copy(progressPermille = 0) != other.copy(progressPermille = 0)
}

@Singleton
class TimerNotifierAdapter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val renderer: NotificationRenderer,
        private val timeSource: TimeSource,
    ) : TimerNotifierPort {
        private data class PostedNotificationEntry(
            val spec: TimerNotificationSpec,
            val postedAtWallMs: Long,
            val lastPostedElapsedMs: Long,
        )

        private val postedEntries = LinkedHashMap<Int, PostedNotificationEntry>()
        private var currentForegroundTimerId: Int? = null

        override fun reconcileRestoredNotifications(
            timers: List<Timer>,
            newlyCompletedIds: Set<Int>,
        ) {
            val nowElapsedMs = timeSource.elapsedRealtimeMs()
            val nowWallMs = timeSource.currentTimeMillis()
            val systemActiveIds = renderer.getSystemActiveNotificationIds()
            val visibleTimerIds = timers.filter { it.isNotificationVisible }.map { it.id }.toSet()

            // Cancel any orphaned OS notifications from deleted or hidden timers
            for (orphanId in (systemActiveIds + postedEntries.keys) - visibleTimerIds) {
                postedEntries.remove(orphanId)
                renderer.cancelNotification(orphanId)
            }

            for (timer in timers) {
                if (!timer.isNotificationVisible) {
                    postedEntries.remove(timer.id)
                    continue
                }

                val spec = buildSpec(timer, nowElapsedMs)
                val shouldPostOnRestore =
                    timer.state is TimerState.Running ||
                        timer.id in newlyCompletedIds ||
                        timer.id in systemActiveIds

                if (shouldPostOnRestore) {
                    val notification = renderFromSpec(timer, spec, nowElapsedMs, nowWallMs)
                    renderer.postNotification(timer.id, notification)
                }

                // Seed entry whether already active in SystemUI or previously dismissed while dead,
                // ensuring actions on OTHER timers never re-post this timer's notification.
                postedEntries[timer.id] =
                    PostedNotificationEntry(
                        spec = spec,
                        postedAtWallMs = nowWallMs,
                        lastPostedElapsedMs = nowElapsedMs,
                    )
            }

            val targetForegroundId = selectTargetForegroundTimerId(timers)
            syncForegroundService(targetForegroundId)
        }

        override fun updateNotifications(timers: List<Timer>) {
            val nowElapsedMs = timeSource.elapsedRealtimeMs()
            val nowWallMs = timeSource.currentTimeMillis()
            val currentTimerIds = timers.map { it.id }.toSet()

            // 1. Cancel notifications for deleted timers only
            val deletedIds = postedEntries.keys.filter { it !in currentTimerIds }
            for (id in deletedIds) {
                postedEntries.remove(id)
                renderer.cancelNotification(id)
            }

            // 2. Cancel notifications for timers whose notification was dismissed/closed/reset
            for (timer in timers) {
                if (!timer.isNotificationVisible && postedEntries.remove(timer.id) != null) {
                    renderer.cancelNotification(timer.id)
                }
            }

            // 3. Determine sticky foreground timer strictly among Running visible timers
            val targetForegroundId = selectTargetForegroundTimerId(timers)
            val runningCount = timers.count { it.isNotificationVisible && it.state is TimerState.Running }
            val minProgressRefreshMs = TimerEngine.refreshIntervalMsForRunningCount(runningCount)

            // 4. Update ONLY timers whose own notification spec actually changed
            for (timer in timers) {
                if (!timer.isNotificationVisible) continue

                val spec = buildSpec(timer, nowElapsedMs)
                val previous = postedEntries[timer.id]

                val shouldPost =
                    when {
                        previous == null -> true
                        spec.hasStructuralChangeFrom(previous.spec) -> true
                        spec.isRunning &&
                            spec.progressPermille != previous.spec.progressPermille &&
                            (nowElapsedMs - previous.lastPostedElapsedMs >= minProgressRefreshMs) -> true
                        else -> false
                    }

                if (shouldPost) {
                    val postedAtWallMs = previous?.postedAtWallMs ?: nowWallMs
                    val notification = renderFromSpec(timer, spec, nowElapsedMs, postedAtWallMs)
                    renderer.postNotification(timer.id, notification)
                    postedEntries[timer.id] =
                        PostedNotificationEntry(
                            spec = spec,
                            postedAtWallMs = postedAtWallMs,
                            lastPostedElapsedMs = nowElapsedMs,
                        )
                }
            }

            syncForegroundService(targetForegroundId)
        }

        private fun selectTargetForegroundTimerId(timers: List<Timer>): Int? {
            val runningVisibleTimers =
                timers.filter { it.isNotificationVisible && it.state is TimerState.Running }
            if (runningVisibleTimers.isEmpty()) return null

            val currentFgId = currentForegroundTimerId
            if (currentFgId != null && runningVisibleTimers.any { it.id == currentFgId }) {
                return currentFgId
            }

            return runningVisibleTimers
                .minByOrNull { (it.state as TimerState.Running).endsAtElapsedMs }
                ?.id
        }

        private fun buildSpec(
            timer: Timer,
            nowElapsedMs: Long,
        ): TimerNotificationSpec {
            val remainingMs = timer.remainingMs(nowElapsedMs)
            val durationMs = timer.config.durationSec * 1000L
            val actions = TimerStateMachine.actionsFor(timer)
            val state = timer.state
            val isRunning = state is TimerState.Running
            val isOngoing = state is TimerState.Running || state is TimerState.Paused
            val showProgress = state is TimerState.Running || state is TimerState.Paused
            val showNext = TimerAction.NEXT_SET in actions
            val isAlerting = state is TimerState.SetDone || state is TimerState.AllSetsDone
            val chronometerBaseElapsedMs =
                if (state is TimerState.Running && remainingMs > 1000L) {
                    state.endsAtElapsedMs
                } else {
                    null
                }

            val progressPermille =
                if (showProgress && durationMs > 0) {
                    (((durationMs - remainingMs).toDouble() * NotificationRenderer.PROGRESS_MAX) / durationMs)
                        .toInt()
                        .coerceIn(0, NotificationRenderer.PROGRESS_MAX)
                } else {
                    0
                }

            val colorHex = TimerStateMachine.computePhaseColor(timer, nowElapsedMs)
            val parsedColor =
                try {
                    Color.parseColor(colorHex)
                } catch (_: Exception) {
                    Color.parseColor("#7a82b7")
                }

            val colorTheme: NotificationColorTheme =
                when (state) {
                    is TimerState.Running, is TimerState.Paused -> NotificationColorTheme.Active(parsedColor)
                    is TimerState.Ready, is TimerState.SetDone, is TimerState.AllSetsDone -> NotificationColorTheme.SystemDefault
                }

            val setLabel =
                when {
                    timer.config.sets != null && timer.config.sets!! > 1 ->
                        "Set ${timer.currentSet}/${timer.config.sets}"
                    timer.config.sets == null && timer.currentSet > 1 ->
                        "Set ${timer.currentSet}"
                    else -> null
                }

            val subtitle =
                when (val timerState = timer.state) {
                    is TimerState.Ready -> {
                        val prefix = if (timerState.isCompleted) "Done" else "Ready"
                        if (setLabel != null) "$prefix · $setLabel" else prefix
                    }
                    is TimerState.Running -> setLabel ?: ""
                    is TimerState.Paused -> if (setLabel != null) "$setLabel · Paused" else "Paused"
                    is TimerState.SetDone -> "${setLabel ?: "Set ${timer.currentSet}"} complete"
                    is TimerState.AllSetsDone -> timer.config.done.name
                }

            val frozenTimeText =
                if (isRunning && remainingMs > 1000L) {
                    null
                } else if (isRunning && remainingMs in 1..1000L) {
                    "00:00"
                } else {
                    NotificationRenderer.formatDisplayTime(remainingMs)
                }

            return TimerNotificationSpec(
                timerId = timer.id,
                title = timer.label,
                subtitle = subtitle,
                durationMs = durationMs,
                isRunning = isRunning,
                showProgress = showProgress,
                progressPermille = progressPermille,
                frozenTimeText = frozenTimeText,
                chronometerBaseElapsedMs = chronometerBaseElapsedMs,
                showNext = showNext,
                isAlerting = isAlerting,
                isOngoing = isOngoing,
                colorTheme = colorTheme,
            )
        }

        private fun renderFromSpec(
            timer: Timer,
            spec: TimerNotificationSpec,
            nowElapsedMs: Long,
            postedAtWallMs: Long,
        ) = renderer.renderTimerNotification(
            TimerNotificationModel(
                timerId = spec.timerId,
                title = spec.title,
                subtitle = spec.subtitle,
                remainingMs = timer.remainingMs(nowElapsedMs),
                durationMs = spec.durationMs,
                isRunning = spec.isRunning,
                showProgress = spec.showProgress,
                showNext = spec.showNext,
                isAlerting = spec.isAlerting,
                isOngoing = spec.isOngoing,
                chronometerBaseElapsedMs = spec.chronometerBaseElapsedMs,
                colorTheme = spec.colorTheme,
                postedAtWallMs = postedAtWallMs,
            ),
        )

        private fun syncForegroundService(targetForegroundId: Int?) {
            if (targetForegroundId != null) {
                if (currentForegroundTimerId != targetForegroundId) {
                    currentForegroundTimerId = targetForegroundId
                    runCatching {
                        TimerForegroundService.start(context, targetForegroundId)
                    }
                }
            } else if (currentForegroundTimerId != null) {
                currentForegroundTimerId = null
                runCatching {
                    TimerForegroundService.stop(context)
                }
            }
        }
    }
