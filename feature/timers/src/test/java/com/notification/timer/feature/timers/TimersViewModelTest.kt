package com.notification.timer.feature.timers

import app.cash.turbine.test
import com.notification.timer.core.domain.engine.TimerEngine
import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.HistoryEntry
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.model.TimerConfig
import com.notification.timer.core.domain.ports.AlertPlayerPort
import com.notification.timer.core.domain.ports.DisplayPreferences
import com.notification.timer.core.domain.ports.HistoryRepository
import com.notification.timer.core.domain.ports.TimeSource
import com.notification.timer.core.domain.ports.TimerNotifierPort
import com.notification.timer.core.domain.ports.TimerStore
import com.notification.timer.core.domain.ports.WakeupSchedulerPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimersViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val sampleConfigs =
        listOf(
            TimerConfig(id = "c1", name = "Pomodoro", durationSec = 1500),
            TimerConfig(id = "c2", name = "Short Break", durationSec = 300),
        )

    private class FakeHistoryRepository(
        initial: List<HistoryEntry> = emptyList(),
    ) : HistoryRepository {
        private val _history = MutableStateFlow(initial)
        override val history: Flow<List<HistoryEntry>> = _history.asStateFlow()
        override val maxHistorySize: Flow<Int> = MutableStateFlow(10)

        val recorded = mutableListOf<TimerConfig>()
        val toggledStars = mutableListOf<String>()
        val updatedConfigs = mutableListOf<TimerConfig>()

        override suspend fun recordTimerUsed(
            config: TimerConfig,
            nowEpochMs: Long,
        ) {
            recorded.add(config)
            val entry =
                HistoryEntry(
                    timerId = config.id,
                    config = config,
                    useCount = 1,
                    lastUsedAtMs = nowEpochMs,
                    isStarred = false,
                )
            _history.value = _history.value + entry
        }

        override suspend fun updateTimerConfig(config: TimerConfig) {
            updatedConfigs.add(config)
            _history.value =
                _history.value.map {
                    if (it.timerId == config.id) it.copy(config = config) else it
                }
        }

        override suspend fun toggleStar(timerId: String) {
            toggledStars.add(timerId)
            _history.value =
                _history.value.map {
                    if (it.timerId == timerId) it.copy(isStarred = !it.isStarred) else it
                }
        }

        override suspend fun setMaxHistorySize(size: Int) {}

        override suspend fun getMaxHistorySize(): Int = 10
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

    private class FakeDisplayPreferences(
        var stored: Boolean = false,
    ) : DisplayPreferences {
        val saved = mutableListOf<Boolean>()

        override suspend fun loadKeepScreenOn(): Boolean = stored

        override suspend fun saveKeepScreenOn(enabled: Boolean) {
            stored = enabled
            saved.add(enabled)
        }
    }

    private lateinit var fakeDisplayPreferences: FakeDisplayPreferences
    private lateinit var fakeHistoryRepository: FakeHistoryRepository
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
        fakeDisplayPreferences = FakeDisplayPreferences()
        fakeHistoryRepository =
            FakeHistoryRepository(
                listOf(
                    HistoryEntry(
                        timerId = "c1",
                        config = sampleConfigs[0],
                        useCount = 5,
                        lastUsedAtMs = 1000L,
                        isStarred = true,
                    ),
                    HistoryEntry(
                        timerId = "c2",
                        config = sampleConfigs[1],
                        useCount = 2,
                        lastUsedAtMs = 500L,
                        isStarred = false,
                    ),
                ),
            )
        viewModel = TimersViewModel(timerEngine, fakeHistoryRepository, fakeDisplayPreferences, fakeEventLog)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial history is loaded from repository into StateFlow`() =
        runTest(testDispatcher) {
            viewModel.history.test {
                advanceUntilIdle()
                val loadedHistory = expectMostRecentItem()
                assertEquals(2, loadedHistory.size)
                assertEquals("Pomodoro", loadedHistory[0].config.name)
                assertTrue(loadedHistory[0].isStarred)
                assertEquals("Short Break", loadedHistory[1].config.name)
                assertFalse(loadedHistory[1].isStarred)
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
    fun `startTimer records in history repository and submits create command`() =
        runTest(testDispatcher) {
            val config =
                TimerConfig(
                    id = "c_custom",
                    name = "Boxing Round",
                    durationSec = 180,
                    sets = 12,
                    runningColor = "#E91E63",
                )
            viewModel.startTimer(config)
            advanceUntilIdle()

            assertEquals(1, fakeHistoryRepository.recorded.size)
            assertEquals("Boxing Round", fakeHistoryRepository.recorded[0].name)
            assertTrue(fakeEventLog.logs.any { it.contains("startTimer: Boxing Round (180s)") })
        }

    @Test
    fun `toggleHistoryStar invokes repository and updates star state`() =
        runTest(testDispatcher) {
            viewModel.history.test {
                viewModel.toggleHistoryStar("c2")
                advanceUntilIdle()

                assertEquals(listOf("c2"), fakeHistoryRepository.toggledStars)
                val updated = expectMostRecentItem().first { it.timerId == "c2" }
                assertTrue(updated.isStarred)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `handleCommand submits commands and records logs`() =
        runTest(testDispatcher) {
            viewModel.handleCommand(TimerCommand.Pause(1001))
            viewModel.handleCommand(TimerCommand.UpdateSets(1001, 3))
            advanceUntilIdle()

            assertTrue(fakeEventLog.logs.any { it.contains("handleCommand: Pause(timerId=1001)") })
            assertTrue(fakeEventLog.logs.any { it.contains("handleCommand: UpdateSets(timerId=1001, sets=3)") })
        }

    @Test
    fun `keepScreenOn defaults to false and never changes without user toggle`() =
        runTest(testDispatcher) {
            viewModel.handleCommand(TimerCommand.Pause(1001))
            advanceUntilIdle()

            assertFalse(viewModel.keepScreenOn.value)
            assertTrue(fakeDisplayPreferences.saved.isEmpty())
        }

    @Test
    fun `keepScreenOn reflects persisted value on startup`() =
        runTest(testDispatcher) {
            val prefs = FakeDisplayPreferences(stored = true)
            val vm = TimersViewModel(timerEngine, fakeHistoryRepository, prefs, fakeEventLog)
            advanceUntilIdle()

            assertTrue(vm.keepScreenOn.value)
            assertTrue(prefs.saved.isEmpty())
        }

    @Test
    fun `toggleKeepScreenOn flips state, persists it, and logs`() =
        runTest(testDispatcher) {
            viewModel.keepScreenOn.test {
                assertFalse(awaitItem())

                viewModel.toggleKeepScreenOn()
                advanceUntilIdle()
                assertTrue(awaitItem())

                viewModel.toggleKeepScreenOn()
                advanceUntilIdle()
                assertFalse(awaitItem())
            }
            assertEquals(listOf(true, false), fakeDisplayPreferences.saved)
            assertTrue(fakeEventLog.logs.any { it.contains("toggleKeepScreenOn: true") })
            assertTrue(fakeEventLog.logs.any { it.contains("toggleKeepScreenOn: false") })
        }

    @Test
    fun `early toggle before initial load completes is not overwritten`() =
        runTest(testDispatcher) {
            val prefs = FakeDisplayPreferences(stored = true)
            val vm = TimersViewModel(timerEngine, fakeHistoryRepository, prefs, fakeEventLog)
            // Tap before the dispatcher has run the initial load.
            vm.toggleKeepScreenOn()
            advanceUntilIdle()

            assertFalse(vm.keepScreenOn.value)
            assertEquals(listOf(false), prefs.saved)
        }

    @Test
    fun `updateTimerAlertSettings submits command and updates history`() =
        runTest(testDispatcher) {
            timerEngine.start(backgroundScope)
            advanceUntilIdle()

            val config = TimerConfig(id = "c_alert_test", name = "Baking", durationSec = 600)
            viewModel.startTimer(config)
            advanceUntilIdle()

            viewModel.timers.test {
                val list = awaitItem()
                val timer = list.first { it.config.id == "c_alert_test" }
                assertTrue(timer.config.done.soundEnabled)
                assertTrue(timer.config.done.vibrate)

                viewModel.updateTimerAlertSettings(
                    timerId = timer.id,
                    soundEnabled = false,
                    soundUri = "content://custom/sound",
                    vibrate = false,
                )
                advanceUntilIdle()

                val updatedList = awaitItem()
                val updatedTimer = updatedList.first { it.id == timer.id }
                assertFalse(updatedTimer.config.done.soundEnabled)
                assertEquals("content://custom/sound", updatedTimer.config.done.sound)
                assertFalse(updatedTimer.config.done.vibrate)

                val history = fakeHistoryRepository.history.first()
                val historyEntry = history.first { it.timerId == "c_alert_test" }
                assertFalse(historyEntry.config.done.soundEnabled)
                assertEquals("content://custom/sound", historyEntry.config.done.sound)
                assertFalse(historyEntry.config.done.vibrate)
            }
        }

    @Test
    fun `updateTimerSettings updates name color sets alert settings and saves to history`() =
        runTest(testDispatcher) {
            timerEngine.start(backgroundScope)
            advanceUntilIdle()

            val config = TimerConfig(id = "c_settings_test", name = "Old Name", durationSec = 300, runningColor = "#3F51B5")
            viewModel.startTimer(config)
            advanceUntilIdle()

            viewModel.timers.test {
                val list = awaitItem()
                val timer = list.first { it.config.id == "c_settings_test" }

                viewModel.updateTimerSettings(
                    timerId = timer.id,
                    name = "New Name",
                    colorHex = "#FF5722",
                    sets = 5,
                    soundEnabled = false,
                    soundUri = "content://sound/alert",
                    vibrate = true,
                )
                advanceUntilIdle()

                val updatedList = expectMostRecentItem()
                val updatedTimer = updatedList.first { it.id == timer.id }
                assertEquals("New Name", updatedTimer.label)
                assertEquals("#FF5722", updatedTimer.config.runningColor)
                assertEquals(5, updatedTimer.config.sets)
                assertFalse(updatedTimer.config.done.soundEnabled)
                assertEquals("content://sound/alert", updatedTimer.config.done.sound)
                assertTrue(updatedTimer.config.done.vibrate)

                val history = fakeHistoryRepository.history.first()
                val historyEntry = history.first { it.timerId == "c_settings_test" }
                assertEquals("New Name", historyEntry.config.name)
                assertEquals("#FF5722", historyEntry.config.runningColor)
                assertEquals(5, historyEntry.config.sets)
                assertFalse(historyEntry.config.done.soundEnabled)
                assertEquals("content://sound/alert", historyEntry.config.done.sound)
                assertTrue(historyEntry.config.done.vibrate)
            }
        }
}
