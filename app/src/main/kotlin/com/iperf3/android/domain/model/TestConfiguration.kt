package com.iperf3.android.domain.model

/**
 * Configuration for an iperf3 network test.
 *
 * This data class encapsulates all parameters needed to run a bandwidth test,
 * supporting both TCP and UDP protocols with various iperf3 flags.
 *
 * @property serverHost The hostname or IP address of the iperf3 server
 * @property serverPort The port number the server is listening on (default: 5201)
 * @property protocol The transport protocol to use: "TCP" or "UDP"
 * @property duration Test duration in milliseconds
 * @property numStreams Number of parallel streams (-P flag)
 * @property bandwidthLimit Target bandwidth limit in bits per second (-b flag), null for unlimited
 * @property bytesToTransfer Number of bytes to transfer instead of duration (-n flag), null for duration-based
 * @property reverse Reverse mode: server sends, client receives (-R flag)
 * @property bidirectional Bidirectional test: simultaneous send and receive (--bidir flag)
 * @property reportingInterval Interval for progress reports in milliseconds
 * @property bufferLength Length of buffer to read/write in bytes
 * @property windowSize TCP window size in bytes, null for system default
 * @property mss Maximum segment size (TCP MSS), null for system default
 * @property noDelay Disable Nagle's algorithm (TCP_NODELAY)
 * @property timeout Connection timeout in milliseconds
 * @property testName Optional user-defined name for this test
 */
data class TestConfiguration(
    val serverHost: String,
    val serverPort: Int = DEFAULT_PORT,
    val protocol: Protocol = Protocol.TCP,
    val duration: Long = DEFAULT_DURATION_MS,
    val numStreams: Int = 1,
    val bandwidthLimit: Long? = null,
    val bytesToTransfer: Long? = null,
    val reverse: Boolean = false,
    val bidirectional: Boolean = false,
    val reportingInterval: Long = DEFAULT_INTERVAL_MS,
    val bufferLength: Int = DEFAULT_BUFFER_LENGTH,
    val windowSize: Int? = null,
    val mss: Int? = null,
    val noDelay: Boolean = false,
    val timeout: Int = DEFAULT_TIMEOUT_MS,
    val testName: String = ""
) {
    /**
     * Transport protocol for the test
     */
    enum class Protocol {
        TCP,
        UDP;

        companion object {
            fun fromString(value: String): Protocol {
                return when (value.uppercase()) {
                    "TCP" -> TCP
                    "UDP" -> UDP
                    else -> TCP
                }
            }
        }
    }

    /**
     * Validates the configuration and returns a list of validation errors.
     * Returns an empty list if the configuration is valid.
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (serverHost.isBlank()) {
            errors.add("Server host is required")
        }

        if (serverPort !in 1..65535) {
            errors.add("Port must be between 1 and 65535")
        }

        if (duration <= 0 && bytesToTransfer == null) {
            errors.add("Duration must be positive or bytes to transfer must be specified")
        }

        if (numStreams < 1 || numStreams > MAX_PARALLEL_STREAMS) {
            errors.add("Number of streams must be between 1 and $MAX_PARALLEL_STREAMS")
        }

        bandwidthLimit?.let {
            if (it <= 0) {
                errors.add("Bandwidth limit must be positive")
            }
        }

        bytesToTransfer?.let {
            if (it <= 0) {
                errors.add("Bytes to transfer must be positive")
            }
        }

        if (bufferLength <= 0) {
            errors.add("Buffer length must be positive")
        }

        return errors
    }

    /**
     * Returns true if this configuration is valid
     */
    fun isValid(): Boolean = validate().isEmpty()

    /**
     * Generates a descriptive name for this test configuration
     */
    fun generateTestName(): String {
        if (testName.isNotBlank()) return testName

        val mode = when {
            bidirectional -> "bidir"
            reverse -> "reverse"
            else -> "normal"
        }

        val durationStr = if (bytesToTransfer != null) {
            "${bytesToTransfer / (1024 * 1024)}MB"
        } else {
            "${duration / 1000}s"
        }

        return "${protocol.name} ${mode} ${numStreams}x $durationStr"
    }

    companion object {
        const val DEFAULT_PORT = 5201
        const val DEFAULT_DURATION_MS = 10_000L // 10 seconds
        const val DEFAULT_INTERVAL_MS = 1_000L // 1 second
        const val DEFAULT_BUFFER_LENGTH = 128 * 1024 // 128 KB
        const val DEFAULT_TIMEOUT_MS = 30_000 // 30 seconds
        const val MAX_PARALLEL_STREAMS = 128

        /**
         * Creates a default configuration for quick testing
         */
        fun default(serverHost: String = "localhost"): TestConfiguration {
            return TestConfiguration(serverHost = serverHost)
        }

        /**
         * Creates a configuration for download test (reverse mode)
         */
        fun download(
            serverHost: String,
            duration: Long = DEFAULT_DURATION_MS,
            numStreams: Int = 1
        ): TestConfiguration {
            return TestConfiguration(
                serverHost = serverHost,
                duration = duration,
                numStreams = numStreams,
                reverse = true
            )
        }

        /**
         * Creates a configuration for upload test (normal mode)
         */
        fun upload(
            serverHost: String,
            duration: Long = DEFAULT_DURATION_MS,
            numStreams: Int = 1
        ): TestConfiguration {
            return TestConfiguration(
                serverHost = serverHost,
                duration = duration,
                numStreams = numStreams,
                reverse = false
            )
        }

        /**
         * Creates a configuration for bidirectional test
         */
        fun bidirectional(
            serverHost: String,
            duration: Long = DEFAULT_DURATION_MS,
            numStreams: Int = 1
        ): TestConfiguration {
            return TestConfiguration(
                serverHost = serverHost,
                duration = duration,
                numStreams = numStreams,
                bidirectional = true
            )
        }

        /**
         * Creates a UDP test configuration
         */
        fun udp(
            serverHost: String,
            bandwidthLimit: Long = 1_000_000_000L, // 1 Gbps default for UDP
            duration: Long = DEFAULT_DURATION_MS
        ): TestConfiguration {
            return TestConfiguration(
                serverHost = serverHost,
                protocol = Protocol.UDP,
                duration = duration,
                bandwidthLimit = bandwidthLimit
            )
        }
    }
}
