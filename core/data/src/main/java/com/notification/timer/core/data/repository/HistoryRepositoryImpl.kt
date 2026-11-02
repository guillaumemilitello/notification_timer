package com.notification.timer.core.data.repository

import com.notification.timer.core.data.model.HistoryRecord
import com.notification.timer.core.data.model.LibraryData
import com.notification.timer.core.data.model.UserSettings
import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.model.HistoryEntry
import com.notification.timer.core.domain.model.TimerConfig
import com.notification.timer.core.domain.ports.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl
    @Inject
    constructor(
        private val dataStoreStore: DataStoreStore,
    ) : HistoryRepository {
        private val mutex = Mutex()
        private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val libraryDataFlow = MutableStateFlow(LibraryData())
        private var isInitialized = false

        init {
            repositoryScope.launch {
                mutex.withLock {
                    ensureInitializedLocked()
                }
            }
        }

        private suspend fun ensureInitializedLocked() {
            if (!isInitialized) {
                val loaded = dataStoreStore.loadLibraryData()
                val migrated = maybeMigrateLegacyPresets(loaded)
                if (migrated != loaded) {
                    dataStoreStore.saveLibraryData(migrated)
                }
                libraryDataFlow.value = migrated
                isInitialized = true
            }
        }

        override val history: Flow<List<HistoryEntry>> =
            libraryDataFlow
                .onStart {
                    mutex.withLock {
                        ensureInitializedLocked()
                    }
                }.map { data ->
                    data.history
                        .mapNotNull { record ->
                            val config = data.timersById[record.timerId] ?: return@mapNotNull null
                            HistoryEntry(
                                timerId = record.timerId,
                                config = config,
                                useCount = record.useCount,
                                lastUsedAtMs = record.lastUsedAtMs,
                                isStarred = record.isStarred,
                            )
                        }.sortedWith(
                            compareByDescending<HistoryEntry> { it.isStarred }
                                .thenByDescending { it.useCount }
                                .thenByDescending { it.lastUsedAtMs },
                        )
                }.distinctUntilChanged()

        override val maxHistorySize: Flow<Int> =
            libraryDataFlow
                .onStart {
                    mutex.withLock {
                        ensureInitializedLocked()
                    }
                }.map { it.settings.maxHistorySize }
                .distinctUntilChanged()

        override suspend fun recordTimerUsed(
            config: TimerConfig,
            nowEpochMs: Long,
        ) {
            mutex.withLock {
                ensureInitializedLocked()
                val current = libraryDataFlow.value
                val timersMap = current.timersById.toMutableMap()
                val historyList = current.history.toMutableList()

                val targetFingerprint = config.fingerprint()
                val existingEntry =
                    timersMap.entries.firstOrNull { it.value.fingerprint() == targetFingerprint }

                val timerId: String
                if (existingEntry != null) {
                    timerId = existingEntry.key
                    // Update timer definition if fields like name capitalization or color changed
                    timersMap[timerId] = config.copy(id = timerId)
                    val historyIndex = historyList.indexOfFirst { it.timerId == timerId }
                    if (historyIndex != -1) {
                        val record = historyList[historyIndex]
                        historyList[historyIndex] =
                            record.copy(
                                useCount = record.useCount + 1,
                                lastUsedAtMs = nowEpochMs,
                            )
                    } else {
                        historyList.add(
                            HistoryRecord(
                                timerId = timerId,
                                useCount = 1,
                                lastUsedAtMs = nowEpochMs,
                                isStarred = false,
                            ),
                        )
                    }
                } else {
                    timerId = config.id.ifBlank { UUID.randomUUID().toString() }
                    timersMap[timerId] = config.copy(id = timerId)
                    historyList.add(
                        HistoryRecord(
                            timerId = timerId,
                            useCount = 1,
                            lastUsedAtMs = nowEpochMs,
                            isStarred = false,
                        ),
                    )
                }

                // Prune excess unstarred entries if exceeding maxHistorySize
                val maxSize = current.settings.maxHistorySize
                pruneHistoryLocked(historyList, timersMap, maxSize, justAddedId = timerId)

                val updated =
                    current.copy(
                        timersById = timersMap,
                        history = historyList,
                    )
                dataStoreStore.saveLibraryData(updated)
                libraryDataFlow.value = updated
            }
        }

        override suspend fun updateTimerConfig(config: TimerConfig) {
            mutex.withLock {
                ensureInitializedLocked()
                val current = libraryDataFlow.value
                val timersMap = current.timersById.toMutableMap()
                if (timersMap.containsKey(config.id)) {
                    timersMap[config.id] = config
                    val updated = current.copy(timersById = timersMap)
                    dataStoreStore.saveLibraryData(updated)
                    libraryDataFlow.value = updated
                }
            }
        }

        override suspend fun toggleStar(timerId: String) {
            mutex.withLock {
                ensureInitializedLocked()
                val current = libraryDataFlow.value
                val historyList = current.history.toMutableList()
                val index = historyList.indexOfFirst { it.timerId == timerId }
                if (index != -1) {
                    val record = historyList[index]
                    historyList[index] = record.copy(isStarred = !record.isStarred)
                    val updated = current.copy(history = historyList)
                    dataStoreStore.saveLibraryData(updated)
                    libraryDataFlow.value = updated
                }
            }
        }

        override suspend fun setMaxHistorySize(maxSize: Int) {
            val clamped = maxSize.coerceIn(5, 50)
            mutex.withLock {
                ensureInitializedLocked()
                val current = libraryDataFlow.value
                val timersMap = current.timersById.toMutableMap()
                val historyList = current.history.toMutableList()

                pruneHistoryLocked(historyList, timersMap, clamped, justAddedId = null)

                val updated =
                    current.copy(
                        settings = current.settings.copy(maxHistorySize = clamped),
                        timersById = timersMap,
                        history = historyList,
                    )
                dataStoreStore.saveLibraryData(updated)
                libraryDataFlow.value = updated
            }
        }

        override suspend fun getMaxHistorySize(): Int =
            mutex.withLock {
                ensureInitializedLocked()
                libraryDataFlow.value.settings.maxHistorySize
            }

        suspend fun loadSettings(): UserSettings =
            mutex.withLock {
                ensureInitializedLocked()
                libraryDataFlow.value.settings
            }

        suspend fun saveSettings(settings: UserSettings) {
            mutex.withLock {
                ensureInitializedLocked()
                val current = libraryDataFlow.value
                val timersMap = current.timersById.toMutableMap()
                val historyList = current.history.toMutableList()

                if (settings.maxHistorySize != current.settings.maxHistorySize) {
                    pruneHistoryLocked(historyList, timersMap, settings.maxHistorySize, justAddedId = null)
                }

                val updated =
                    current.copy(
                        settings = settings,
                        timersById = timersMap,
                        history = historyList,
                    )
                dataStoreStore.saveLibraryData(updated)
                libraryDataFlow.value = updated
            }
        }

        private fun pruneHistoryLocked(
            historyList: MutableList<HistoryRecord>,
            timersMap: MutableMap<String, TimerConfig>,
            maxSize: Int,
            justAddedId: String?,
        ) {
            while (historyList.size > maxSize) {
                // Find candidates for eviction: unstarred only, excluding the just-added ID
                val candidate =
                    historyList
                        .filter { !it.isStarred && it.timerId != justAddedId }
                        .minWithOrNull(
                            compareBy<HistoryRecord> { it.useCount }
                                .thenBy { it.lastUsedAtMs },
                        ) ?: break

                historyList.remove(candidate)
            }

            // Remove orphaned timer definitions not referenced by history
            val referencedIds = historyList.map { it.timerId }.toSet()
            timersMap.keys.retainAll(referencedIds)
        }

        private fun maybeMigrateLegacyPresets(data: LibraryData): LibraryData {
            if (data.presets.isEmpty()) return data

            val timersMap = data.timersById.toMutableMap()
            val historyList = data.history.toMutableList()
            var now = System.currentTimeMillis()

            for (legacy in data.presets) {
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

            return data.copy(
                timersById = timersMap,
                history = historyList,
                presets = emptyList(), // cleared after migration
            )
        }
    }
