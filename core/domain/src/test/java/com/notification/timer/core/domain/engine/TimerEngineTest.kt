package com.notification.timer.core.domain.engine

import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.Preset
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.ports.AlertPlayerPort
import com.notification.timer.core.domain.ports.TimeSource
import com.notification.timer.core.domain.ports.TimerNotifierPort
import com.notification.timer.core.domain.ports.TimerStore
import com.notification.timer.core.domain.ports.WakeupSchedulerPort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {
    private class FakeTimeSource(
        var elapsedMs: Long = 10_000L,
        var wallMs: Long = 1_700_000_000_000L,
    ) : TimeSource {
        override fun elapsedRealtimeMs(): Long = elapsedMs

        override fun currentTimeMillis(): Long = wallMs
    }

    private class InMemoryTimerStore : TimerStore {
        var timers: List<Timer> = emptyList()
        var nextId: Int = 1000

        override suspend fun saveTimers(timers: List<Timer>) {
            this.timers = timers
        }

        override suspend fun saveRuntimeState(
            timers: List<Timer>,
            nextTimerId: Int,
        ) {
            this.timers = timers
            this.nextId = nextTimerId
        }

        override suspend fun loadTimers(): List<Timer> = timers

        override suspend fun loadNextTimerId(): Int = nextId
    }

    private class NoOpWakeupScheduler : WakeupSchedulerPort {
        override fun scheduleWakeup(eventTimeMs: Long) {}

        override fun cancelWakeup() {}
    }

    private class NoOpAlertPlayer : AlertPlayerPort {
        override fun playAlert(alert: Alert) {}
    }

    private class RecordingNotifier : TimerNotifierPort {
        val snapshots = mutableListOf<List<Timer>>()

        override fun updateNotifications(timers: List<Timer>) {
            snapshots.add(timers)
        }
    }

    @Test
    fun `scales cleanly to N concurrent timers without 5-timer ceiling and deduplicates labels`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val engineScope = TestScope(dispatcher)
            val clock = FakeTimeSource()
            val store = InMemoryTimerStore()
            val notifier = RecordingNotifier()
            val engine = TimerEngine(clock, store, NoOpWakeupScheduler(), NoOpAlertPlayer(), notifier)

            engine.start(engineScope)
            runCurrent()

            // Create 10 manual timers (with distinct temp preset IDs, just like Numpad)
            repeat(10) { idx ->
                engine.submitCommand(
                    TimerCommand.Create(
                        preset = Preset(id = "temp_$idx", name = "Timer", durationSec = 60),
                        startImmediately = idx % 2 == 0,
                    ),
                )
            }
            runCurrent()

            val timers = engine.timers.value
            assertEquals(10, timers.size)
            assertEquals((1000..1009).toSet(), timers.map { it.id }.toSet())
            assertEquals(
                setOf(
                    "Timer",
                    "Timer 2",
                    "Timer 3",
                    "Timer 4",
                    "Timer 5",
                    "Timer 6",
                    "Timer 7",
                    "Timer 8",
                    "Timer 9",
                    "Timer 10",
                ),
                timers.map { it.label }.toSet(),
            )

            // Verify notification visibility matches startImmediately per timer
            for (timer in timers) {
                val expectedVisible = (timer.id - 1000) % 2 == 0
                assertEquals(expectedVisible, timer.isNotificationVisible)
            }

            // Clear all timers and restart a new engine against the same store: IDs never reuse 1000..1009
            for (timer in timers) {
                engine.submitCommand(TimerCommand.Clear(timer.id))
            }
            runCurrent()
            assertTrue(engine.timers.value.isEmpty())
            engineScope.cancel()

            val restartedScope = TestScope(dispatcher)
            val restartedEngine = TimerEngine(clock, store, NoOpWakeupScheduler(), NoOpAlertPlayer(), notifier)
            restartedEngine.start(restartedScope)
            runCurrent()

            restartedEngine.submitCommand(
                TimerCommand.Create(
                    preset = Preset(id = "p_after_restart", name = "Pasta", durationSec = 120),
                    startImmediately = true,
                ),
            )
            runCurrent()

            val createdAfterRestart = restartedEngine.timers.value.single()
            assertEquals(1010, createdAfterRestart.id)
            assertTrue(createdAfterRestart.isNotificationVisible)
            assertFalse(createdAfterRestart.id in 1000..1009)

            advanceTimeBy(10)
            restartedScope.cancel()
        }
}
