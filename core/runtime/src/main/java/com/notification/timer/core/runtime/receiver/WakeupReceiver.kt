package com.notification.timer.core.runtime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.engine.TimerEngine
import com.notification.timer.core.runtime.wakeup.WakeupScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WakeupReceiver : BroadcastReceiver() {
    @Inject
    lateinit var timerEngine: TimerEngine

    @Inject
    lateinit var wakeupScheduler: WakeupScheduler

    @Inject
    lateinit var eventLog: EventLog

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != WakeupScheduler.ACTION_WAKEUP) return

        val pendingResult = goAsync()
        wakeupScheduler.acquireWakeLock(WakeupScheduler.WAKELOCK_TIMEOUT_MS)
        eventLog.i("WakeupReceiver", "Wakeup alarm fired, processing state tick")

        try {
            // Engine loop will pick up and process due alerts on tick
            pendingResult.finish()
        } catch (e: Exception) {
            eventLog.e("WakeupReceiver", "Error during wakeup: ${e.message}", e)
            pendingResult.finish()
        }
    }
}
