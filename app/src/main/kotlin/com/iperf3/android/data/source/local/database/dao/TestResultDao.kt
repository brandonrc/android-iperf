package com.iperf3.android.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.iperf3.android.data.source.local.database.entity.TestResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TestResultEntity.
 */
@Dao
interface TestResultDao {

    /**
     * Inserts a new test result.
     *
     * @param result The test result entity to insert
     * @return The ID of the inserted row
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TestResultEntity): Long

    /**
     * Updates an existing test result.
     *
     * @param result The test result entity to update
     */
    @Update
    suspend fun update(result: TestResultEntity)

    /**
     * Gets all test results ordered by timestamp descending.
     *
     * @return Flow of all test results
     */
    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TestResultEntity>>

    /**
     * Gets a test result by ID.
     *
     * @param id The test result ID
     * @return The test result entity or null
     */
    @Query("SELECT * FROM test_results WHERE id = :id")
    suspend fun getById(id: Long): TestResultEntity?

    /**
     * Gets a test result by ID as a Flow.
     *
     * @param id The test result ID
     * @return Flow of the test result or null
     */
    @Query("SELECT * FROM test_results WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<TestResultEntity?>

    /**
     * Gets the most recent test result.
     *
     * @return Flow of the most recent test result or null
     */
    @Query("SELECT * FROM test_results ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecent(): Flow<TestResultEntity?>

    /**
     * Gets test results for a specific server.
     *
     * @param serverHost The server hostname or IP
     * @return Flow of test results for that server
     */
    @Query("SELECT * FROM test_results WHERE server_host = :serverHost ORDER BY timestamp DESC")
    fun getByServer(serverHost: String): Flow<List<TestResultEntity>>

    /**
     * Gets test results within a time range.
     *
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return Flow of test results in the range
     */
    @Query("SELECT * FROM test_results WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getInRange(startTime: Long, endTime: Long): Flow<List<TestResultEntity>>

    /**
     * Gets test results by protocol.
     *
     * @param protocol The protocol (TCP or UDP)
     * @return Flow of test results for that protocol
     */
    @Query("SELECT * FROM test_results WHERE protocol = :protocol ORDER BY timestamp DESC")
    fun getByProtocol(protocol: String): Flow<List<TestResultEntity>>

    /**
     * Gets the total count of test results.
     *
     * @return Flow of the count
     */
    @Query("SELECT COUNT(*) FROM test_results")
    fun getCount(): Flow<Int>

    /**
     * Deletes a test result by ID.
     *
     * @param id The test result ID
     */
    @Query("DELETE FROM test_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Deletes all test results older than the specified timestamp.
     *
     * @param cutoffTime The cutoff timestamp
     * @return Number of rows deleted
     */
    @Query("DELETE FROM test_results WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int

    /**
     * Deletes all test results.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM test_results")
    suspend fun deleteAll(): Int

    /**
     * Gets all test result IDs.
     *
     * @return List of all test result IDs
     */
    @Query("SELECT id FROM test_results ORDER BY timestamp DESC")
    suspend fun getAllIds(): List<Long>

    /**
     * Gets test results by IDs.
     *
     * @param ids List of test result IDs
     * @return List of test result entities
     */
    @Query("SELECT * FROM test_results WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TestResultEntity>
}
