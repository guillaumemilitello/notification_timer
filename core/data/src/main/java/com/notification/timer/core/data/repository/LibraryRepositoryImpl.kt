package com.notification.timer.core.data.repository

import com.notification.timer.core.data.model.UserSettings
import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.model.Preset
import com.notification.timer.core.domain.ports.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl
    @Inject
    constructor(
        private val dataStoreStore: DataStoreStore,
    ) : LibraryRepository {
        override suspend fun savePresets(presets: List<Preset>) {
            val current = dataStoreStore.loadLibraryData()
            dataStoreStore.saveLibraryData(current.copy(presets = presets))
        }

        override suspend fun loadPresets(): List<Preset> = dataStoreStore.loadLibraryData().presets

        suspend fun saveSettings(settings: UserSettings) {
            val current = dataStoreStore.loadLibraryData()
            dataStoreStore.saveLibraryData(current.copy(settings = settings))
        }

        suspend fun loadSettings(): UserSettings = dataStoreStore.loadLibraryData().settings
    }
