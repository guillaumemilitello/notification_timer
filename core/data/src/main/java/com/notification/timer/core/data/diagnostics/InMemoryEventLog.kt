package com.notification.timer.core.data.diagnostics

import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.diagnostics.LogEntry
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryEventLog
    @Inject
    constructor() : EventLog {
        private val buffer = ConcurrentLinkedQueue<LogEntry>()
        private val maxCapacity = 1000

        override fun log(
            tag: String,
            message: String,
        ) {
            val entry =
                LogEntry(
                    timestamp = System.currentTimeMillis(),
                    tag = tag,
                    message = message,
                )
            buffer.add(entry)
            while (buffer.size > maxCapacity) {
                buffer.poll()
            }
        }

        override fun getEntries(): List<LogEntry> = buffer.toList()

        override fun clear() {
            buffer.clear()
        }
    }
