package com.iperf3.android.domain.repository

import com.iperf3.android.domain.model.ServerProgress
import com.iperf3.android.domain.model.ServerStatus
import com.iperf3.android.domain.model.TestConfiguration
import com.iperf3.android.domain.model.TestProgress
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for iperf3 operations.
 *
 * This interface defines the contract for running iperf3 tests
 * and managing the iperf3 server. Implementations handle all
 * network communication and protocol details.
 */
interface IPerf3Repository {

    /**
     * Runs an iperf3 client test against a remote server.
     *
     * This method starts a test with the provided configuration and emits
     * progress updates as a Flow. The flow will emit:
     * - Connecting: When establishing connection
     * - Connected: When connection is established
     * - Started: When the test begins
     * - Interval: For each reporting interval
     * - Complete: When the test finishes successfully
     * - Error: If an error occurs
     * - Cancelled: If the test is cancelled
     *
     * @param config The test configuration
     * @return A Flow of TestProgress updates
     */
    fun runClientTest(config: TestConfiguration): Flow<TestProgress>

    /**
     * Cancels any running client test.
     */
    suspend fun cancelClientTest()

    /**
     * Checks if a client test is currently running.
     *
     * @return True if a test is in progress
     */
    fun isClientTestRunning(): Boolean

    /**
     * Starts the iperf3 server on the specified port.
     *
     * This method starts a server that accepts connections from iperf3 clients.
     * It emits progress updates as a Flow including connection events and
     * test progress for each client.
     *
     * @param port The port to listen on (default: 5201)
     * @param bindAddress The address to bind to (default: "0.0.0.0")
     * @return A Flow of ServerProgress updates
     */
    fun startServer(
        port: Int = 5201,
        bindAddress: String = "0.0.0.0"
    ): Flow<ServerProgress>

    /**
     * Stops the running iperf3 server.
     */
    suspend fun stopServer()

    /**
     * Gets the current server status.
     *
     * @return A Flow of ServerStatus updates
     */
    fun getServerStatus(): Flow<ServerStatus>

    /**
     * Checks if the server is currently running.
     *
     * @return True if the server is running
     */
    fun isServerRunning(): Boolean
}
