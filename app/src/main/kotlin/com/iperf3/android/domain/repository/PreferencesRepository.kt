package com.iperf3.android.domain.repository

import com.iperf3.android.domain.model.TestConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app preferences.
 *
 * This interface defines the contract for storing and retrieving
 * user preferences using DataStore.
 */
interface PreferencesRepository {

    // ==================== Server Settings ====================

    /**
     * Gets the default server port.
     */
    fun getServerPort(): Flow<Int>

    /**
     * Sets the default server port.
     */
    suspend fun setServerPort(port: Int)

    /**
     * Gets the last used server host.
     */
    fun getLastServerHost(): Flow<String>

    /**
     * Sets the last used server host.
     */
    suspend fun setLastServerHost(host: String)

    // ==================== Test Settings ====================

    /**
     * Gets the default test duration in seconds.
     */
    fun getDefaultDuration(): Flow<Int>

    /**
     * Sets the default test duration in seconds.
     */
    suspend fun setDefaultDuration(seconds: Int)

    /**
     * Gets the default number of parallel streams.
     */
    fun getDefaultParallelStreams(): Flow<Int>

    /**
     * Sets the default number of parallel streams.
     */
    suspend fun setDefaultParallelStreams(streams: Int)

    /**
     * Gets the default protocol (TCP or UDP).
     */
    fun getDefaultProtocol(): Flow<String>

    /**
     * Sets the default protocol.
     */
    suspend fun setDefaultProtocol(protocol: String)

    /**
     * Gets the default bandwidth limit in bits per second (0 for unlimited).
     */
    fun getDefaultBandwidthLimit(): Flow<Long>

    /**
     * Sets the default bandwidth limit.
     */
    suspend fun setDefaultBandwidthLimit(bps: Long)

    // ==================== Automated Testing ====================

    /**
     * Gets whether automated testing is enabled.
     */
    fun getAutoTestEnabled(): Flow<Boolean>

    /**
     * Sets whether automated testing is enabled.
     */
    suspend fun setAutoTestEnabled(enabled: Boolean)

    /**
     * Gets the automated test interval in milliseconds.
     */
    fun getAutoTestInterval(): Flow<Long>

    /**
     * Sets the automated test interval.
     */
    suspend fun setAutoTestInterval(intervalMs: Long)

    /**
     * Gets the server host for automated tests.
     */
    fun getAutoTestServerHost(): Flow<String>

    /**
     * Sets the server host for automated tests.
     */
    suspend fun setAutoTestServerHost(host: String)

    // ==================== UI Settings ====================

    /**
     * Gets whether dark mode is enabled.
     */
    fun getDarkModeEnabled(): Flow<Boolean>

    /**
     * Sets whether dark mode is enabled.
     */
    suspend fun setDarkModeEnabled(enabled: Boolean)

    /**
     * Gets whether to show notifications for test results.
     */
    fun getShowNotifications(): Flow<Boolean>

    /**
     * Sets whether to show notifications.
     */
    suspend fun setShowNotifications(show: Boolean)

    // ==================== Data Management ====================

    /**
     * Gets the number of days to retain test history.
     */
    fun getHistoryRetentionDays(): Flow<Int>

    /**
     * Sets the number of days to retain test history.
     */
    suspend fun setHistoryRetentionDays(days: Int)

    // ==================== Helpers ====================

    /**
     * Creates a TestConfiguration from the stored preferences.
     *
     * @param serverHost The server host to use (defaults to last used)
     * @return A TestConfiguration with preference values
     */
    suspend fun createDefaultConfiguration(serverHost: String? = null): TestConfiguration

    /**
     * Saves the relevant settings from a TestConfiguration as defaults.
     *
     * @param config The configuration to save settings from
     */
    suspend fun saveConfigurationAsDefaults(config: TestConfiguration)

    /**
     * Clears all preferences and resets to defaults.
     */
    suspend fun clearAll()
}
