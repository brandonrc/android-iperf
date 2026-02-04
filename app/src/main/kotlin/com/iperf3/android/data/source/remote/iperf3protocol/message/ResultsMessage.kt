package com.iperf3.android.data.source.remote.iperf3protocol.message

import com.google.gson.annotations.SerializedName

/**
 * Complete iperf3 JSON results format.
 *
 * This class represents the full JSON output produced by iperf3,
 * including start information, intervals, and end summary.
 *
 * Example structure:
 * ```json
 * {
 *   "start": { ... },
 *   "intervals": [ ... ],
 *   "end": { ... }
 * }
 * ```
 */
data class IPerf3Results(
    @SerializedName("start")
    val start: StartInfo,

    @SerializedName("intervals")
    val intervals: List<IntervalInfo>,

    @SerializedName("end")
    val end: EndInfo,

    @SerializedName("error")
    val error: String? = null
)

/**
 * Start information from the iperf3 JSON output
 */
data class StartInfo(
    @SerializedName("connected")
    val connected: List<ConnectionInfo>,

    @SerializedName("version")
    val version: String,

    @SerializedName("system_info")
    val systemInfo: String,

    @SerializedName("timestamp")
    val timestamp: TimestampInfo,

    @SerializedName("connecting_to")
    val connectingTo: ConnectingTo? = null,

    @SerializedName("cookie")
    val cookie: String,

    @SerializedName("tcp_mss_default")
    val tcpMssDefault: Int? = null,

    @SerializedName("sock_bufsize")
    val sockBufsize: Int? = null,

    @SerializedName("sndbuf_actual")
    val sndbufActual: Int? = null,

    @SerializedName("rcvbuf_actual")
    val rcvbufActual: Int? = null,

    @SerializedName("test_start")
    val testStart: TestStartInfo
)

/**
 * Connection information for a single stream
 */
data class ConnectionInfo(
    @SerializedName("socket")
    val socket: Int,

    @SerializedName("local_host")
    val localHost: String,

    @SerializedName("local_port")
    val localPort: Int,

    @SerializedName("remote_host")
    val remoteHost: String,

    @SerializedName("remote_port")
    val remotePort: Int
)

/**
 * Timestamp information
 */
data class TimestampInfo(
    @SerializedName("time")
    val time: String,

    @SerializedName("timesecs")
    val timesecs: Long
)

/**
 * Server connection target
 */
data class ConnectingTo(
    @SerializedName("host")
    val host: String,

    @SerializedName("port")
    val port: Int
)

/**
 * Test start parameters as reported by iperf3
 */
data class TestStartInfo(
    @SerializedName("protocol")
    val protocol: String,

    @SerializedName("num_streams")
    val numStreams: Int,

    @SerializedName("blksize")
    val blksize: Int,

    @SerializedName("omit")
    val omit: Int,

    @SerializedName("duration")
    val duration: Int,

    @SerializedName("bytes")
    val bytes: Long,

    @SerializedName("blocks")
    val blocks: Long,

    @SerializedName("reverse")
    val reverse: Int,

    @SerializedName("tos")
    val tos: Int
)

/**
 * A single interval's results
 */
data class IntervalInfo(
    @SerializedName("streams")
    val streams: List<StreamIntervalInfo>,

    @SerializedName("sum")
    val sum: SumIntervalInfo
)

/**
 * Per-stream interval data
 */
data class StreamIntervalInfo(
    @SerializedName("socket")
    val socket: Int,

    @SerializedName("start")
    val start: Double,

    @SerializedName("end")
    val end: Double,

    @SerializedName("seconds")
    val seconds: Double,

    @SerializedName("bytes")
    val bytes: Long,

    @SerializedName("bits_per_second")
    val bitsPerSecond: Double,

    @SerializedName("retransmits")
    val retransmits: Int? = null,

    @SerializedName("snd_cwnd")
    val sndCwnd: Long? = null,

    @SerializedName("rtt")
    val rtt: Long? = null,

    @SerializedName("rttvar")
    val rttvar: Long? = null,

    @SerializedName("pmtu")
    val pmtu: Int? = null,

    // UDP-specific fields
    @SerializedName("packets")
    val packets: Long? = null,

    @SerializedName("jitter_ms")
    val jitterMs: Double? = null,

    @SerializedName("lost_packets")
    val lostPackets: Long? = null,

    @SerializedName("lost_percent")
    val lostPercent: Double? = null,

    @SerializedName("out_of_order")
    val outOfOrder: Long? = null,

    @SerializedName("omitted")
    val omitted: Boolean? = null
)

