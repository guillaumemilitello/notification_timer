package com.notification.timer.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.data.diagnostics.InMemoryEventLog
import com.notification.timer.core.data.model.HistoryRecord
import com.notification.timer.core.data.model.LibraryData
import com.notification.timer.core.data.model.UserSettings
import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.model.TimerConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DisplayPreferencesRepositoryTest {
    private lateinit var context: Context
    private lateinit var eventLog: InMemoryEventLog
    private lateinit var dataStoreStore: DataStoreStore
    private lateinit var repository: DisplayPreferencesRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "library.json").delete()
        eventLog = InMemoryEventLog()
        dataStoreStore = DataStoreStore(context, eventLog)
        repository = DisplayPreferencesRepository(dataStoreStore, eventLog)
    }

    @Test
    fun `keepScreenOn defaults to false on fresh install`() =
        runTest {
            assertFalse(repository.loadKeepScreenOn())
        }

    @Test
    fun `saveKeepScreenOn persists across repository instances`() =
        runTest {
            repository.saveKeepScreenOn(true)

            val reloaded = DisplayPreferencesRepository(DataStoreStore(context, eventLog), eventLog)
            assertTrue(reloaded.loadKeepScreenOn())

            reloaded.saveKeepScreenOn(false)
            assertFalse(repository.loadKeepScreenOn())
        }

    @Test
    fun `saveKeepScreenOn preserves history and other settings`() =
        runTest {
            val config = TimerConfig(id = "c1", name = "Keep Me", durationSec = 90)
            dataStoreStore.saveLibraryData(
                LibraryData(
                    settings = UserSettings(timeIncrementSec = 45),
                    timersById = mapOf("c1" to config),
                    history = listOf(HistoryRecord("c1", 1, 1000L)),
                ),
            )

            repository.saveKeepScreenOn(true)

            val loaded = dataStoreStore.loadLibraryData()
            assertTrue(loaded.settings.keepScreenOn)
            assertEquals(45, loaded.settings.timeIncrementSec)
            assertEquals(mapOf("c1" to config), loaded.timersById)
        }

    @Test
    fun `legacy library file without keepScreenOn field loads as false`() =
        runTest {
            File(context.filesDir, "library.json")
                .writeText("""{"schemaVersion":2,"settings":{"timeIncrementSec":30},"timersById":{},"history":[]}""")

            assertFalse(repository.loadKeepScreenOn())
            assertEquals(30, dataStoreStore.loadLibraryData().settings.timeIncrementSec)
        }

    @Test
    fun `saveKeepScreenOn logs the change`() =
        runTest {
            repository.saveKeepScreenOn(true)

            assertTrue(eventLog.getEntries().any { it.message.contains("keepScreenOn=true") })
        }
}
