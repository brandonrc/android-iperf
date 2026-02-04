package com.iperf3.android.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Represents the complete results of an iperf3 test.
 *
 * This class captures all the summary statistics from a completed test,
 * including bandwidth measurements, quality metrics, and the raw JSON output
 * for full iperf3 compatibility.
 *
 * @property id Unique identifier for this test result (database primary key)
 * @property testName User-defined or auto-generated name for this test
 * @property serverHost The iperf3 server hostname/IP that was tested
 * @property serverPort The server port used
 * @property timestamp Unix timestamp (milliseconds) when the test started
 * @property protocol Transport protocol used ("TCP" or "UDP")
 * @property duration Actual test duration in milliseconds
 * @property totalBytes Total bytes transferred during the test
 * @property avgBandwidth Average bandwidth in bits per second
 * @property minBandwidth Minimum interval bandwidth in bits per second
 * @property maxBandwidth Maximum interval bandwidth in bits per second
 * @property jitter Jitter in milliseconds (UDP only)
 * @property packetLoss Packet loss percentage (UDP only)
 * @property totalPackets Total packets sent/received (UDP only)
 * @property lostPackets Total lost packets (UDP only)
 * @property retransmits Total TCP retransmissions (TCP only)
 * @property qualityScore Calculated quality score (0-100)
 * @property numStreams Number of parallel streams used
 * @property reverseMode Whether reverse mode was enabled
 * @property bidirectional Whether bidirectional mode was enabled
 * @property intervals List of interval results for detailed analysis
 * @property rawJson The complete iperf3 JSON output for compatibility
 * @property errorMessage Error message if the test failed, null otherwise
 */
