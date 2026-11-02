package com.notification.timer.core.data.repository

import android.content.Context
import android.net.Uri
import com.notification.timer.core.data.json.JsonCodec
import com.notification.timer.core.data.model.ExportData
import com.notification.timer.core.data.model.HistoryRecord
import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.diagnostics.EventLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImportResult {
    data class Success(
        val historyCount: Int,
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
            const val CURRENT_SCHEMA_VERSION = 2
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
                            timersById = libraryData.timersById,
                            history = libraryData.history,
                        )

                    val jsonString = JsonCodec.json.encodeToString(ExportData.serializer(), exportData)
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    eventLog.i("BackupRepository", "Exported ${libraryData.history.size} history timers successfully")
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
                    val timersMap = currentLibrary.timersById.toMutableMap()
                    val historyList = currentLibrary.history.toMutableList()

                    // Import v2 timersById and history if available
                    if (exportData.timersById.isNotEmpty() || exportData.history.isNotEmpty()) {
                        timersMap.putAll(exportData.timersById)
                        for (incoming in exportData.history) {
                            val existingIndex = historyList.indexOfFirst { it.timerId == incoming.timerId }
                            if (existingIndex != -1) {
                                val currentRecord = historyList[existingIndex]
                                historyList[existingIndex] =
                                    currentRecord.copy(
                                        useCount = maxOf(currentRecord.useCount, incoming.useCount),
                                        lastUsedAtMs = maxOf(currentRecord.lastUsedAtMs, incoming.lastUsedAtMs),
                                        isStarred = currentRecord.isStarred || incoming.isStarred,
                                    )
                            } else {
                                historyList.add(incoming)
                            }
                        }
                    }

                    // Import legacy v1 presets if present
                    if (exportData.presets.isNotEmpty()) {
                        var now = System.currentTimeMillis()
                        for (legacy in exportData.presets) {
                            val id = legacy.id.ifBlank { UUID.randomUUID().toString() }
                            if (!timersMap.containsKey(id)) {
                                timersMap[id] = legacy.copy(id = id)
                                historyList.add(
                                    HistoryRecord(
                                        timerId = id,
                                        useCount = 1,
                                        lastUsedAtMs = now++,
                                        isStarred = false,
                                    ),
                                )
                            }
                        }
                    }

                    val updatedLibrary =
                        currentLibrary.copy(
                            settings = exportData.settings,
                            timersById = timersMap,
                            history = historyList,
                        )
                    dataStoreStore.saveLibraryData(updatedLibrary)

                    eventLog.i("BackupRepository", "Imported ${historyList.size} history timers successfully")
                    ImportResult.Success(historyList.size)
                } catch (e: Exception) {
                    eventLog.e("BackupRepository", "Import failed: ${e.message}", e)
                    ImportResult.ErrorInvalidFormat(e.message ?: "Unknown error")
                }
            }
    }
