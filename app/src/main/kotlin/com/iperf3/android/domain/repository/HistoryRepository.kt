package com.iperf3.android.domain.repository

import com.iperf3.android.domain.model.IntervalResult
import com.iperf3.android.domain.model.TestResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for test history persistence.
 *
 * This interface defines the contract for storing and retrieving
 * test results. Implementations handle database operations.
 */
interface HistoryRepository {

    /**
     * Saves a test result to the database.
     *
     * @param result The test result to save
     * @return The ID of the saved result
     */
    suspend fun saveTestResult(result: TestResult): Long

    /**
     * Gets all test results, ordered by timestamp descending.
     *
     * @return A Flow of all test results
     */
    fun getAllResults(): Flow<List<TestResult>>

    /**
     * Gets a single test result by ID.
     *
     * @param id The test result ID
     * @return A Flow of the test result, or null if not found
     */
    fun getResult(id: Long): Flow<TestResult?>

    /**
     * Gets the interval results for a specific test.
     *
     * @param testId The test result ID
     * @return A Flow of interval results
     */
    fun getIntervals(testId: Long): Flow<List<IntervalResult>>

    /**
     * Deletes a test result and its associated intervals.
     *
     * @param id The test result ID to delete
     */
    suspend fun deleteResult(id: Long)

    /**
     * Deletes all test results older than the specified number of days.
     *
     * @param days Number of days to keep
     * @return Number of results deleted
     */
    suspend fun deleteOlderThan(days: Int): Int

    /**
     * Deletes all test results.
     *
     * @return Number of results deleted
     */
    suspend fun deleteAll(): Int

    /**
     * Gets the total count of test results.
     *
     * @return A Flow of the count
     */
    fun getCount(): Flow<Int>

    /**
     * Gets test results for a specific server.
     *
     * @param serverHost The server hostname or IP
     * @return A Flow of test results for that server
     */
    fun getResultsForServer(serverHost: String): Flow<List<TestResult>>

    /**
     * Gets the most recent test result.
     *
     * @return A Flow of the most recent result, or null if none exist
     */
    fun getMostRecent(): Flow<TestResult?>

    /**
     * Gets test results within a time range.
     *
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return A Flow of test results in the time range
     */
    fun getResultsInRange(startTime: Long, endTime: Long): Flow<List<TestResult>>

    /**
     * Exports test results to JSON.
     *
     * @param ids Optional list of specific result IDs to export (all if null)
     * @return JSON string containing the exported results
     */
    suspend fun exportToJson(ids: List<Long>? = null): String

    /**
     * Imports test results from JSON.
     *
     * @param json JSON string containing test results
     * @return Number of results imported
     */
    suspend fun importFromJson(json: String): Int
}
