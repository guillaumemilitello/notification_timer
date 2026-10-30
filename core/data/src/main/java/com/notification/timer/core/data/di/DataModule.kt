package com.notification.timer.core.data.di

import com.notification.timer.core.data.diagnostics.InMemoryEventLog
import com.notification.timer.core.data.repository.LibraryRepositoryImpl
import com.notification.timer.core.data.repository.TimerStoreImpl
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.ports.LibraryRepository
import com.notification.timer.core.domain.ports.TimerStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindEventLog(impl: InMemoryEventLog): EventLog

    @Binds
    @Singleton
    abstract fun bindTimerStore(impl: TimerStoreImpl): TimerStore

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository
}
