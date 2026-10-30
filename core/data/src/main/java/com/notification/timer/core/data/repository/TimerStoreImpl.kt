package com.notification.timer.core.data.repository

import com.notification.timer.core.data.model.RuntimeData
import com.notification.timer.core.data.store.DataStoreStore
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.ports.TimerStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerStoreImpl
    @Inject
    constructor(
        private val dataStoreStore: DataStoreStore,
    ) : TimerStore {
        override suspend fun saveTimers(timers: List<Timer>) {
            val currentNextId = dataStoreStore.loadRuntimeData().nextTimerId
            val minNextId = maxOf(currentNextId, (timers.maxOfOrNull { it.id } ?: 999) + 1)
            dataStoreStore.saveRuntimeData(RuntimeData(nextTimerId = minNextId, timers = timers))
        }

        override suspend fun saveRuntimeState(
            timers: List<Timer>,
            nextTimerId: Int,
        ) {
            dataStoreStore.saveRuntimeData(RuntimeData(nextTimerId = nextTimerId, timers = timers))
        }

        override suspend fun loadTimers(): List<Timer> = dataStoreStore.loadRuntimeData().timers

        override suspend fun loadNextTimerId(): Int = dataStoreStore.loadRuntimeData().nextTimerId
    }
