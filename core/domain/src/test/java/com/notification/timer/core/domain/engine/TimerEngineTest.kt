package com.notification.timer.core.domain.engine

import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.diagnostics.LogEntry
import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.model.TimerConfig
import com.notification.timer.core.domain.model.TimerState
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

    private class RecordingEventLog : EventLog {
        val messages = mutableListOf<String>()

        override fun log(
            tag: String,
            message: String,
        ) {
            messages.add("[$tag] $message")
        }

        override fun getEntries(): List<LogEntry> = emptyList()

        override fun clear() {
            messages.clear()
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

            // Create 10 manual timers (with distinct config IDs, just like Numpad)
            repeat(10) { idx ->
                engine.submitCommand(
                    TimerCommand.Create(
                        config = TimerConfig(id = "temp_$idx", name = "Timer", durationSec = 60),
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
                    config = TimerConfig(id = "c_after_restart", name = "Pasta", durationSec = 120),
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

    @Test
    fun `multiple timers started at different times have strictly isolated remaining times and log state transitions`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val engineScope = TestScope(dispatcher)
            val clock = FakeTimeSource(elapsedMs = 10_000L)
            val store = InMemoryTimerStore()
            val notifier = RecordingNotifier()
            val eventLog = RecordingEventLog()
            val engine =
                TimerEngine(
                    timeSource = clock,
                    timerStore = store,
                    wakeupScheduler = NoOpWakeupScheduler(),
                    alertPlayer = NoOpAlertPlayer(),
                    timerNotifier = notifier,
                    eventLog = eventLog,
                )

            engine.start(engineScope)
            runCurrent()

            // Timer 1: 60s started immediately at t = 10_000
            engine.submitCommand(
                TimerCommand.Create(
                    config = TimerConfig(id = "c1", name = "Timer 1", durationSec = 60),
                    startImmediately = true,
                ),
            )
            // Timer 2: 30s created in Ready state at t = 10_000
            engine.submitCommand(
                TimerCommand.Create(
                    config = TimerConfig(id = "c2", name = "Timer 2", durationSec = 30),
                    startImmediately = false,
                ),
            )
            runCurrent()

            val timersAtT10 = engine.timers.value
            assertEquals(2, timersAtT10.size)
            val timer2AtT10 = timersAtT10.first { it.config.id == "c2" }
            val timer1AtT10 = timersAtT10.first { it.config.id == "c1" }

            assertTrue(timer1AtT10.state is TimerState.Running)
            assertTrue(timer2AtT10.state is TimerState.Ready)
            assertEquals(60_000L, timer1AtT10.remainingMs(clock.elapsedRealtimeMs()))
            assertEquals(30_000L, timer2AtT10.remainingMs(clock.elapsedRealtimeMs()))

            // Timer 1 runs for 25 seconds while Timer 2 sits idle in Ready
            clock.elapsedMs = 35_000L
            runCurrent()

            assertEquals(35_000L, timer1AtT10.remainingMs(clock.elapsedRealtimeMs()))
            // Timer 2 sitting in Ready must still compute exact config duration
            assertEquals(30_000L, timer2AtT10.remainingMs(clock.elapsedRealtimeMs()))

            // User presses Play on Timer 2 at t = 35_000
            engine.submitCommand(TimerCommand.Start(timer2AtT10.id))
            runCurrent()

            val timersAtT35 = engine.timers.value
            val timer2Started = timersAtT35.first { it.id == timer2AtT10.id }
            val timer1Running = timersAtT35.first { it.id == timer1AtT10.id }

            assertTrue(timer2Started.state is TimerState.Running)
            val runningState2 = timer2Started.state as TimerState.Running
            // Ends at 35_000 + 30_000 = 65_000
            assertEquals(65_000L, runningState2.endsAtElapsedMs)

            // Remaining time right after start must be EXACTLY duration (30_000ms), NOT 55_000ms
            assertEquals(30_000L, timer2Started.remainingMs(clock.elapsedRealtimeMs()))
            assertEquals(35_000L, timer1Running.remainingMs(clock.elapsedRealtimeMs()))

            // Advance 10s to t = 45_000
            clock.elapsedMs = 45_000L
            runCurrent()

            assertEquals(20_000L, timer2Started.remainingMs(clock.elapsedRealtimeMs()))
            assertEquals(25_000L, timer1Running.remainingMs(clock.elapsedRealtimeMs()))

            // Verify eventLog recorded the commands and state transitions
            assertTrue(eventLog.messages.any { it.contains("submitCommand: Start(timerId=${timer2AtT10.id})") })
            assertTrue(eventLog.messages.any { it.contains("Started timer ${timer2AtT10.id}") })

            // Update sets of Timer 2 from null to 4
            engine.submitCommand(TimerCommand.UpdateSets(timer2AtT10.id, 4))
            runCurrent()

            val timer2AfterUpdate = engine.timers.value.first { it.id == timer2AtT10.id }
            assertEquals(4, timer2AfterUpdate.config.sets)

            // Update sets to 0 (infinite)
            engine.submitCommand(TimerCommand.UpdateSets(timer2AtT10.id, 0))
            runCurrent()

            val timer2AfterInfinite = engine.timers.value.first { it.id == timer2AtT10.id }
            assertEquals(null, timer2AfterInfinite.config.sets)

            engineScope.cancel()
        }

    @Test
    fun `creating timer from draft config preserves custom name, color, sets, and duration`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val engineScope = TestScope(dispatcher)
            val clock = FakeTimeSource()
            val store = InMemoryTimerStore()
            val notifier = RecordingNotifier()
            val engine = TimerEngine(clock, store, NoOpWakeupScheduler(), NoOpAlertPlayer(), notifier)

            engine.start(engineScope)
            runCurrent()

            val draftConfig =
                TimerConfig(
                    id = "temp_draft_1",
                    name = "Sprint Intervals",
                    durationSec = 45,
                    sets = 8,
                    runningColor = "#E91E63",
                )
            engine.submitCommand(TimerCommand.Create(config = draftConfig, startImmediately = true))
            runCurrent()

            val timers = engine.timers.value
            assertEquals(1, timers.size)
            val timer = timers.first()
            assertEquals("Sprint Intervals", timer.label)
            assertEquals("#E91E63", timer.config.runningColor)
            assertEquals(8, timer.config.sets)
            assertEquals(45, timer.config.durationSec)
            assertTrue(timer.state is TimerState.Running)

            engineScope.cancel()
        }

    @Test
    fun `UpdateAlertSettings updates sound and vibration of target timer`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val engineScope = TestScope(dispatcher)
            val clock = FakeTimeSource()
            val store = InMemoryTimerStore()
            val engine = TimerEngine(clock, store, NoOpWakeupScheduler(), NoOpAlertPlayer(), RecordingNotifier())

            engine.start(engineScope)
            runCurrent()

            val config = TimerConfig(id = "c1", name = "Cook", durationSec = 100)
            engine.submitCommand(TimerCommand.Create(config, startImmediately = false))
            runCurrent()

            val created = engine.timers.value.first()
            assertTrue(created.config.done.soundEnabled)
            assertTrue(created.config.done.vibrate)

            engine.submitCommand(
                TimerCommand.UpdateAlertSettings(
                    timerId = created.id,
                    soundEnabled = false,
                    soundUri = "content://media/alarm",
                    vibrate = false,
                ),
            )
            runCurrent()

            val updated = engine.timers.value.first()
            assertFalse(updated.config.done.soundEnabled)
            assertEquals("content://media/alarm", updated.config.done.sound)
            assertFalse(updated.config.done.vibrate)

            engineScope.cancel()
        }
}
