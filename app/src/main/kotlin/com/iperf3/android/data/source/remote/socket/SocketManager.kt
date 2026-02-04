package com.iperf3.android.data.source.remote.socket

import okio.BufferedSink
import okio.BufferedSource
import java.io.Closeable
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Abstraction for socket management in iperf3 tests.
 *
 * This interface provides a common API for creating and managing
 * TCP and UDP sockets for both client and server modes.
 */
interface SocketManager {

    /**
     * Creates a TCP client socket connected to the specified host and port.
     *
     * @param host The hostname or IP address to connect to
     * @param port The port number to connect to
     * @param timeout Connection timeout in milliseconds
     * @return A connected TCPSocket
     */
    suspend fun createTcpSocket(
        host: String,
        port: Int,
        timeout: Int = 30000
    ): TCPSocket

    /**
     * Creates a TCP server socket listening on the specified port.
     *
     * @param port The port to listen on
     * @param backlog The maximum number of pending connections
     * @return A listening TCPServerSocket
     */
    suspend fun createTcpServerSocket(
        port: Int,
        backlog: Int = 128
    ): TCPServerSocket

    /**
     * Creates a UDP socket bound to the specified port.
     *
     * @param port The port to bind to (0 for any available port)
     * @return A UDPSocket
     */
    suspend fun createUdpSocket(port: Int = 0): UDPSocket
}

/**
 * Represents a connected TCP socket.
 */
interface TCPSocket : Closeable {

    /**
     * The local address of this socket
     */
    val localAddress: InetAddress

    /**
     * The local port of this socket
     */
    val localPort: Int

    /**
     * The remote address this socket is connected to
     */
    val remoteAddress: InetAddress

    /**
     * The remote port this socket is connected to
     */
    val remotePort: Int

    /**
     * Whether this socket is connected
     */
    val isConnected: Boolean

    /**
     * Whether this socket is closed
     */
    val isClosed: Boolean

    /**
     * The buffered source for reading from this socket
     */
    val source: BufferedSource

    /**
     * The buffered sink for writing to this socket
     */
    val sink: BufferedSink

    /**
     * Sets the socket timeout for read operations.
     *
     * @param timeout Timeout in milliseconds
     */
    fun setSoTimeout(timeout: Int)

    /**
     * Enables/disables TCP_NODELAY (disables Nagle's algorithm).
     *
     * @param enable True to disable Nagle's algorithm
     */
    fun setTcpNoDelay(enable: Boolean)

    /**
     * Sets the socket send buffer size.
     *
     * @param size Buffer size in bytes
     */
    fun setSendBufferSize(size: Int)

    /**
     * Sets the socket receive buffer size.
     *
     * @param size Buffer size in bytes
     */
    fun setReceiveBufferSize(size: Int)

    /**
     * Gets the actual send buffer size.
     *
     * @return Buffer size in bytes
     */
    fun getSendBufferSize(): Int

    /**
     * Gets the actual receive buffer size.
     *
     * @return Buffer size in bytes
     */
    fun getReceiveBufferSize(): Int

    /**
     * Shuts down the output side of the socket.
     */
    fun shutdownOutput()

    /**
     * Shuts down the input side of the socket.
     */
    fun shutdownInput()
}

/**
 * Represents a TCP server socket that accepts incoming connections.
 */
interface TCPServerSocket : Closeable {

    /**
     * The port this server is listening on
     */
    val port: Int

    /**
     * The address this server is bound to
     */
    val localAddress: InetAddress

    /**
     * Whether this server socket is closed
     */
    val isClosed: Boolean

    /**
     * Accepts an incoming connection.
     *
     * This method blocks until a connection is available.
     *
     * @return The accepted TCPSocket
     */
    suspend fun accept(): TCPSocket

    /**
     * Sets the timeout for accept operations.
     *
     * @param timeout Timeout in milliseconds (0 for infinite)
     */
    fun setSoTimeout(timeout: Int)

    /**
     * Sets the receive buffer size for accepted sockets.
     *
     * @param size Buffer size in bytes
     */
    fun setReceiveBufferSize(size: Int)
}

/**
 * Represents a UDP socket for datagram communication.
 */
interface UDPSocket : Closeable {

    /**
     * The local address of this socket
     */
    val localAddress: InetAddress

    /**
     * The local port of this socket
     */
    val localPort: Int

    /**
     * Whether this socket is closed
     */
    val isClosed: Boolean

    /**
     * Whether this socket is connected to a specific address
     */
    val isConnected: Boolean

    /**
     * Connects this socket to a remote address.
     *
     * After connecting, send() can be used without specifying address.
     *
     * @param address The remote address to connect to
     * @param port The remote port to connect to
     */
    suspend fun connect(address: String, port: Int)

    /**
     * Sends a datagram to the specified address.
     *
     * @param data The data to send
     * @param address The destination address
     * @param port The destination port
     */
    suspend fun send(data: ByteArray, address: InetAddress, port: Int)

    /**
     * Sends a datagram to the connected address.
     *
     * Requires the socket to be connected first.
     *
     * @param data The data to send
     */
    suspend fun send(data: ByteArray)

    /**
     * Receives a datagram.
     *
     * @param maxSize Maximum size of datagram to receive
     * @return Pair of (data, source address)
     */
    suspend fun receive(maxSize: Int): Pair<ByteArray, InetSocketAddress>

    /**
     * Sets the socket timeout for receive operations.
     *
     * @param timeout Timeout in milliseconds (0 for infinite)
     */
    fun setSoTimeout(timeout: Int)

    /**
     * Sets the socket send buffer size.
     *
     * @param size Buffer size in bytes
     */
    fun setSendBufferSize(size: Int)

    /**
     * Sets the socket receive buffer size.
     *
     * @param size Buffer size in bytes
     */
    fun setReceiveBufferSize(size: Int)

    /**
     * Enables/disables broadcasting.
     *
     * @param enable True to enable broadcasting
     */
    fun setBroadcast(enable: Boolean)
}
