package com.notification.timer.core.data.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.data.diagnostics.InMemoryEventLog
import com.notification.timer.core.data.model.LibraryData
import com.notification.timer.core.data.model.UserSettings
import com.notification.timer.core.domain.model.Preset
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
            val samplePreset = Preset(id = "p1", name = "Test Preset", durationSec = 100)
            val initialData =
                LibraryData(
                    settings = UserSettings(timeIncrementSec = 30),
                    presets = listOf(samplePreset),
                )

            dataStoreStore.saveLibraryData(initialData)
            val loaded = dataStoreStore.loadLibraryData()

            assertEquals(30, loaded.settings.timeIncrementSec)
            assertEquals(1, loaded.presets.size)
            assertEquals("Test Preset", loaded.presets[0].name)
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
