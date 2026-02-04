package com.iperf3.android.data.source.local.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iperf3.android.data.source.local.database.entity.IntervalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for IntervalEntity.
 */
@Dao
interface IntervalDao {

    /**
     * Inserts a single interval.
     *
     * @param interval The interval entity to insert
     * @return The ID of the inserted row
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interval: IntervalEntity): Long

    /**
     * Inserts multiple intervals.
     *
     * @param intervals List of interval entities to insert
     * @return List of inserted row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(intervals: List<IntervalEntity>): List<Long>

    /**
     * Gets all intervals for a test, ordered by start time.
     *
     * @param testId The parent test result ID
     * @return Flow of intervals for that test
     */
    @Query("SELECT * FROM intervals WHERE test_id = :testId ORDER BY start_time ASC")
    fun getByTestId(testId: Long): Flow<List<IntervalEntity>>

    /**
     * Gets all intervals for a test synchronously.
     *
     * @param testId The parent test result ID
     * @return List of intervals for that test
     */
    @Query("SELECT * FROM intervals WHERE test_id = :testId ORDER BY start_time ASC")
    suspend fun getByTestIdSync(testId: Long): List<IntervalEntity>

    /**
     * Gets intervals for a specific stream within a test.
     *
     * @param testId The parent test result ID
     * @param streamId The stream ID
     * @return Flow of intervals for that stream
     */
    @Query("SELECT * FROM intervals WHERE test_id = :testId AND stream_id = :streamId ORDER BY start_time ASC")
    fun getByTestIdAndStream(testId: Long, streamId: Int): Flow<List<IntervalEntity>>

    /**
     * Gets the aggregated (summed) intervals for a test.
     *
     * For tests with parallel streams, this returns intervals where
     * stream_id = -1 (which indicates aggregated data).
     *
     * @param testId The parent test result ID
     * @return Flow of aggregated intervals
     */
    @Query("SELECT * FROM intervals WHERE test_id = :testId AND stream_id = -1 ORDER BY start_time ASC")
    fun getAggregatedByTestId(testId: Long): Flow<List<IntervalEntity>>

    /**
     * Gets the count of intervals for a test.
     *
     * @param testId The parent test result ID
     * @return Number of intervals
     */
    @Query("SELECT COUNT(*) FROM intervals WHERE test_id = :testId")
    suspend fun getCountByTestId(testId: Long): Int

    /**
     * Deletes all intervals for a test.
     *
     * Note: This is usually not needed because of the CASCADE delete
     * relationship with test_results, but is provided for explicit cleanup.
     *
     * @param testId The parent test result ID
     */
    @Query("DELETE FROM intervals WHERE test_id = :testId")
    suspend fun deleteByTestId(testId: Long)

    /**
     * Deletes all intervals.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM intervals")
    suspend fun deleteAll(): Int

    /**
     * Gets the average bandwidth for a test.
     *
     * @param testId The parent test result ID
     * @return Average bandwidth in bits per second
     */
    @Query("SELECT AVG(bits_per_second) FROM intervals WHERE test_id = :testId")
    suspend fun getAverageBandwidth(testId: Long): Double?

    /**
     * Gets the min and max bandwidth for a test.
     *
     * @param testId The parent test result ID
     * @return Pair of (min, max) bandwidth in bits per second
     */
    @Query("SELECT MIN(bits_per_second), MAX(bits_per_second) FROM intervals WHERE test_id = :testId")
    suspend fun getBandwidthRange(testId: Long): BandwidthRange?

    /**
     * Gets the total bytes transferred for a test.
     *
     * @param testId The parent test result ID
     * @return Total bytes transferred
     */
    @Query("SELECT SUM(bytes_transferred) FROM intervals WHERE test_id = :testId")
    suspend fun getTotalBytes(testId: Long): Long?
}

/**
 * Data class for bandwidth range query result.
 */
data class BandwidthRange(
    @ColumnInfo(name = "MIN(bits_per_second)") val min: Double?,
    @ColumnInfo(name = "MAX(bits_per_second)") val max: Double?
)
