package com.notification.timer.core.domain.engine

import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventPlannerTest {
    private val config1 =
        TimerConfig(
            id = "c1",
            name = "Timer 1",
            durationSec = 60,
            alerts = listOf(Alert("Get ready", 15, "#FF9800")),
        )

    private val config2 =
        TimerConfig(
            id = "c2",
            name = "Timer 2",
            durationSec = 120,
        )

    @Test
    fun `findEarliestNextEventMs returns null when no timers are running`() {
        val timer = Timer(id = 1000, config = config1, label = "Timer 1")
        val earliest = EventPlanner.findEarliestNextEventMs(listOf(timer), nowElapsedMs = 100000L)
        assertNull(earliest)
    }

    @Test
    fun `findEarliestNextEventMs picks earliest alert across multiple timers`() {
        val now = 100000L
        val timer1 = TimerStateMachine.start(Timer(id = 1000, config = config1, label = "T1"), now) // ends at 160000, alert at 145000
        val timer2 = TimerStateMachine.start(Timer(id = 1001, config = config2, label = "T2"), now) // ends at 220000

        val earliest = EventPlanner.findEarliestNextEventMs(listOf(timer1, timer2), nowElapsedMs = now)

        // Earliest event should be Timer 1's 15s alert at 145000L
        assertEquals(145000L, earliest)
    }
}
