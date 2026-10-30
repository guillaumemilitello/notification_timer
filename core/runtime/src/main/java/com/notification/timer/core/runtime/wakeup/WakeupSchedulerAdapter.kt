package com.notification.timer.core.runtime.wakeup

import com.notification.timer.core.domain.ports.WakeupSchedulerPort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeupSchedulerAdapter
    @Inject
    constructor(
        private val scheduler: WakeupScheduler,
    ) : WakeupSchedulerPort {
        override fun scheduleWakeup(eventTimeMs: Long) {
            scheduler.scheduleWakeup(eventTimeMs)
        }

        override fun cancelWakeup() {
            scheduler.cancelWakeup()
        }
    }
