package com.iperf3.android.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iperf3.android.di.IoDispatcher
import com.iperf3.android.domain.model.TestConfiguration
import com.iperf3.android.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PreferencesRepository] backed by DataStore.
 *
 * Stores and retrieves user preferences including server settings,
 * test defaults, automated testing configuration, and UI settings.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PreferencesRepository {

    private companion object PreferencesKeys {
        val SERVER_PORT = intPreferencesKey("server_port")
        val LAST_SERVER_HOST = stringPreferencesKey("last_server_host")
        val DEFAULT_DURATION = intPreferencesKey("default_duration")
        val DEFAULT_PARALLEL_STREAMS = intPreferencesKey("default_parallel_streams")
        val DEFAULT_PROTOCOL = stringPreferencesKey("default_protocol")
        val DEFAULT_BANDWIDTH_LIMIT = longPreferencesKey("default_bandwidth_limit")
        val AUTO_TEST_ENABLED = booleanPreferencesKey("auto_test_enabled")
        val AUTO_TEST_INTERVAL = longPreferencesKey("auto_test_interval")
        val AUTO_TEST_SERVER_HOST = stringPreferencesKey("auto_test_server_host")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
    }

    // ==================== Server Settings ====================

    override fun getServerPort(): Flow<Int> {
        return dataStore.data
            .map { preferences -> preferences[SERVER_PORT] ?: 5201 }
            .flowOn(ioDispatcher)
    }

    override suspend fun setServerPort(port: Int) {
        dataStore.edit { preferences -> preferences[SERVER_PORT] = port }
    }

    override fun getLastServerHost(): Flow<String> {
        return dataStore.data
            .map { preferences -> preferences[LAST_SERVER_HOST] ?: "" }
            .flowOn(ioDispatcher)
    }

    override suspend fun setLastServerHost(host: String) {
        dataStore.edit { preferences -> preferences[LAST_SERVER_HOST] = host }
    }

    // ==================== Test Settings ====================

    override fun getDefaultDuration(): Flow<Int> {
        return dataStore.data
            .map { preferences -> preferences[DEFAULT_DURATION] ?: 10 }
            .flowOn(ioDispatcher)
    }

    override suspend fun setDefaultDuration(seconds: Int) {
        dataStore.edit { preferences -> preferences[DEFAULT_DURATION] = seconds }
    }

    override fun getDefaultParallelStreams(): Flow<Int> {
        return dataStore.data
            .map { preferences -> preferences[DEFAULT_PARALLEL_STREAMS] ?: 1 }
            .flowOn(ioDispatcher)
    }

    override suspend fun setDefaultParallelStreams(streams: Int) {
        dataStore.edit { preferences -> preferences[DEFAULT_PARALLEL_STREAMS] = streams }
    }

    override fun getDefaultProtocol(): Flow<String> {
        return dataStore.data
            .map { preferences -> preferences[DEFAULT_PROTOCOL] ?: "TCP" }
            .flowOn(ioDispatcher)
    }

    override suspend fun setDefaultProtocol(protocol: String) {
        dataStore.edit { preferences -> preferences[DEFAULT_PROTOCOL] = protocol }
    }

    override fun getDefaultBandwidthLimit(): Flow<Long> {
        return dataStore.data
            .map { preferences -> preferences[DEFAULT_BANDWIDTH_LIMIT] ?: 0L }
            .flowOn(ioDispatcher)
    }

    override suspend fun setDefaultBandwidthLimit(bps: Long) {
        dataStore.edit { preferences -> preferences[DEFAULT_BANDWIDTH_LIMIT] = bps }
    }

    // ==================== Automated Testing ====================

    override fun getAutoTestEnabled(): Flow<Boolean> {
        return dataStore.data
            .map { preferences -> preferences[AUTO_TEST_ENABLED] ?: false }
            .flowOn(ioDispatcher)
    }

    override suspend fun setAutoTestEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[AUTO_TEST_ENABLED] = enabled }
    }

    override fun getAutoTestInterval(): Flow<Long> {
        return dataStore.data
            .map { preferences -> preferences[AUTO_TEST_INTERVAL] ?: 3600000L }
            .flowOn(ioDispatcher)
    }

    override suspend fun setAutoTestInterval(intervalMs: Long) {
        dataStore.edit { preferences -> preferences[AUTO_TEST_INTERVAL] = intervalMs }
    }

    override fun getAutoTestServerHost(): Flow<String> {
        return dataStore.data
            .map { preferences -> preferences[AUTO_TEST_SERVER_HOST] ?: "" }
            .flowOn(ioDispatcher)
    }

    override suspend fun setAutoTestServerHost(host: String) {
        dataStore.edit { preferences -> preferences[AUTO_TEST_SERVER_HOST] = host }
    }

    // ==================== UI Settings ====================

    override fun getDarkModeEnabled(): Flow<Boolean> {
        return dataStore.data
            .map { preferences -> preferences[DARK_MODE_ENABLED] ?: false }
            .flowOn(ioDispatcher)
    }

    override suspend fun setDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[DARK_MODE_ENABLED] = enabled }
    }

    override fun getShowNotifications(): Flow<Boolean> {
        return dataStore.data
            .map { preferences -> preferences[SHOW_NOTIFICATIONS] ?: true }
            .flowOn(ioDispatcher)
    }

    override suspend fun setShowNotifications(show: Boolean) {
        dataStore.edit { preferences -> preferences[SHOW_NOTIFICATIONS] = show }
    }

    // ==================== Data Management ====================

    override fun getHistoryRetentionDays(): Flow<Int> {
        return dataStore.data
            .map { preferences -> preferences[HISTORY_RETENTION_DAYS] ?: 30 }
            .flowOn(ioDispatcher)
    }

    override suspend fun setHistoryRetentionDays(days: Int) {
        dataStore.edit { preferences -> preferences[HISTORY_RETENTION_DAYS] = days }
    }

    // ==================== Helpers ====================

    override suspend fun createDefaultConfiguration(serverHost: String?): TestConfiguration =
        withContext(ioDispatcher) {
            val host = serverHost ?: getLastServerHost().first()
            val port = getServerPort().first()
            val duration = getDefaultDuration().first()
            val parallelStreams = getDefaultParallelStreams().first()
            val protocol = getDefaultProtocol().first()
            val bandwidthLimit = getDefaultBandwidthLimit().first()

            TestConfiguration(
                serverHost = host,
                serverPort = port,
                protocol = TestConfiguration.Protocol.fromString(protocol),
                duration = duration * 1000L,
                numStreams = parallelStreams,
                bandwidthLimit = if (bandwidthLimit > 0) bandwidthLimit else null
            )
        }

    override suspend fun saveConfigurationAsDefaults(config: TestConfiguration) {
        setLastServerHost(config.serverHost)
        setServerPort(config.serverPort)
        setDefaultDuration((config.duration / 1000).toInt())
        setDefaultParallelStreams(config.numStreams)
        setDefaultProtocol(config.protocol.name)
        setDefaultBandwidthLimit(config.bandwidthLimit ?: 0L)
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
