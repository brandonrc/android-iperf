package com.iperf3.android.domain.model

/**
 * Represents the results of a single reporting interval during an iperf3 test.
 *
 * During a test, results are reported at regular intervals (typically every second).
 * This class captures the metrics for one such interval.
 *
 * @property id Unique identifier for this interval (database primary key)
 * @property testId The ID of the parent test this interval belongs to
 * @property streamId The stream index (0-based) for parallel stream tests
 * @property startTime Start time of the interval in seconds from test start
 * @property endTime End time of the interval in seconds from test start
 * @property bytesTransferred Total bytes transferred during this interval
 * @property bitsPerSecond Calculated bandwidth in bits per second
 * @property retransmits Number of TCP retransmissions (TCP only)
 * @property congestionWindow Congestion window size in bytes (TCP only)
 * @property jitter Jitter in milliseconds (UDP only)
 * @property packets Number of packets sent/received (UDP only)
 * @property lostPackets Number of lost packets (UDP only)
 * @property outOfOrderPackets Number of out-of-order packets (UDP only)
 */
data class IntervalResult(
    val id: Long = 0,
    val testId: Long = 0,
    val streamId: Int = 0,
    val startTime: Double,
    val endTime: Double,
    val bytesTransferred: Long,
    val bitsPerSecond: Double,
    val retransmits: Int? = null,
    val congestionWindow: Long? = null,
    val jitter: Double? = null,
    val packets: Long? = null,
    val lostPackets: Long? = null,
    val outOfOrderPackets: Long? = null
) {
    /**
     * Duration of this interval in seconds
     */
    val duration: Double
        get() = endTime - startTime

    /**
     * Calculated packet loss percentage (UDP only)
     * Returns null for TCP tests
     */
    val packetLossPercent: Double?
        get() {
            val total = packets ?: return null
            val lost = lostPackets ?: return null
            return if (total > 0) (lost.toDouble() / total) * 100 else 0.0
        }

    /**
     * Bandwidth in megabits per second
     */
    val mbps: Double
        get() = bitsPerSecond / 1_000_000

    /**
     * Bandwidth in gigabits per second
     */
    val gbps: Double
        get() = bitsPerSecond / 1_000_000_000

    /**
     * Bytes transferred in megabytes
     */
    val megabytesTransferred: Double
        get() = bytesTransferred.toDouble() / (1024 * 1024)

    /**
     * Returns a human-readable bandwidth string
     */
    fun formatBandwidth(): String {
        return when {
            bitsPerSecond >= 1_000_000_000 -> String.format("%.2f Gbps", gbps)
            bitsPerSecond >= 1_000_000 -> String.format("%.2f Mbps", mbps)
            bitsPerSecond >= 1_000 -> String.format("%.2f Kbps", bitsPerSecond / 1_000)
            else -> String.format("%.0f bps", bitsPerSecond)
        }
    }

    /**
     * Returns a human-readable jitter string (UDP only)
     */
    fun formatJitter(): String? {
        return jitter?.let { String.format("%.3f ms", it) }
    }

    companion object {
        /**
         * Creates an empty/zero interval result
         */
        fun empty(testId: Long = 0, startTime: Double = 0.0, endTime: Double = 0.0): IntervalResult {
            return IntervalResult(
                testId = testId,
                startTime = startTime,
                endTime = endTime,
                bytesTransferred = 0,
                bitsPerSecond = 0.0
            )
        }

        /**
         * Aggregates multiple stream intervals into a single combined interval
         * Used when displaying combined results for parallel stream tests
         */
        fun aggregate(intervals: List<IntervalResult>): IntervalResult? {
            if (intervals.isEmpty()) return null

            val totalBytes = intervals.sumOf { it.bytesTransferred }
            val totalBps = intervals.sumOf { it.bitsPerSecond }
            val avgJitter = intervals.mapNotNull { it.jitter }.average().takeIf { !it.isNaN() }
            val totalPackets = intervals.mapNotNull { it.packets }.sum().takeIf { it > 0 }
            val totalLostPackets = intervals.mapNotNull { it.lostPackets }.sum().takeIf { it >= 0 }
            val totalRetransmits = intervals.mapNotNull { it.retransmits }.sum().takeIf { it >= 0 }

            return IntervalResult(
                testId = intervals.first().testId,
                streamId = -1, // Indicates aggregated result
                startTime = intervals.minOf { it.startTime },
                endTime = intervals.maxOf { it.endTime },
                bytesTransferred = totalBytes,
                bitsPerSecond = totalBps,
                retransmits = totalRetransmits,
                jitter = avgJitter,
                packets = totalPackets,
                lostPackets = totalLostPackets
            )
        }
    }
}
