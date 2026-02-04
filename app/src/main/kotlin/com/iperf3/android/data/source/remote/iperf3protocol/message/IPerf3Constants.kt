package com.iperf3.android.data.source.remote.iperf3protocol.message

/**
 * Constants and state definitions for the iperf3 protocol.
 *
 * The iperf3 protocol uses a control connection (TCP) for negotiation
 * and one or more data connections for actual bandwidth testing.
 */
object IPerf3Constants {

    // Default values
    const val DEFAULT_PORT = 5201
    const val DEFAULT_BUFFER_SIZE = 128 * 1024  // 128 KB
    const val DEFAULT_TCP_WINDOW_SIZE = 0  // Use system default
    const val DEFAULT_UDP_BUFFER_SIZE = 8 * 1024  // 8 KB for UDP
    const val DEFAULT_MSS = 0  // Use system default
    const val COOKIE_SIZE = 37  // iperf3 cookie is 36 characters + null terminator

    // Protocol version
    const val IPERF3_VERSION = "3.14"  // We emulate iperf3 3.14

    /**
     * iperf3 protocol states
     *
     * The protocol follows this state machine:
     * PARAM_EXCHANGE -> CREATE_STREAMS -> TEST_START -> TEST_RUNNING ->
     * EXCHANGE_RESULTS -> DISPLAY_RESULTS -> IPERF_DONE
     */
    object State {
        const val TEST_START = 1
        const val TEST_RUNNING = 2
        const val RESULT_REQUEST = 3      // Deprecated in newer iperf3
        const val TEST_END = 4
        const val STREAM_BEGIN = 5        // Deprecated
        const val STREAM_RUNNING = 6      // Deprecated
        const val STREAM_END = 7          // Deprecated
        const val ALL_STREAMS_END = 8     // Deprecated
        const val PARAM_EXCHANGE = 9
        const val CREATE_STREAMS = 10
        const val SERVER_TERMINATE = 11
        const val CLIENT_TERMINATE = 12
        const val EXCHANGE_RESULTS = 13
        const val DISPLAY_RESULTS = 14
        const val IPERF_START = 15
        const val IPERF_DONE = 16
        const val ACCESS_DENIED = -1
        const val SERVER_ERROR = -2

        fun nameOf(state: Int): String = when (state) {
            TEST_START -> "TEST_START"
            TEST_RUNNING -> "TEST_RUNNING"
            RESULT_REQUEST -> "RESULT_REQUEST"
            TEST_END -> "TEST_END"
            STREAM_BEGIN -> "STREAM_BEGIN"
            STREAM_RUNNING -> "STREAM_RUNNING"
            STREAM_END -> "STREAM_END"
            ALL_STREAMS_END -> "ALL_STREAMS_END"
            PARAM_EXCHANGE -> "PARAM_EXCHANGE"
            CREATE_STREAMS -> "CREATE_STREAMS"
            SERVER_TERMINATE -> "SERVER_TERMINATE"
            CLIENT_TERMINATE -> "CLIENT_TERMINATE"
            EXCHANGE_RESULTS -> "EXCHANGE_RESULTS"
            DISPLAY_RESULTS -> "DISPLAY_RESULTS"
            IPERF_START -> "IPERF_START"
            IPERF_DONE -> "IPERF_DONE"
            ACCESS_DENIED -> "ACCESS_DENIED"
            SERVER_ERROR -> "SERVER_ERROR"
            else -> "UNKNOWN($state)"
        }
    }

    /**
     * Test role: sender or receiver
     */
    object Role {
        const val SENDER = 's'.code
        const val RECEIVER = 'r'.code
    }

    /**
     * Protocol identifiers
     */
    object Protocol {
        const val TCP = "tcp"
        const val UDP = "udp"
        const val SCTP = "sctp"  // Not commonly used

        fun fromInt(value: Int): String = when (value) {
            6 -> TCP
            17 -> UDP
            132 -> SCTP
            else -> TCP
        }

        fun toInt(protocol: String): Int = when (protocol.lowercase()) {
            TCP -> 6
            UDP -> 17
            SCTP -> 132
            else -> 6
        }
    }

    /**
     * UDP datagram header format
     *
     * UDP packets have a 12-byte header:
     * - 4 bytes: seconds (big-endian uint32)
     * - 4 bytes: microseconds (big-endian uint32)
     * - 4 bytes: packet count (big-endian uint32)
     */
    const val UDP_HEADER_SIZE = 12

    /**
     * Minimum control message size
     */
    const val MIN_CONTROL_MESSAGE_SIZE = 4

    /**
     * Maximum control message size (1MB limit for safety)
     */
    const val MAX_CONTROL_MESSAGE_SIZE = 1024 * 1024

    /**
     * Control message timeout in milliseconds
     */
    const val CONTROL_TIMEOUT_MS = 30_000L

    /**
     * Data transfer timeout in milliseconds
     */
    const val DATA_TIMEOUT_MS = 120_000L
}
