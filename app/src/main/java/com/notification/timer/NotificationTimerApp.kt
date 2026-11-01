package com.notification.timer

import android.app.Application
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.engine.TimerEngine
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import javax.inject.Inject

@HiltAndroidApp
class NotificationTimerApp : Application() {
    @Inject
    lateinit var timerEngine: TimerEngine

    @Inject
    lateinit var eventLog: EventLog

    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        eventLog.i("NotificationTimerApp", "Application onCreate: starting TimerEngine")
        timerEngine.start(applicationScope)
    }
}
