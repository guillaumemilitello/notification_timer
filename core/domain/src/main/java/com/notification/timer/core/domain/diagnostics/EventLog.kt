package com.notification.timer.core.domain.diagnostics

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

data class LogEntry(
    val timestamp: Long,
    val tag: String,
    val message: String,
    val level: LogLevel = LogLevel.DEBUG,
    val throwable: Throwable? = null,
)

interface EventLog {
    fun log(
        tag: String,
        message: String,
    )

    fun d(
        tag: String,
        message: String,
    ) = log(tag, message)

    fun i(
        tag: String,
        message: String,
    ) = log(tag, message)

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = log(tag, message)

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = log(tag, message)

    fun getEntries(): List<LogEntry>

    fun clear()
}
