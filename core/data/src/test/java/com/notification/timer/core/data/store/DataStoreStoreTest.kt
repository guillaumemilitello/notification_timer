package com.notification.timer.core.data.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.data.diagnostics.InMemoryEventLog
import com.notification.timer.core.data.model.HistoryRecord
import com.notification.timer.core.data.model.LibraryData
import com.notification.timer.core.data.model.UserSettings
import com.notification.timer.core.domain.model.TimerConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DataStoreStoreTest {
    private lateinit var context: Context
    private lateinit var dataStoreStore: DataStoreStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreStore = DataStoreStore(context, InMemoryEventLog())
    }

    @Test
    fun `libraryData round-trip save and load`() =
        runTest {
            val sampleConfig = TimerConfig(id = "c1", name = "Test Timer", durationSec = 100)
            val initialData =
                LibraryData(
                    settings = UserSettings(timeIncrementSec = 30),
                    timersById = mapOf("c1" to sampleConfig),
                    history =
                        listOf(
                            HistoryRecord(
                                timerId = "c1",
                                useCount = 3,
                                lastUsedAtMs = 1000L,
                                isStarred = true,
                            ),
                        ),
                )

            dataStoreStore.saveLibraryData(initialData)
            val loaded = dataStoreStore.loadLibraryData()

            assertEquals(30, loaded.settings.timeIncrementSec)
            assertEquals(1, loaded.timersById.size)
            assertEquals("Test Timer", loaded.timersById["c1"]?.name)
            assertEquals(1, loaded.history.size)
            assertEquals(3, loaded.history[0].useCount)
            assertTrue(loaded.history[0].isStarred)
        }

    @Test
    fun `corrupt library file recovers with default and moves file aside`() =
        runTest {
            val libraryFile = File(context.filesDir, "library.json")
            libraryFile.writeText("INVALID_CORRUPT_JSON{{{")

            val loaded = dataStoreStore.loadLibraryData()
            assertEquals(20, loaded.settings.timeIncrementSec) // Default UserSettings

            val corruptFiles = context.filesDir.listFiles { _, name -> name.contains("corrupt") }
            assertTrue(corruptFiles != null && corruptFiles.isNotEmpty())
        }
}
