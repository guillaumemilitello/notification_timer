package com.notification.timer.feature.timers

import app.cash.turbine.test
import com.notification.timer.core.domain.engine.TimerEngine
import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.Preset
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.ports.AlertPlayerPort
import com.notification.timer.core.domain.ports.LibraryRepository
import com.notification.timer.core.domain.ports.TimeSource
import com.notification.timer.core.domain.ports.TimerNotifierPort
import com.notification.timer.core.domain.ports.TimerStore
import com.notification.timer.core.domain.ports.WakeupSchedulerPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimersViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val samplePresets =
        listOf(
            Preset(id = "p1", name = "Pomodoro", durationSec = 1500),
            Preset(id = "p2", name = "Short Break", durationSec = 300),
        )

    private val fakeLibraryRepository =
        object : LibraryRepository {
            override suspend fun loadPresets(): List<Preset> = samplePresets

            override suspend fun savePresets(presets: List<Preset>) {}
        }

    private val fakeTimeSource =
        object : TimeSource {
            override fun elapsedRealtimeMs(): Long = 0L

            override fun currentTimeMillis(): Long = 0L
        }

    private val fakeTimerStore =
        object : TimerStore {
            override suspend fun saveTimers(timers: List<Timer>) {}

            override suspend fun saveRuntimeState(
                timers: List<Timer>,
                nextTimerId: Int,
            ) {}

            override suspend fun loadTimers(): List<Timer> = emptyList()

            override suspend fun loadNextTimerId(): Int = 1000
        }

    private val fakeWakeupScheduler =
        object : WakeupSchedulerPort {
            override fun scheduleWakeup(eventTimeMs: Long) {}

            override fun cancelWakeup() {}
        }

    private val fakeAlertPlayer =
        object : AlertPlayerPort {
            override fun playAlert(alert: Alert) {}
        }

    private val fakeTimerNotifier =
        object : TimerNotifierPort {
            override fun updateNotifications(timers: List<Timer>) {}
        }

    private val fakeEventLog =
        object : com.notification.timer.core.domain.diagnostics.EventLog {
            val logs = mutableListOf<String>()

            override fun log(
                tag: String,
                message: String,
            ) {
                logs.add("[$tag] $message")
            }

            override fun getEntries(): List<com.notification.timer.core.domain.diagnostics.LogEntry> = emptyList()

            override fun clear() {
                logs.clear()
            }
        }

    private lateinit var timerEngine: TimerEngine
    private lateinit var viewModel: TimersViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        timerEngine =
            TimerEngine(
                timeSource = fakeTimeSource,
                timerStore = fakeTimerStore,
                wakeupScheduler = fakeWakeupScheduler,
                alertPlayer = fakeAlertPlayer,
                timerNotifier = fakeTimerNotifier,
                eventLog = fakeEventLog,
            )
        // Construct ViewModel without calling timerEngine.start() to avoid infinite loop
        viewModel = TimersViewModel(timerEngine, fakeLibraryRepository, fakeEventLog)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial presets are loaded from repository into StateFlow`() =
        runTest(testDispatcher) {
            viewModel.presets.test {
                advanceUntilIdle()
                val loadedPresets = expectMostRecentItem()
                assertEquals(2, loadedPresets.size)
                assertEquals("Pomodoro", loadedPresets[0].name)
                assertEquals("Short Break", loadedPresets[1].name)
            }
        }

    @Test
    fun `timers flow initially emits empty list when engine has no timers`() =
        runTest(testDispatcher) {
            viewModel.timers.test {
                val initialList = awaitItem()
                assertEquals(emptyList<Timer>(), initialList)
            }
        }

    @Test
    fun `startTimerFromPreset and handleCommand submit commands and record logs`() =
        runTest(testDispatcher) {
            viewModel.startTimerFromPreset(samplePresets.first())
            viewModel.handleCommand(TimerCommand.Pause(1001))
            viewModel.handleCommand(TimerCommand.UpdateSets(1001, 3))
            advanceUntilIdle()

            assertTrue(fakeEventLog.logs.any { it.contains("startTimerFromPreset: Pomodoro (1500s)") })
            assertTrue(fakeEventLog.logs.any { it.contains("handleCommand: Pause(timerId=1001)") })
            assertTrue(fakeEventLog.logs.any { it.contains("handleCommand: UpdateSets(timerId=1001, sets=3)") })
        }
}
