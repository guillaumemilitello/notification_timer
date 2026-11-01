package com.notification.timer.core.data.repository

import android.content.Context
import android.net.Uri
import com.notification.timer.core.data.json.JsonCodec
import com.notification.timer.core.data.model.ExportData
import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.diagnostics.EventLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImportResult {
    data class Success(
        val presetsCount: Int,
    ) : ImportResult

    data class ErrorIncompatibleSchema(
        val fileSchemaVersion: Int,
    ) : ImportResult

    data class ErrorInvalidFormat(
        val message: String,
    ) : ImportResult
}

@Singleton
class BackupRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStoreStore: DataStoreStore,
        private val eventLog: EventLog,
    ) {
        companion object {
            const val CURRENT_SCHEMA_VERSION = 1
        }

        suspend fun exportToUri(uri: Uri): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val libraryData = dataStoreStore.loadLibraryData()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    val exportData =
                        ExportData(
                            schemaVersion = CURRENT_SCHEMA_VERSION,
                            appVersion = "2.0.0",
                            exportedAt = dateFormat.format(Date()),
                            settings = libraryData.settings,
                            presets = libraryData.presets,
                        )

                    val jsonString = JsonCodec.json.encodeToString(ExportData.serializer(), exportData)
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    eventLog.i("BackupRepository", "Exported ${libraryData.presets.size} presets successfully")
                    true
                } catch (e: Exception) {
                    eventLog.e("BackupRepository", "Export failed: ${e.message}", e)
                    false
                }
            }

        suspend fun importFromUri(uri: Uri): ImportResult =
            withContext(Dispatchers.IO) {
                try {
                    val jsonString =
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.bufferedReader().readText()
                        } ?: return@withContext ImportResult.ErrorInvalidFormat("Empty or unreadable file")

                    val exportData = JsonCodec.json.decodeFromString(ExportData.serializer(), jsonString)

                    if (exportData.schemaVersion > CURRENT_SCHEMA_VERSION) {
                        return@withContext ImportResult.ErrorIncompatibleSchema(exportData.schemaVersion)
                    }

                    val currentLibrary = dataStoreStore.loadLibraryData()
                    val updatedLibrary =
                        currentLibrary.copy(
                            settings = exportData.settings,
                            presets = exportData.presets,
                        )
                    dataStoreStore.saveLibraryData(updatedLibrary)

                    eventLog.i("BackupRepository", "Imported ${exportData.presets.size} presets successfully")
                    ImportResult.Success(exportData.presets.size)
                } catch (e: Exception) {
                    eventLog.e("BackupRepository", "Import failed: ${e.message}", e)
                    ImportResult.ErrorInvalidFormat(e.message ?: "Unknown error")
                }
            }
    }
