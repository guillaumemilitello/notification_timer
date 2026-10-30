package com.notification.timer

import android.app.Application
import com.notification.timer.core.domain.engine.TimerEngine
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import javax.inject.Inject

@HiltAndroidApp
class NotificationTimerApp : Application() {
    @Inject
    lateinit var timerEngine: TimerEngine

    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        timerEngine.start(applicationScope)
    }
}
