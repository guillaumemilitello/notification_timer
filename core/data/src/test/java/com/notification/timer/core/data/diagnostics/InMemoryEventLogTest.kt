package com.notification.timer.core.data.diagnostics

import com.notification.timer.core.domain.diagnostics.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InMemoryEventLogTest {
    private lateinit var eventLog: InMemoryEventLog

    @Before
    fun setup() {
        eventLog = InMemoryEventLog()
    }

    @Test
    fun `records log entries with appropriate log levels and timestamps`() {
        eventLog.d("TestTag", "Debug message")
        eventLog.i("TestTag", "Info message")
        val exception = RuntimeException("Boom")
        eventLog.w("TestTag", "Warning message", exception)
        eventLog.e("TestTag", "Error message", exception)

        val entries = eventLog.getEntries()
        assertEquals(4, entries.size)

        assertEquals(LogLevel.DEBUG, entries[0].level)
        assertEquals("Debug message", entries[0].message)
        assertEquals("TestTag", entries[0].tag)
        assertTrue(entries[0].timestamp > 0)

        assertEquals(LogLevel.INFO, entries[1].level)
        assertEquals("Info message", entries[1].message)

        assertEquals(LogLevel.WARN, entries[2].level)
        assertEquals("Warning message", entries[2].message)
        assertEquals(exception, entries[2].throwable)

        assertEquals(LogLevel.ERROR, entries[3].level)
        assertEquals("Error message", entries[3].message)
        assertEquals(exception, entries[3].throwable)
    }

    @Test
    fun `clear empties all recorded entries`() {
        eventLog.i("TestTag", "First")
        eventLog.i("TestTag", "Second")
        assertEquals(2, eventLog.getEntries().size)

        eventLog.clear()
        assertTrue(eventLog.getEntries().isEmpty())
    }

    @Test
    fun `buffer caps capacity without unbounded growth`() {
        for (i in 1..1050) {
            eventLog.d("Stress", "Message #$i")
        }

        val entries = eventLog.getEntries()
        assertEquals(1000, entries.size)
        // First entry in rolling buffer should be #51 after dropping first 50
        assertEquals("Message #51", entries.first().message)
        assertEquals("Message #1050", entries.last().message)
    }
}
