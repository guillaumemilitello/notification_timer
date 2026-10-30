package com.notification.timer.core.data.store

import android.content.Context
import com.notification.timer.core.data.json.JsonCodec
import com.notification.timer.core.data.model.LibraryData
import com.notification.timer.core.data.model.RuntimeData
import com.notification.timer.core.domain.diagnostics.EventLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val eventLog: EventLog,
    ) {
        private val libraryMutex = Mutex()
        private val runtimeMutex = Mutex()

        private val libraryFile: File
            get() = File(context.filesDir, "library.json")

        private val runtimeFile: File
            get() = File(context.noBackupFilesDir, "runtime.json")

        suspend fun loadLibraryData(): LibraryData =
            libraryMutex.withLock {
                readOrRecover(libraryFile, LibraryData.serializer(), LibraryData())
            }

        suspend fun saveLibraryData(data: LibraryData) =
            libraryMutex.withLock {
                writeAtomic(libraryFile, LibraryData.serializer(), data)
            }

        suspend fun loadRuntimeData(): RuntimeData =
            runtimeMutex.withLock {
                readOrRecover(runtimeFile, RuntimeData.serializer(), RuntimeData())
            }

        suspend fun saveRuntimeData(data: RuntimeData) =
            runtimeMutex.withLock {
                writeAtomic(runtimeFile, RuntimeData.serializer(), data)
            }

        private suspend fun <T> readOrRecover(
            file: File,
            serializer: KSerializer<T>,
            default: T,
        ): T =
            withContext(Dispatchers.IO) {
                if (!file.exists()) return@withContext default

                try {
                    val content = file.readText()
                    JsonCodec.json.decodeFromString(serializer, content)
                } catch (e: Exception) {
                    eventLog.log("DataStoreStore", "Corrupt file detected at ${file.name}: ${e.message}")
                    val corruptFile = File(file.parentFile, "${file.nameWithoutExtension}_corrupt_${System.currentTimeMillis()}.json")
                    file.renameTo(corruptFile)
                    default
                }
            }

        private suspend fun <T> writeAtomic(
            file: File,
            serializer: KSerializer<T>,
            data: T,
        ) = withContext(Dispatchers.IO) {
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            val content = JsonCodec.json.encodeToString(serializer, data)
            tempFile.writeText(content)
            if (!tempFile.renameTo(file)) {
                file.delete()
                tempFile.renameTo(file)
            }
        }
    }
