package com.notification.timer.core.data.diagnostics

import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.diagnostics.LogEntry
import com.notification.timer.core.domain.diagnostics.LogLevel
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryEventLog
    @Inject
    constructor() : EventLog {
        private val buffer = ConcurrentLinkedQueue<LogEntry>()
        private val maxCapacity = 1000

        private fun record(
            level: LogLevel,
            tag: String,
            message: String,
            throwable: Throwable? = null,
        ) {
            val entry =
                LogEntry(
                    timestamp = System.currentTimeMillis(),
                    tag = tag,
                    message = message,
                    level = level,
                    throwable = throwable,
                )
            buffer.add(entry)
            while (buffer.size > maxCapacity) {
                buffer.poll()
            }
        }

        override fun log(
            tag: String,
            message: String,
        ) {
            d(tag, message)
        }

        override fun d(
            tag: String,
            message: String,
        ) {
            record(LogLevel.DEBUG, tag, message)
            try {
                android.util.Log.d(tag, message)
            } catch (_: Throwable) {
                // Ignore in pure JVM unit tests where android.util.Log is not mocked
            }
        }

        override fun i(
            tag: String,
            message: String,
        ) {
            record(LogLevel.INFO, tag, message)
            try {
                android.util.Log.i(tag, message)
            } catch (_: Throwable) {
                // Ignore in pure JVM unit tests where android.util.Log is not mocked
            }
        }

        override fun w(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) {
            record(LogLevel.WARN, tag, message, throwable)
            try {
                if (throwable != null) {
                    android.util.Log.w(tag, message, throwable)
                } else {
                    android.util.Log.w(tag, message)
                }
            } catch (_: Throwable) {
                // Ignore in pure JVM unit tests where android.util.Log is not mocked
            }
        }

        override fun e(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) {
            record(LogLevel.ERROR, tag, message, throwable)
            try {
                if (throwable != null) {
                    android.util.Log.e(tag, message, throwable)
                } else {
                    android.util.Log.e(tag, message)
                }
            } catch (_: Throwable) {
                // Ignore in pure JVM unit tests where android.util.Log is not mocked
            }
        }

        override fun getEntries(): List<LogEntry> = buffer.toList()

        override fun clear() {
            buffer.clear()
        }
    }