/**
 * Summed interval data across all streams
 */
data class SumIntervalInfo(
    @SerializedName("start")
    val start: Double,

    @SerializedName("end")
    val end: Double,

    @SerializedName("seconds")
    val seconds: Double,

    @SerializedName("bytes")
    val bytes: Long,

    @SerializedName("bits_per_second")
    val bitsPerSecond: Double,

    @SerializedName("retransmits")
    val retransmits: Int? = null,

    // UDP-specific
    @SerializedName("packets")
    val packets: Long? = null,

    @SerializedName("jitter_ms")
    val jitterMs: Double? = null,

    @SerializedName("lost_packets")
    val lostPackets: Long? = null,

    @SerializedName("lost_percent")
    val lostPercent: Double? = null,

    @SerializedName("omitted")
    val omitted: Boolean? = null
)

/**
 * End/summary information
 */
data class EndInfo(
    @SerializedName("streams")
    val streams: List<StreamEndInfo>,

    @SerializedName("sum_sent")
    val sumSent: SumEndInfo,

    @SerializedName("sum_received")
    val sumReceived: SumEndInfo,

    @SerializedName("cpu_utilization_percent")
    val cpuUtilizationPercent: CpuUtilization? = null,

    // UDP-specific
    @SerializedName("sum")
    val sum: SumEndInfo? = null
)

/**
 * Per-stream end summary
 */
data class StreamEndInfo(
    @SerializedName("sender")
    val sender: StreamSummary,

    @SerializedName("receiver")
    val receiver: StreamSummary
)

/**
 * Stream summary data
 */
data class StreamSummary(
    @SerializedName("socket")
    val socket: Int,

    @SerializedName("start")
    val start: Double,

    @SerializedName("end")
    val end: Double,

    @SerializedName("seconds")
    val seconds: Double,

    @SerializedName("bytes")
    val bytes: Long,

    @SerializedName("bits_per_second")
    val bitsPerSecond: Double,

    @SerializedName("retransmits")
    val retransmits: Int? = null,

    @SerializedName("max_snd_cwnd")
    val maxSndCwnd: Long? = null,

    @SerializedName("max_rtt")
    val maxRtt: Long? = null,

    @SerializedName("min_rtt")
    val minRtt: Long? = null,

    @SerializedName("mean_rtt")
    val meanRtt: Long? = null,

    // UDP-specific
    @SerializedName("packets")
    val packets: Long? = null,

    @SerializedName("jitter_ms")
    val jitterMs: Double? = null,

    @SerializedName("lost_packets")
    val lostPackets: Long? = null,

    @SerializedName("lost_percent")
    val lostPercent: Double? = null,

    @SerializedName("out_of_order")
    val outOfOrder: Long? = null
)

/**
 * Summed end data
 */
data class SumEndInfo(
    @SerializedName("start")
    val start: Double,

    @SerializedName("end")
    val end: Double,

    @SerializedName("seconds")
    val seconds: Double,

    @SerializedName("bytes")
    val bytes: Long,

    @SerializedName("bits_per_second")
    val bitsPerSecond: Double,

    @SerializedName("retransmits")
    val retransmits: Int? = null,

    // UDP-specific
    @SerializedName("packets")
    val packets: Long? = null,

    @SerializedName("jitter_ms")
    val jitterMs: Double? = null,

    @SerializedName("lost_packets")
    val lostPackets: Long? = null,

    @SerializedName("lost_percent")
    val lostPercent: Double? = null
)

/**
 * CPU utilization data
 */
data class CpuUtilization(
    @SerializedName("host_total")
    val hostTotal: Double,

    @SerializedName("host_user")
    val hostUser: Double,

    @SerializedName("host_system")
    val hostSystem: Double,

    @SerializedName("remote_total")
    val remoteTotal: Double,

    @SerializedName("remote_user")
    val remoteUser: Double,

    @SerializedName("remote_system")
    val remoteSystem: Double
)
