package com.iperf3.android.data.source.remote.iperf3protocol.message

import com.google.gson.annotations.SerializedName
import com.iperf3.android.domain.model.TestConfiguration

/**
 * Test parameters exchanged between client and server during the PARAM_EXCHANGE state.
 *
 * This class represents the JSON message sent from client to server containing
 * all the test configuration parameters.
 *
 * Example JSON:
 * ```json
 * {
 *   "tcp": true,
 *   "omit": 0,
 *   "time": 10,
 *   "num": 0,
 *   "blockcount": 0,
 *   "MSS": 0,
 *   "nodelay": false,
 *   "parallel": 1,
 *   "reverse": false,
 *   "bidirectional": false,
 *   "window": 0,
 *   "len": 131072,
 *   "bandwidth": 0,
 *   "fqrate": 0,
 *   "pacing_timer": 1000,
 *   "burst": 0,
 *   "TOS": 0,
 *   "flowlabel": 0,
 *   "title": "",
 *   "extra_data": "",
 *   "congestion": "",
 *   "congestion_used": "",
 *   "get_server_output": false,
 *   "udp_counters_64bit": true,
 *   "repeating_payload": false,
 *   "zerocopy": false,
 *   "dont_fragment": false
 * }
 * ```
 */
data class TestParams(
    @SerializedName("tcp")
    val tcp: Boolean = true,

    @SerializedName("udp")
    val udp: Boolean = false,

    @SerializedName("omit")
    val omit: Int = 0,

    @SerializedName("time")
    val time: Int = 10,

    @SerializedName("num")
    val num: Long = 0,

    @SerializedName("blockcount")
    val blockcount: Long = 0,

    @SerializedName("MSS")
    val mss: Int = 0,

    @SerializedName("nodelay")
    val nodelay: Boolean = false,

    @SerializedName("parallel")
    val parallel: Int = 1,

    @SerializedName("reverse")
    val reverse: Boolean = false,

    @SerializedName("bidirectional")
    val bidirectional: Boolean = false,

    @SerializedName("window")
    val window: Int = 0,

    @SerializedName("len")
    val len: Int = 131072,

    @SerializedName("bandwidth")
    val bandwidth: Long = 0,

    @SerializedName("fqrate")
    val fqrate: Long = 0,

    @SerializedName("pacing_timer")
    val pacingTimer: Int = 1000,

    @SerializedName("burst")
    val burst: Int = 0,

    @SerializedName("TOS")
    val tos: Int = 0,

    @SerializedName("flowlabel")
    val flowlabel: Int = 0,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("extra_data")
    val extraData: String = "",

    @SerializedName("congestion")
    val congestion: String = "",

    @SerializedName("congestion_used")
    val congestionUsed: String = "",

    @SerializedName("get_server_output")
    val getServerOutput: Boolean = false,

    @SerializedName("udp_counters_64bit")
    val udpCounters64bit: Boolean = true,

    @SerializedName("repeating_payload")
    val repeatingPayload: Boolean = false,

    @SerializedName("zerocopy")
    val zerocopy: Boolean = false,

    @SerializedName("dont_fragment")
    val dontFragment: Boolean = false,

    @SerializedName("client_version")
    val clientVersion: String = IPerf3Constants.IPERF3_VERSION
) {
    /**
     * Returns the protocol string
     */
    val protocol: String
        get() = if (udp) "UDP" else "TCP"

    /**
     * Duration in milliseconds
     */
    val durationMs: Long
        get() = time * 1000L

    companion object {
        /**
         * Creates TestParams from a TestConfiguration
         */
        fun fromConfiguration(config: TestConfiguration): TestParams {
            val isUdp = config.protocol == TestConfiguration.Protocol.UDP

            return TestParams(
                tcp = !isUdp,
                udp = isUdp,
                time = (config.duration / 1000).toInt(),
                num = config.bytesToTransfer ?: 0,
                mss = config.mss ?: 0,
                nodelay = config.noDelay,
                parallel = config.numStreams,
                reverse = config.reverse,
                bidirectional = config.bidirectional,
                window = config.windowSize ?: 0,
                len = config.bufferLength,
                bandwidth = config.bandwidthLimit ?: 0,
                title = config.testName
            )
        }

        /**
         * Creates a default TCP test configuration
         */
        fun defaultTcp(
            duration: Int = 10,
            parallel: Int = 1
        ): TestParams {
            return TestParams(
                tcp = true,
                udp = false,
                time = duration,
                parallel = parallel
            )
        }

        /**
         * Creates a default UDP test configuration
         */
        fun defaultUdp(
            duration: Int = 10,
            bandwidth: Long = 1_000_000_000, // 1 Gbps
            parallel: Int = 1
        ): TestParams {
            return TestParams(
                tcp = false,
                udp = true,
                time = duration,
                parallel = parallel,
                bandwidth = bandwidth,
                len = 8192 // 8KB for UDP
            )
        }
    }
}

/**
 * Response from server after receiving test parameters
 */
data class TestParamsResponse(
    @SerializedName("state")
    val state: Int,

    @SerializedName("server_cookie")
    val serverCookie: String? = null,

    @SerializedName("version")
    val version: String? = null,

    @SerializedName("error")
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = state != IPerf3Constants.State.ACCESS_DENIED &&
                state != IPerf3Constants.State.SERVER_ERROR &&
                error == null
}
