package com.notification.timer.core.runtime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notification.timer.core.domain.engine.TimerEngine
import com.notification.timer.core.domain.engine.TimerStateMachine
import com.notification.timer.core.domain.model.TimerAction
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.runtime.notification.NotificationRenderer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var timerEngine: TimerEngine

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val timerId = intent.getIntExtra("timerId", -1)
        if (timerId == -1) return

        val timer = timerEngine.timers.value.firstOrNull { it.id == timerId } ?: return
        val actions = TimerStateMachine.actionsFor(timer)

        when (intent.action) {
            NotificationRenderer.ACTION_PLAY_PAUSE -> {
                when {
                    TimerAction.PAUSE in actions -> timerEngine.submitCommand(TimerCommand.Pause(timerId))
                    TimerAction.RESUME in actions -> timerEngine.submitCommand(TimerCommand.Resume(timerId))
                    TimerAction.START in actions -> timerEngine.submitCommand(TimerCommand.Start(timerId))
                    TimerAction.START_NEXT_SET in actions ->
                        timerEngine.submitCommand(TimerCommand.StartNextSet(timerId))
                    TimerAction.EXTRA_SET in actions -> timerEngine.submitCommand(TimerCommand.ExtraSet(timerId))
                }
            }

            NotificationRenderer.ACTION_NEXT -> {
                if (TimerAction.NEXT_SET in actions) {
                    timerEngine.submitCommand(TimerCommand.NextSet(timerId))
                }
            }

            NotificationRenderer.ACTION_CLOSE -> {
                timerEngine.submitCommand(TimerCommand.CloseNotification(timerId))
            }

            NotificationRenderer.ACTION_DISMISS -> {
                timerEngine.submitCommand(TimerCommand.DismissNotification(timerId))
            }
        }
    }
}
