package com.iperf3.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.iperf3.android.data.repository.HistoryRepositoryImpl
import com.iperf3.android.data.repository.IPerf3RepositoryImpl
import com.iperf3.android.data.repository.PreferencesRepositoryImpl
import com.iperf3.android.data.source.local.database.AppDatabase
import com.iperf3.android.data.source.local.database.dao.IntervalDao
import com.iperf3.android.data.source.local.database.dao.TestResultDao
import com.iperf3.android.domain.repository.HistoryRepository
import com.iperf3.android.domain.repository.IPerf3Repository
import com.iperf3.android.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "iperf3_preferences"
)

/**
 * Hilt module that provides data layer dependencies.
 *
 * This module provides the Room database, DAOs, DataStore,
 * and binds repository implementations to their interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideTestResultDao(database: AppDatabase): TestResultDao {
        return database.testResultDao()
    }

    @Provides
    fun provideIntervalDao(database: AppDatabase): IntervalDao {
        return database.intervalDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}

/**
 * Hilt module that binds repository interfaces to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindIPerf3Repository(
        impl: IPerf3RepositoryImpl
    ): IPerf3Repository
}
