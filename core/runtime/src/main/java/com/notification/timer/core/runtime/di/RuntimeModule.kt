package com.notification.timer.core.runtime.di

import com.notification.timer.core.domain.engine.TimerEngine
import com.notification.timer.core.domain.ports.AlertPlayerPort
import com.notification.timer.core.domain.ports.TimeSource
import com.notification.timer.core.domain.ports.TimerNotifierPort
import com.notification.timer.core.domain.ports.TimerStore
import com.notification.timer.core.domain.ports.WakeupSchedulerPort
import com.notification.timer.core.runtime.audio.AlertPlayer
import com.notification.timer.core.runtime.notification.TimerNotifierAdapter
import com.notification.timer.core.runtime.time.SystemTimeSource
import com.notification.timer.core.runtime.wakeup.WakeupSchedulerAdapter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RuntimeModule {
    @Binds
    @Singleton
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource

    @Binds
    @Singleton
    abstract fun bindWakeupSchedulerPort(impl: WakeupSchedulerAdapter): WakeupSchedulerPort

    @Binds
    @Singleton
    abstract fun bindAlertPlayerPort(impl: AlertPlayer): AlertPlayerPort

    @Binds
    @Singleton
    abstract fun bindTimerNotifierPort(impl: TimerNotifierAdapter): TimerNotifierPort

    companion object {
        @Provides
        @Singleton
        fun provideTimerEngine(
            timeSource: TimeSource,
            timerStore: TimerStore,
            wakeupSchedulerPort: WakeupSchedulerPort,
            alertPlayerPort: AlertPlayerPort,
            timerNotifierPort: TimerNotifierPort,
        ): TimerEngine =
            TimerEngine(
                timeSource = timeSource,
                timerStore = timerStore,
                wakeupScheduler = wakeupSchedulerPort,
                alertPlayer = alertPlayerPort,
                timerNotifier = timerNotifierPort,
            )
    }
}
