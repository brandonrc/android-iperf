package com.iperf3.android.data.source.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing iperf3 test results.
 *
 * This entity stores the summary statistics from a completed test,
 * including bandwidth measurements, quality metrics, and metadata.
 */
@Entity(
    tableName = "test_results",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["server_host"]),
        Index(value = ["protocol"])
    ]
)
data class TestResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "test_name")
    val testName: String,

    @ColumnInfo(name = "server_host")
    val serverHost: String,

    @ColumnInfo(name = "server_port")
    val serverPort: Int,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "protocol")
    val protocol: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long,

    @ColumnInfo(name = "avg_bandwidth_bps")
    val avgBandwidthBps: Double,

    @ColumnInfo(name = "min_bandwidth_bps")
    val minBandwidthBps: Double,

    @ColumnInfo(name = "max_bandwidth_bps")
    val maxBandwidthBps: Double,

    @ColumnInfo(name = "jitter_ms")
    val jitterMs: Double?,

    @ColumnInfo(name = "packet_loss_percent")
    val packetLossPercent: Double?,

    @ColumnInfo(name = "total_packets")
    val totalPackets: Long?,

    @ColumnInfo(name = "lost_packets")
    val lostPackets: Long?,

    @ColumnInfo(name = "retransmits")
    val retransmits: Int?,

    @ColumnInfo(name = "quality_score")
    val qualityScore: Float,

    @ColumnInfo(name = "num_streams")
    val numStreams: Int,

    @ColumnInfo(name = "reverse_mode")
    val reverseMode: Boolean,

    @ColumnInfo(name = "bidirectional")
    val bidirectional: Boolean,

    @ColumnInfo(name = "raw_json")
    val rawJson: String,

    @ColumnInfo(name = "error_message")
    val errorMessage: String?
)
