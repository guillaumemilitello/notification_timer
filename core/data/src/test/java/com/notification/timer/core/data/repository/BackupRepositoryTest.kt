package com.notification.timer.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.data.diagnostics.InMemoryEventLog
import com.notification.timer.core.data.model.LibraryData
import com.notification.timer.core.data.model.UserSettings
import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.model.Preset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var dataStoreStore: DataStoreStore
    private lateinit var eventLog: InMemoryEventLog
    private lateinit var backupRepository: BackupRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        eventLog = InMemoryEventLog()
        dataStoreStore = DataStoreStore(context, eventLog)
        backupRepository = BackupRepository(context, dataStoreStore, eventLog)
    }

    @Test
    fun `export and import round-trip preserves library data`() =
        runTest {
            val originalPreset =
                Preset(
                    id = "p-101",
                    name = "Tabata Interval",
                    durationSec = 20,
                    sets = 8,
                )
            val initialLibrary =
                LibraryData(
                    settings = UserSettings(timeIncrementSec = 45),
                    presets = listOf(originalPreset),
                )
            dataStoreStore.saveLibraryData(initialLibrary)

            val exportFile = tempFolder.newFile("backup.json")
            val exportUri = Uri.fromFile(exportFile)

            // Export to file URI
            val exportSuccess = backupRepository.exportToUri(exportUri)
            assertTrue("Export should succeed", exportSuccess)
            assertTrue("Export file should not be empty", exportFile.length() > 0)

            // Clear dataStoreStore to simulate fresh state
            dataStoreStore.saveLibraryData(LibraryData())

            // Import back
            val importResult = backupRepository.importFromUri(exportUri)
            assertTrue("Import should succeed", importResult is ImportResult.Success)
            assertEquals(1, (importResult as ImportResult.Success).presetsCount)

            val reloaded = dataStoreStore.loadLibraryData()
            assertEquals(45, reloaded.settings.timeIncrementSec)
            assertEquals(1, reloaded.presets.size)
            assertEquals("Tabata Interval", reloaded.presets[0].name)
            assertEquals(20, reloaded.presets[0].durationSec)
            assertEquals(8, reloaded.presets[0].sets)
        }

    @Test
    fun `import corrupted file returns ErrorInvalidFormat without crashing`() =
        runTest {
            val corruptFile = tempFolder.newFile("corrupt.json")
            corruptFile.writeText("{ this is : invalid json syntax !!!")
            val corruptUri = Uri.fromFile(corruptFile)

            val result = backupRepository.importFromUri(corruptUri)
            assertTrue("Result should be ErrorInvalidFormat", result is ImportResult.ErrorInvalidFormat)
        }

    @Test
    fun `import future schema version returns ErrorIncompatibleSchema`() =
        runTest {
            val futureFile = tempFolder.newFile("future_backup.json")
            val futureJson =
                """
                {
                    "schemaVersion": 999,
                    "appVersion": "99.0.0",
                    "exportedAt": "2099-01-01T00:00:00Z",
                    "settings": { "timeIncrementSec": 15 },
                    "presets": []
                }
                """.trimIndent()
            futureFile.writeText(futureJson)
            val futureUri = Uri.fromFile(futureFile)

            val result = backupRepository.importFromUri(futureUri)
            assertTrue("Result should be ErrorIncompatibleSchema", result is ImportResult.ErrorIncompatibleSchema)
            assertEquals(999, (result as ImportResult.ErrorIncompatibleSchema).fileSchemaVersion)
        }
}
