package com.notification.timer.core.runtime.time

import android.os.SystemClock
import com.notification.timer.core.domain.ports.TimeSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeSource
    @Inject
    constructor() : TimeSource {
        override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()

        override fun currentTimeMillis(): Long = System.currentTimeMillis()
    }
