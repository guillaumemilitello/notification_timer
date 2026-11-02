package com.notification.timer.core.data.repository

import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.ports.DisplayPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisplayPreferencesRepository
    @Inject
    constructor(
        private val dataStoreStore: DataStoreStore,
        private val eventLog: EventLog,
    ) : DisplayPreferences {
        override suspend fun loadKeepScreenOn(): Boolean = dataStoreStore.loadLibraryData().settings.keepScreenOn

        override suspend fun saveKeepScreenOn(enabled: Boolean) {
            val current = dataStoreStore.loadLibraryData()
            dataStoreStore.saveLibraryData(
                current.copy(settings = current.settings.copy(keepScreenOn = enabled)),
            )
            eventLog.d(TAG, "Saved keepScreenOn=$enabled")
        }

        private companion object {
            const val TAG = "DisplayPreferences"
        }
    }
