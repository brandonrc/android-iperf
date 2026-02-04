package com.iperf3.android.data.repository

import com.iperf3.android.data.source.remote.iperf3protocol.BandwidthLimiter
import com.iperf3.android.data.source.remote.iperf3protocol.ControlMessageHandler
import com.iperf3.android.data.source.remote.iperf3protocol.message.IPerf3Constants
import com.iperf3.android.data.source.remote.iperf3protocol.message.TestParams
import com.iperf3.android.data.source.remote.socket.SocketManagerImpl
import com.iperf3.android.data.source.remote.socket.TCPServerSocket
import com.iperf3.android.data.source.remote.socket.TCPSocket
import com.iperf3.android.di.IoDispatcher
import com.iperf3.android.domain.model.IntervalResult
import com.iperf3.android.domain.model.ServerProgress
import com.iperf3.android.domain.model.ServerStatus
import com.iperf3.android.domain.model.TestConfiguration
import com.iperf3.android.domain.model.TestProgress
import com.iperf3.android.domain.model.TestResult
import com.iperf3.android.domain.repository.IPerf3Repository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IPerf3Repository] that handles the iperf3 network protocol.
 *
 * Manages client test execution, server operation, and the iperf3 protocol
 * state machine for bandwidth measurement.
 */
@Singleton
class IPerf3RepositoryImpl @Inject constructor(
    private val controlMessageHandler: ControlMessageHandler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IPerf3Repository {

    private val socketManager = SocketManagerImpl()

    private val clientRunning = AtomicBoolean(false)
    private val clientCancelled = AtomicBoolean(false)
    private val serverRunning = AtomicBoolean(false)

    private var controlSocket: TCPSocket? = null
    private var dataSocket: TCPSocket? = null
    private var serverSocket: TCPServerSocket? = null

    private val serverStatusFlow = MutableStateFlow(ServerStatus.STOPPED)

    override fun runClientTest(config: TestConfiguration): Flow<TestProgress> = callbackFlow {
        if (clientRunning.getAndSet(true)) {
            trySend(TestProgress.Error("A test is already running"))
            close()
            return@callbackFlow
        }

        clientCancelled.set(false)
        var controlConn: TCPSocket? = null
        var dataConn: TCPSocket? = null
        val intervals = mutableListOf<IntervalResult>()
        val testStartTime = System.currentTimeMillis()

        try {
            // Step 1: Connect to server
            trySend(TestProgress.Connecting(config.serverHost, config.serverPort))

            controlConn = withContext(ioDispatcher) {
                socketManager.createTcpSocket(
                    config.serverHost,
                    config.serverPort,
                    config.timeout
                )
            }
            controlSocket = controlConn

            if (clientCancelled.get()) {
                trySend(TestProgress.Cancelled())
                close()
                return@callbackFlow
            }

            // Step 2: Write cookie
            val cookie = controlMessageHandler.generateCookie()
            withContext(ioDispatcher) {
                controlMessageHandler.writeCookie(controlConn.sink, cookie)
            }

            trySend(TestProgress.Connected(IPerf3Constants.IPERF3_VERSION, cookie))

            // Step 3: Read state - expect PARAM_EXCHANGE
            val paramState = withContext(ioDispatcher) {
                controlMessageHandler.readState(controlConn.source)
            }
            if (paramState != IPerf3Constants.State.PARAM_EXCHANGE) {
                handleUnexpectedState(paramState, config)?.let { trySend(it) }
                close()
                return@callbackFlow
            }

            // Step 4: Write test params
            val testParams = TestParams.fromConfiguration(config)
            withContext(ioDispatcher) {
                controlMessageHandler.writeTestParams(controlConn.sink, testParams)
            }

            // Step 5: Read state - expect CREATE_STREAMS
            val createState = withContext(ioDispatcher) {
                controlMessageHandler.readState(controlConn.source)
            }
            if (createState != IPerf3Constants.State.CREATE_STREAMS) {
                handleUnexpectedState(createState, config)?.let { trySend(it) }
                close()
                return@callbackFlow
            }

            // Create data stream(s)
            dataConn = withContext(ioDispatcher) {
                socketManager.createTcpSocket(
                    config.serverHost,
                    config.serverPort,
                    config.timeout
                )
            }
            dataSocket = dataConn

            // Send cookie on data connection to associate it
            withContext(ioDispatcher) {
                controlMessageHandler.writeCookie(dataConn.sink, cookie)
            }

            if (config.noDelay) {
                dataConn.setTcpNoDelay(true)
            }
            config.windowSize?.let { windowSize ->
                if (windowSize > 0) {
                    dataConn.setSendBufferSize(windowSize)
                    dataConn.setReceiveBufferSize(windowSize)
                }
            }

            // Step 6: Read state - expect TEST_START
            val startState = withContext(ioDispatcher) {
                controlMessageHandler.readState(controlConn.source)
            }
            if (startState != IPerf3Constants.State.TEST_START) {
                handleUnexpectedState(startState, config)?.let { trySend(it) }
                close()
                return@callbackFlow
            }

            trySend(TestProgress.Started(config, testStartTime))

            // Step 7: Read state - expect TEST_RUNNING
            val runState = withContext(ioDispatcher) {
                controlMessageHandler.readState(controlConn.source)
            }
            if (runState != IPerf3Constants.State.TEST_RUNNING) {
                handleUnexpectedState(runState, config)?.let { trySend(it) }
                close()
                return@callbackFlow
            }

            // Data transfer phase
            val bandwidthLimiter = if (config.bandwidthLimit != null && config.bandwidthLimit > 0) {
                BandwidthLimiter(config.bandwidthLimit)
            } else {
                BandwidthLimiter.unlimited()
            }

            val buffer = ByteArray(config.bufferLength)
            val durationMs = config.duration
            var intervalStartTime = 0.0
            var intervalBytes = 0L
            val intervalDurationSec = config.reportingInterval / 1000.0
            var intervalIndex = 0

            if (config.reverse) {
                // Receive mode: read data from server
                dataConn.setSoTimeout(((durationMs + 5000).toInt()))
                val receiveStartTime = System.nanoTime()

                while (!clientCancelled.get()) {
                    val elapsed = (System.nanoTime() - receiveStartTime) / 1_000_000_000.0

                    if (elapsed >= durationMs / 1000.0) break

                    try {
                        val bytesRead = withContext(ioDispatcher) {
                            dataConn.source.read(buffer, 0, buffer.size)
                        }
                        if (bytesRead <= 0) break
                        intervalBytes += bytesRead

                        val currentIntervalEnd = (intervalIndex + 1) * intervalDurationSec
                        if (elapsed >= currentIntervalEnd) {
                            val intervalResult = IntervalResult(
                                streamId = 0,
                                startTime = intervalStartTime,
                                endTime = elapsed,
                                bytesTransferred = intervalBytes,
                                bitsPerSecond = (intervalBytes * 8.0) / (elapsed - intervalStartTime)
                            )
                            intervals.add(intervalResult)

                            val progress = (elapsed / (durationMs / 1000.0)).toFloat().coerceIn(0f, 1f)
                            trySend(
                                TestProgress.Interval(
                                    interval = intervalResult,
                                    elapsedTime = (elapsed * 1000).toLong(),
                                    estimatedProgress = progress
                                )
                            )

                            intervalStartTime = elapsed
                            intervalBytes = 0
                            intervalIndex++
                        }
                    } catch (e: IOException) {
                        break
                    }
                }
            } else {
                // Send mode: write data to server
                val sendStartTime = System.nanoTime()

                while (!clientCancelled.get()) {
                    val elapsed = (System.nanoTime() - sendStartTime) / 1_000_000_000.0

                    if (elapsed >= durationMs / 1000.0) break

                    try {
                        bandwidthLimiter.acquireTokens(buffer.size)

                        withContext(ioDispatcher) {
                            dataConn.sink.write(buffer)
                            dataConn.sink.flush()
                        }
                        intervalBytes += buffer.size

                        val currentIntervalEnd = (intervalIndex + 1) * intervalDurationSec
                        if (elapsed >= currentIntervalEnd) {
                            val intervalResult = IntervalResult(
                                streamId = 0,
                                startTime = intervalStartTime,
                                endTime = elapsed,
                                bytesTransferred = intervalBytes,
                                bitsPerSecond = (intervalBytes * 8.0) / (elapsed - intervalStartTime)
                            )
                            intervals.add(intervalResult)

                            val progress = (elapsed / (durationMs / 1000.0)).toFloat().coerceIn(0f, 1f)
                            trySend(
                                TestProgress.Interval(
                                    interval = intervalResult,
                                    elapsedTime = (elapsed * 1000).toLong(),
                                    estimatedProgress = progress
                                )
                            )

                            intervalStartTime = elapsed
                            intervalBytes = 0
                            intervalIndex++
                        }
                    } catch (e: IOException) {
                        break
                    }
                }
            }

            if (clientCancelled.get()) {
                val partialResult = TestResult.fromIntervals(config, intervals)
                trySend(TestProgress.Cancelled(partialResult))
                close()
                return@callbackFlow
            }

            // Step 8: Send TEST_END on control connection
            withContext(ioDispatcher) {
                controlMessageHandler.writeState(
                    controlConn.sink,
                    IPerf3Constants.State.TEST_END
                )
            }

            // Step 9: Read state - expect EXCHANGE_RESULTS
            val exchangeState = withContext(ioDispatcher) {
                controlMessageHandler.readState(controlConn.source)
            }
            if (exchangeState == IPerf3Constants.State.EXCHANGE_RESULTS) {
                // Read server results
                withContext(ioDispatcher) {
                    try {
                        controlMessageHandler.readJsonMessage(controlConn.source)
                    } catch (_: Exception) {
                        "{}"
                    }
                }

                // Write client results (empty for now)
                withContext(ioDispatcher) {
                    controlMessageHandler.writeJsonMessage(controlConn.sink, "{}")
                }
            }

            // Step 10: Read state - expect DISPLAY_RESULTS
            withContext(ioDispatcher) {
                try {
                    controlMessageHandler.readState(controlConn.source)
                } catch (_: Exception) {
                    IPerf3Constants.State.DISPLAY_RESULTS
                }
            }

            // Step 11: Read state - expect IPERF_DONE
            withContext(ioDispatcher) {
                try {
                    controlMessageHandler.readState(controlConn.source)
                } catch (_: Exception) {
                    IPerf3Constants.State.IPERF_DONE
                }
            }

            // Build final result
            val testResult = TestResult.fromIntervals(config, intervals)
            trySend(TestProgress.Complete(testResult))

        } catch (e: Exception) {
            if (clientCancelled.get()) {
                val partialResult = if (intervals.isNotEmpty()) {
                    TestResult.fromIntervals(config, intervals)
                } else null
                trySend(TestProgress.Cancelled(partialResult))
            } else {
                val partialResult = if (intervals.isNotEmpty()) {
                    TestResult.fromIntervals(config, intervals)
                } else null
                trySend(
                    TestProgress.Error(
                        message = e.message ?: "Unknown error",
                        cause = e,
                        partialResult = partialResult
                    )
                )
            }
        } finally {
            controlConn?.close()
            dataConn?.close()
            controlSocket = null
            dataSocket = null
            clientRunning.set(false)
        }

        close()

        awaitClose {
            controlSocket?.close()
            dataSocket?.close()
            clientRunning.set(false)
        }
    }.flowOn(ioDispatcher)

    override fun startServer(port: Int, bindAddress: String): Flow<ServerProgress> = callbackFlow {
        if (serverRunning.getAndSet(true)) {
            trySend(ServerProgress.Error("Server is already running"))
            close()
            return@callbackFlow
        }

        var serverSock: TCPServerSocket? = null

        try {
            trySend(ServerProgress.Starting(port))

            serverSock = withContext(ioDispatcher) {
                socketManager.createTcpServerSocket(port)
            }
            serverSocket = serverSock

            val status = ServerStatus.running(port, bindAddress)
            serverStatusFlow.value = status
            trySend(ServerProgress.Ready(status))

            var connectionCount = 0

            while (serverRunning.get() && !serverSock.isClosed) {
                try {
                    serverSock.setSoTimeout(1000)
                    val clientSocket = withContext(ioDispatcher) {
                        serverSock.accept()
                    }

                    connectionCount++
                    val connectionId = connectionCount
                    val clientAddress = clientSocket.remoteAddress.hostAddress ?: "unknown"
                    val clientPort = clientSocket.remotePort

                    trySend(
                        ServerProgress.ClientConnected(
                            clientAddress = clientAddress,
                            clientPort = clientPort,
                            connectionId = connectionId
                        )
                    )

                    serverStatusFlow.value = serverStatusFlow.value.copy(
                        activeConnections = serverStatusFlow.value.activeConnections + 1,
                        totalConnectionsServed = serverStatusFlow.value.totalConnectionsServed + 1,
                        lastClientAddress = clientAddress
                    )

                    // Handle client connection (simplified - read cookie and params)
                    try {
                        withContext(ioDispatcher) {
                            val cookie = controlMessageHandler.readCookie(clientSocket.source)

                            // Send PARAM_EXCHANGE state
                            controlMessageHandler.writeState(
                                clientSocket.sink,
                                IPerf3Constants.State.PARAM_EXCHANGE
                            )

                            // Read test params
                            val params = controlMessageHandler.readTestParams(clientSocket.source)

                            // Send CREATE_STREAMS
                            controlMessageHandler.writeState(
                                clientSocket.sink,
                                IPerf3Constants.State.CREATE_STREAMS
                            )

                            // Accept data connection
                            val dataClientSocket = serverSock.accept()
                            controlMessageHandler.readCookie(dataClientSocket.source)

                            // Send TEST_START
                            controlMessageHandler.writeState(
                                clientSocket.sink,
                                IPerf3Constants.State.TEST_START
                            )

                            // Send TEST_RUNNING
                            controlMessageHandler.writeState(
                                clientSocket.sink,
                                IPerf3Constants.State.TEST_RUNNING
                            )

                            // Receive/send data for the duration
                            val buffer = ByteArray(params.len)
                            val durationMs = params.durationMs
                            val startTime = System.nanoTime()
                            var totalBytes = 0L

                            while (serverRunning.get()) {
                                val elapsed = (System.nanoTime() - startTime) / 1_000_000
                                if (elapsed >= durationMs) break

                                try {
                                    if (params.reverse) {
                                        dataClientSocket.sink.write(buffer)
                                        dataClientSocket.sink.flush()
                                    } else {
                                        val bytesRead = dataClientSocket.source.read(buffer, 0, buffer.size)
                                        if (bytesRead <= 0) break
                                        totalBytes += bytesRead
                                    }
                                } catch (_: IOException) {
                                    break
                                }
                            }

                            // Read TEST_END from client
                            try {
                                controlMessageHandler.readState(clientSocket.source)
                            } catch (_: Exception) {}

                            // Send EXCHANGE_RESULTS
                            controlMessageHandler.writeState(
                                clientSocket.sink,
                                IPerf3Constants.State.EXCHANGE_RESULTS
                            )

                            // Write server results
                            controlMessageHandler.writeJsonMessage(clientSocket.sink, "{}")

                            // Read client results
                            try {
                                controlMessageHandler.readJsonMessage(clientSocket.source)
                            } catch (_: Exception) {}

                            // Send DISPLAY_RESULTS
                            controlMessageHandler.writeState(
                                clientSocket.sink,
                                IPerf3Constants.State.DISPLAY_RESULTS
                            )

                            // Send IPERF_DONE
                            controlMessageHandler.writeState(
                                clientSocket.sink,
                                IPerf3Constants.State.IPERF_DONE
                            )

                            serverStatusFlow.value = serverStatusFlow.value.copy(
                                totalBytesTransferred = serverStatusFlow.value.totalBytesTransferred + totalBytes
                            )

                            dataClientSocket.close()
                            clientSocket.close()
                        }
                    } catch (e: Exception) {
                        clientSocket.close()
                    }

                    trySend(
                        ServerProgress.ClientDisconnected(
                            connectionId = connectionId,
                            clientAddress = clientAddress
                        )
                    )

                    serverStatusFlow.value = serverStatusFlow.value.copy(
                        activeConnections = (serverStatusFlow.value.activeConnections - 1).coerceAtLeast(0)
                    )

                } catch (_: java.net.SocketTimeoutException) {
                    // Accept timed out, loop back and check if still running
                    continue
                }
            }

            trySend(ServerProgress.Stopped)

        } catch (e: Exception) {
            trySend(
                ServerProgress.Error(
                    message = e.message ?: "Server error",
                    cause = e
                )
            )
        } finally {
            serverSock?.close()
            serverSocket = null
            serverRunning.set(false)
            serverStatusFlow.value = ServerStatus.STOPPED
        }

        close()

        awaitClose {
            serverSocket?.close()
            serverRunning.set(false)
        }
    }.flowOn(ioDispatcher)

    override suspend fun cancelClientTest() {
        clientCancelled.set(true)
        controlSocket?.close()
        dataSocket?.close()
    }

    override suspend fun stopServer() {
        serverRunning.set(false)
        serverSocket?.close()
    }

    override fun getServerStatus(): Flow<ServerStatus> = serverStatusFlow

    override fun isClientTestRunning(): Boolean = clientRunning.get()

    override fun isServerRunning(): Boolean = serverRunning.get()

    /**
     * Handles unexpected protocol states by returning an appropriate error progress.
     */
    private fun handleUnexpectedState(state: Int, config: TestConfiguration): TestProgress? {
        return when (state) {
            IPerf3Constants.State.ACCESS_DENIED ->
                TestProgress.Error("Access denied by server")
            IPerf3Constants.State.SERVER_ERROR ->
                TestProgress.Error("Server error")
            IPerf3Constants.State.SERVER_TERMINATE ->
                TestProgress.Error("Server terminated the connection")
            else ->
                TestProgress.Error(
                    "Unexpected protocol state: ${IPerf3Constants.State.nameOf(state)}"
                )
        }
    }
}
