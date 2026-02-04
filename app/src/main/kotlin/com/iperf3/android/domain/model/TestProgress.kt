package com.iperf3.android.domain.model

/**
 * Sealed class representing the progress and state of an iperf3 test.
 *
 * This is used as a Flow emission type to communicate test progress
 * from the data layer to the UI layer.
 */
sealed class TestProgress {

    /**
     * Test is idle/not started
     */
    data object Idle : TestProgress()

    /**
     * Test is connecting to the server
     *
     * @property serverHost The server being connected to
     * @property serverPort The port being connected to
     */
    data class Connecting(
        val serverHost: String,
        val serverPort: Int
    ) : TestProgress()

    /**
     * Connection established, test is being configured
     *
     * @property serverVersion The iperf3 server version
     * @property cookie The session cookie for this test
     */
    data class Connected(
        val serverVersion: String,
        val cookie: String
    ) : TestProgress()

    /**
     * Test has started and data transfer is in progress
     *
     * @property config The test configuration being executed
     * @property startTime Unix timestamp when the test started
     */
    data class Started(
        val config: TestConfiguration,
        val startTime: Long = System.currentTimeMillis()
    ) : TestProgress()

    /**
     * An interval result has been received
     *
     * @property interval The interval result data
     * @property elapsedTime Time elapsed since test start in milliseconds
     * @property estimatedProgress Estimated progress as a percentage (0.0 to 1.0)
     */
    data class Interval(
        val interval: IntervalResult,
        val elapsedTime: Long,
        val estimatedProgress: Float
    ) : TestProgress() {
        /**
         * Progress as an integer percentage (0-100)
         */
        val progressPercent: Int
            get() = (estimatedProgress * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Test has completed successfully
     *
     * @property result The complete test results
     */
    data class Complete(
        val result: TestResult
    ) : TestProgress()

    /**
     * Test failed with an error
     *
     * @property message Error description
     * @property cause The underlying exception, if any
     * @property partialResult Partial results if any data was collected before failure
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val partialResult: TestResult? = null
    ) : TestProgress()

    /**
     * Test was cancelled by user
     *
     * @property partialResult Partial results if any data was collected before cancellation
     */
    data class Cancelled(
        val partialResult: TestResult? = null
    ) : TestProgress()

    /**
     * Returns true if the test is in a terminal state (complete, error, or cancelled)
     */
    val isTerminal: Boolean
        get() = this is Complete || this is Error || this is Cancelled

    /**
     * Returns true if the test is actively running
     */
    val isRunning: Boolean
        get() = this is Connecting || this is Connected || this is Started || this is Interval
}

/**
 * Sealed class representing server-side progress events
 */
sealed class ServerProgress {

    /**
     * Server is starting up
     */
    data class Starting(
        val port: Int
    ) : ServerProgress()

    /**
     * Server is ready and listening for connections
     */
    data class Ready(
        val status: ServerStatus
    ) : ServerProgress()

    /**
     * A new client has connected
     */
    data class ClientConnected(
        val clientAddress: String,
        val clientPort: Int,
        val connectionId: Int
    ) : ServerProgress()

    /**
     * A test is in progress with a client
     */
    data class TestRunning(
        val connectionId: Int,
        val clientAddress: String,
        val interval: IntervalResult
    ) : ServerProgress()

    /**
     * A test has completed
     */
    data class TestComplete(
        val connectionId: Int,
        val clientAddress: String,
        val result: TestResult
    ) : ServerProgress()

    /**
     * A client has disconnected
     */
    data class ClientDisconnected(
        val connectionId: Int,
        val clientAddress: String
    ) : ServerProgress()

    /**
     * Server encountered an error
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ServerProgress()

    /**
     * Server has stopped
     */
    data object Stopped : ServerProgress()
}
