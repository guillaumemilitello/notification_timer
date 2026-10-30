package com.notification.timer.core.domain.diagnostics

data class LogEntry(
    val timestamp: Long,
    val tag: String,
    val message: String,
)

interface EventLog {
    fun log(
        tag: String,
        message: String,
    )

    fun getEntries(): List<LogEntry>

    fun clear()
}