data class TestResult(
    val id: Long = 0,
    val testName: String,
    val serverHost: String,
    val serverPort: Int = 5201,
    val timestamp: Long,
    val protocol: String,
    val duration: Long,
    val totalBytes: Long,
    val avgBandwidth: Double,
    val minBandwidth: Double,
    val maxBandwidth: Double,
    val jitter: Double? = null,
    val packetLoss: Double? = null,
    val totalPackets: Long? = null,
    val lostPackets: Long? = null,
    val retransmits: Int? = null,
    val qualityScore: Float,
    val numStreams: Int,
    val reverseMode: Boolean,
    val bidirectional: Boolean,
    val intervals: List<IntervalResult> = emptyList(),
    val rawJson: String = "",
    val errorMessage: String? = null
) {
    /**
     * Whether this test completed successfully
     */
    val isSuccess: Boolean
        get() = errorMessage == null

    /**
     * Whether this was a TCP test
     */
    val isTcp: Boolean
        get() = protocol.uppercase() == "TCP"

    /**
     * Whether this was a UDP test
     */
    val isUdp: Boolean
        get() = protocol.uppercase() == "UDP"

    /**
     * Average bandwidth in Mbps
     */
    val avgMbps: Double
        get() = avgBandwidth / 1_000_000

    /**
     * Average bandwidth in Gbps
     */
    val avgGbps: Double
        get() = avgBandwidth / 1_000_000_000

    /**
     * Total data transferred in megabytes
     */
    val totalMegabytes: Double
        get() = totalBytes.toDouble() / (1024 * 1024)

    /**
     * Total data transferred in gigabytes
     */
    val totalGigabytes: Double
        get() = totalBytes.toDouble() / (1024 * 1024 * 1024)

    /**
     * Duration in seconds
     */
    val durationSeconds: Double
        get() = duration / 1000.0

    /**
     * Bandwidth variance (max - min) / avg
     */
    val bandwidthVariance: Double
        get() = if (avgBandwidth > 0) (maxBandwidth - minBandwidth) / avgBandwidth else 0.0

    /**
     * Formatted timestamp as readable date/time
     */
    fun formatTimestamp(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochMilli(timestamp))
    }

    /**
     * Returns a human-readable average bandwidth string
     */
    fun formatAvgBandwidth(): String {
        return when {
            avgBandwidth >= 1_000_000_000 -> String.format("%.2f Gbps", avgGbps)
            avgBandwidth >= 1_000_000 -> String.format("%.2f Mbps", avgMbps)
            avgBandwidth >= 1_000 -> String.format("%.2f Kbps", avgBandwidth / 1_000)
            else -> String.format("%.0f bps", avgBandwidth)
        }
    }

    /**
     * Returns a human-readable data transferred string
     */
    fun formatDataTransferred(): String {
        return when {
            totalBytes >= 1024L * 1024 * 1024 -> String.format("%.2f GB", totalGigabytes)
            totalBytes >= 1024 * 1024 -> String.format("%.2f MB", totalMegabytes)
            totalBytes >= 1024 -> String.format("%.2f KB", totalBytes / 1024.0)
            else -> "$totalBytes bytes"
        }
    }

    /**
     * Returns a human-readable quality score description
     */
    fun qualityDescription(): String {
        return when {
            qualityScore >= 90 -> "Excellent"
            qualityScore >= 75 -> "Good"
            qualityScore >= 50 -> "Fair"
            qualityScore >= 25 -> "Poor"
            else -> "Bad"
        }
    }

    /**
     * Test mode description
     */
    val modeDescription: String
        get() = when {
            bidirectional -> "Bidirectional"
            reverseMode -> "Download (Reverse)"
            else -> "Upload"
        }

    /**
     * Calculates latency percentiles from interval results
     * Returns a map with p50, p95, p99 keys
     */
    fun calculateLatencyPercentiles(): Map<String, Double> {
        val jitters = intervals.mapNotNull { it.jitter }.sorted()
        if (jitters.isEmpty()) return emptyMap()

        fun percentile(p: Double): Double {
            val index = (p / 100.0 * (jitters.size - 1)).toInt()
            return jitters.getOrElse(index) { jitters.last() }
        }

        return mapOf(
            "p50" to percentile(50.0),
            "p95" to percentile(95.0),
            "p99" to percentile(99.0)
        )
    }

    companion object {
        /**
         * Creates an error result
         */
        fun error(
            config: TestConfiguration,
            errorMessage: String
        ): TestResult {
            return TestResult(
                testName = config.generateTestName(),
                serverHost = config.serverHost,
                serverPort = config.serverPort,
                timestamp = System.currentTimeMillis(),
                protocol = config.protocol.name,
                duration = 0,
                totalBytes = 0,
                avgBandwidth = 0.0,
                minBandwidth = 0.0,
                maxBandwidth = 0.0,
                qualityScore = 0f,
                numStreams = config.numStreams,
                reverseMode = config.reverse,
                bidirectional = config.bidirectional,
                errorMessage = errorMessage
            )
        }

        /**
         * Creates a TestResult from a list of interval results
         */
        fun fromIntervals(
            config: TestConfiguration,
            intervals: List<IntervalResult>,
            rawJson: String = ""
        ): TestResult {
            val totalBytes = intervals.sumOf { it.bytesTransferred }
            val bandwidths = intervals.map { it.bitsPerSecond }
            val avgBandwidth = if (bandwidths.isNotEmpty()) bandwidths.average() else 0.0
            val minBandwidth = bandwidths.minOrNull() ?: 0.0
            val maxBandwidth = bandwidths.maxOrNull() ?: 0.0

            // UDP-specific metrics
            val jitters = intervals.mapNotNull { it.jitter }
            val avgJitter = if (jitters.isNotEmpty()) jitters.average() else null

            val totalPackets = intervals.mapNotNull { it.packets }.sum().takeIf { it > 0 }
            val totalLostPackets = intervals.mapNotNull { it.lostPackets }.sum()
            val packetLoss = if (totalPackets != null && totalPackets > 0) {
                (totalLostPackets.toDouble() / totalPackets) * 100
            } else null

            // TCP-specific metrics
            val totalRetransmits = intervals.mapNotNull { it.retransmits }.sum().takeIf { it >= 0 }

            // Calculate duration from intervals
            val duration = if (intervals.isNotEmpty()) {
                ((intervals.maxOf { it.endTime } - intervals.minOf { it.startTime }) * 1000).toLong()
            } else {
                config.duration
            }

            return TestResult(
                testName = config.generateTestName(),
                serverHost = config.serverHost,
                serverPort = config.serverPort,
                timestamp = System.currentTimeMillis(),
                protocol = config.protocol.name,
                duration = duration,
                totalBytes = totalBytes,
                avgBandwidth = avgBandwidth,
                minBandwidth = minBandwidth,
                maxBandwidth = maxBandwidth,
                jitter = avgJitter,
                packetLoss = packetLoss,
                totalPackets = totalPackets,
                lostPackets = totalLostPackets,
                retransmits = totalRetransmits,
                qualityScore = 0f, // Will be calculated by use case
                numStreams = config.numStreams,
                reverseMode = config.reverse,
                bidirectional = config.bidirectional,
                intervals = intervals,
                rawJson = rawJson
            )
        }
    }
}
