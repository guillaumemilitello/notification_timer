package com.notification.timer.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.data.diagnostics.InMemoryEventLog
import com.notification.timer.core.data.model.LibraryData
import com.notification.timer.core.data.model.UserSettings
import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.model.TimerConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var dataStoreStore: DataStoreStore
    private lateinit var eventLog: InMemoryEventLog
    private lateinit var repository: HistoryRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        eventLog = InMemoryEventLog()
        dataStoreStore = DataStoreStore(context, eventLog)
        repository = HistoryRepositoryImpl(dataStoreStore)
    }

    @Test
    fun `recordTimerUsed adds new entry and increments useCount on repeat with matching fingerprint`() =
        runTest {
            val config1 = TimerConfig(id = "c1", name = "Coffee", durationSec = 180)
            repository.recordTimerUsed(config1, nowEpochMs = 10_000L)

            val historyAfterFirst = repository.history.first()
            assertEquals(1, historyAfterFirst.size)
            val entry1 = historyAfterFirst[0]
            assertEquals("c1", entry1.timerId)
            assertEquals("Coffee", entry1.config.name)
            assertEquals(1, entry1.useCount)
            assertEquals(10_000L, entry1.lastUsedAtMs)
            assertFalse(entry1.isStarred)

            // Record same config with different ID (e.g. from numpad re-entry)
            val config2 = TimerConfig(id = "temp_numpad_2", name = "Coffee", durationSec = 180)
            repository.recordTimerUsed(config2, nowEpochMs = 20_000L)

            val historyAfterSecond = repository.history.first()
            assertEquals(1, historyAfterSecond.size)
            val entry2 = historyAfterSecond[0]
            // Should deduplicate to the existing timer ID
            assertEquals("c1", entry2.timerId)
            assertEquals(2, entry2.useCount)
            assertEquals(20_000L, entry2.lastUsedAtMs)
        }

    @Test
    fun `ranking prioritizes starred entries then useCount then lastUsedAtMs`() =
        runTest {
            val t1 = TimerConfig(id = "t1", name = "Used 1x", durationSec = 60)
            val t2 = TimerConfig(id = "t2", name = "Used 5x", durationSec = 120)
            val t3 = TimerConfig(id = "t3", name = "Used 2x Starred", durationSec = 180)

            repository.recordTimerUsed(t1, nowEpochMs = 1000L)

            repeat(5) { idx ->
                repository.recordTimerUsed(t2, nowEpochMs = 2000L + idx)
            }
            repeat(2) { idx ->
                repository.recordTimerUsed(t3, nowEpochMs = 3000L + idx)
            }

            // Star t3
            repository.toggleStar("t3")

            val list = repository.history.first()
            assertEquals(3, list.size)
            // t3 is starred, so it should be at index 0 despite lower use count than t2
            assertEquals("t3", list[0].timerId)
            assertTrue(list[0].isStarred)

            // t2 (5x) should be before t1 (1x)
            assertEquals("t2", list[1].timerId)
            assertEquals("t1", list[2].timerId)
        }

    @Test
    fun `toggleStar flips isStarred flag`() =
        runTest {
            val config = TimerConfig(id = "t1", name = "Tea", durationSec = 120)
            repository.recordTimerUsed(config, nowEpochMs = 1000L)

            assertFalse(repository.history.first()[0].isStarred)

            repository.toggleStar("t1")
            assertTrue(repository.history.first()[0].isStarred)

            repository.toggleStar("t1")
            assertFalse(repository.history.first()[0].isStarred)
        }

    @Test
    fun `eviction preserves starred items and newly added item when capacity is exceeded`() =
        runTest {
            // Set max history size to 5 (valid range 5..50)
            repository.setMaxHistorySize(5)

            val t1 = TimerConfig(id = "t1", name = "T1", durationSec = 10)
            val t2 = TimerConfig(id = "t2", name = "T2", durationSec = 20)
            val t3 = TimerConfig(id = "t3", name = "T3", durationSec = 30)
            val t4 = TimerConfig(id = "t4", name = "T4", durationSec = 40)
            val t5 = TimerConfig(id = "t5", name = "T5", durationSec = 50)
            val t6 = TimerConfig(id = "t6", name = "T6", durationSec = 60)

            repository.recordTimerUsed(t1, nowEpochMs = 1000L) // useCount = 1
            repository.toggleStar("t1") // starred!

            repeat(3) { idx ->
                repository.recordTimerUsed(t2, nowEpochMs = 2000L + idx)
            } // useCount = 3
            repository.recordTimerUsed(t3, nowEpochMs = 3000L) // useCount = 1
            repository.recordTimerUsed(t4, nowEpochMs = 4000L) // useCount = 1
            repository.recordTimerUsed(t5, nowEpochMs = 5000L) // useCount = 1

            assertEquals(5, repository.history.first().size)

            // Add 6th timer: capacity 5 exceeded
            repository.recordTimerUsed(t6, nowEpochMs = 6000L)

            val history = repository.history.first()
            assertEquals(5, history.size)

            val ids = history.map { it.timerId }.toSet()
            // t1 must NOT be evicted because it's starred
            assertTrue(ids.contains("t1"))
            // t6 must NOT be evicted because it was just added
            assertTrue(ids.contains("t6"))
            // t2 has useCount 3, so it stays
            assertTrue(ids.contains("t2"))
            // t3 has the lowest lastUsedAtMs (3000L) among unstarred items with useCount 1 -> t3 should be evicted!
            assertFalse(ids.contains("t3"))

            // Verify orphaned timer definition t3 was cleaned up in libraryData
            val storedData = dataStoreStore.loadLibraryData()
            assertFalse(storedData.timersById.containsKey("t3"))
            assertTrue(storedData.timersById.containsKey("t1"))
            assertTrue(storedData.timersById.containsKey("t2"))
            assertTrue(storedData.timersById.containsKey("t6"))
        }

    @Test
    fun `legacy presets in library are automatically migrated to history on load`() =
        runTest {
            val legacyPreset = TimerConfig(id = "legacy_p1", name = "Legacy Tea", durationSec = 150)

            @Suppress("DEPRECATION")
            val initial =
                LibraryData(
                    settings = UserSettings(timeIncrementSec = 20),
                    presets = listOf(legacyPreset),
                )
            dataStoreStore.saveLibraryData(initial)

            val freshRepo = HistoryRepositoryImpl(dataStoreStore)
            val history = freshRepo.history.first()
            assertEquals(1, history.size)
            assertEquals("legacy_p1", history[0].timerId)
            assertEquals("Legacy Tea", history[0].config.name)
            assertEquals(1, history[0].useCount)
        }

    @Test
    fun `recordTimerUsed preserves and persists sound and vibration alongside timer`() =
        runTest {
            val customAlert =
                com.notification.timer.core.domain.model.Alert(
                    name = "Done",
                    beforeEndSec = 0,
                    color = "#4CAF50",
                    sound = "content://settings/custom_alert",
                    soundEnabled = false,
                    vibrate = false,
                )
            val config =
                TimerConfig(
                    id = "c_custom_alert",
                    name = "Silent Timer",
                    durationSec = 300,
                    done = customAlert,
                )

            repository.recordTimerUsed(config, nowEpochMs = 50_000L)

            val history = repository.history.first()
            assertEquals(1, history.size)
            val savedConfig = history[0].config
            assertEquals("Silent Timer", savedConfig.name)
            assertFalse(savedConfig.done.soundEnabled)
            assertEquals("content://settings/custom_alert", savedConfig.done.sound)
            assertFalse(savedConfig.done.vibrate)
        }

    @Test
    fun `updateTimerConfig updates saved timer settings in history`() =
        runTest {
            val config = TimerConfig(id = "c_update_test", name = "Test", durationSec = 120)
            repository.recordTimerUsed(config, nowEpochMs = 10_000L)

            val initialHistory = repository.history.first()
            assertTrue(initialHistory[0].config.done.soundEnabled)
            assertTrue(initialHistory[0].config.done.vibrate)

            val updatedConfig =
                config.copy(
                    done =
                        config.done.copy(
                            soundEnabled = false,
                            sound = "content://settings/custom_sound",
                            vibrate = false,
                        ),
                )
            repository.updateTimerConfig(updatedConfig)

            val updatedHistory = repository.history.first()
            assertFalse(updatedHistory[0].config.done.soundEnabled)
            assertEquals("content://settings/custom_sound", updatedHistory[0].config.done.sound)
            assertFalse(updatedHistory[0].config.done.vibrate)
        }
}
