package com.iperf3.android.data.source.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing individual interval results.
 *
 * Each test consists of multiple intervals (typically one per second).
 * This entity stores the metrics for each interval.
 */
@Entity(
    tableName = "intervals",
    foreignKeys = [
        ForeignKey(
            entity = TestResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["test_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["test_id"]),
        Index(value = ["test_id", "stream_id"])
    ]
)
data class IntervalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "test_id")
    val testId: Long,

    @ColumnInfo(name = "stream_id")
    val streamId: Int,

    @ColumnInfo(name = "start_time")
    val startTime: Double,

    @ColumnInfo(name = "end_time")
    val endTime: Double,

    @ColumnInfo(name = "bytes_transferred")
    val bytesTransferred: Long,

    @ColumnInfo(name = "bits_per_second")
    val bitsPerSecond: Double,

    @ColumnInfo(name = "retransmits")
    val retransmits: Int?,

    @ColumnInfo(name = "congestion_window")
    val congestionWindow: Long?,

    @ColumnInfo(name = "jitter_ms")
    val jitterMs: Double?,

    @ColumnInfo(name = "packets")
    val packets: Long?,

    @ColumnInfo(name = "lost_packets")
    val lostPackets: Long?,

    @ColumnInfo(name = "out_of_order_packets")
    val outOfOrderPackets: Long?
)
