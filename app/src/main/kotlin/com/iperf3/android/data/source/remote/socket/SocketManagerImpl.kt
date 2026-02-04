package com.iperf3.android.data.source.remote.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of SocketManager using Java NIO.
 */
@Singleton
class SocketManagerImpl @Inject constructor() : SocketManager {

    override suspend fun createTcpSocket(
        host: String,
        port: Int,
        timeout: Int
    ): TCPSocket = withContext(Dispatchers.IO) {
        val socket = Socket()
        socket.soTimeout = timeout
        socket.connect(InetSocketAddress(host, port), timeout)
        TCPSocketImpl(socket)
    }

    override suspend fun createTcpServerSocket(
        port: Int,
        backlog: Int
    ): TCPServerSocket = withContext(Dispatchers.IO) {
        val serverSocket = ServerSocket(port, backlog)
        TCPServerSocketImpl(serverSocket)
    }

    override suspend fun createUdpSocket(port: Int): UDPSocket = withContext(Dispatchers.IO) {
        val socket = if (port > 0) {
            DatagramSocket(port)
        } else {
            DatagramSocket()
        }
        UDPSocketImpl(socket)
    }
}

/**
 * Implementation of TCPSocket using Java Socket.
 */
class TCPSocketImpl(
    private val socket: Socket
) : TCPSocket {

    private val _source: BufferedSource by lazy {
        socket.getInputStream().source().buffer()
    }

    private val _sink: BufferedSink by lazy {
        socket.getOutputStream().sink().buffer()
    }

    override val localAddress: InetAddress
        get() = socket.localAddress

    override val localPort: Int
        get() = socket.localPort

    override val remoteAddress: InetAddress
        get() = socket.inetAddress

    override val remotePort: Int
        get() = socket.port

    override val isConnected: Boolean
        get() = socket.isConnected && !socket.isClosed

    override val isClosed: Boolean
        get() = socket.isClosed

    override val source: BufferedSource
        get() = _source

    override val sink: BufferedSink
        get() = _sink

    override fun setSoTimeout(timeout: Int) {
        socket.soTimeout = timeout
    }

    override fun setTcpNoDelay(enable: Boolean) {
        socket.tcpNoDelay = enable
    }

    override fun setSendBufferSize(size: Int) {
        socket.sendBufferSize = size
    }

    override fun setReceiveBufferSize(size: Int) {
        socket.receiveBufferSize = size
    }

    override fun getSendBufferSize(): Int {
        return socket.sendBufferSize
    }

    override fun getReceiveBufferSize(): Int {
        return socket.receiveBufferSize
    }

    override fun shutdownOutput() {
        if (!socket.isOutputShutdown) {
            socket.shutdownOutput()
        }
    }

    override fun shutdownInput() {
        if (!socket.isInputShutdown) {
            socket.shutdownInput()
        }
    }

    override fun close() {
        try {
            _sink.close()
        } catch (_: Exception) {}
        try {
            _source.close()
        } catch (_: Exception) {}
        try {
            socket.close()
        } catch (_: Exception) {}
    }
}

/**
 * Implementation of TCPServerSocket using Java ServerSocket.
 */
class TCPServerSocketImpl(
    private val serverSocket: ServerSocket
) : TCPServerSocket {

    override val port: Int
        get() = serverSocket.localPort

    override val localAddress: InetAddress
        get() = serverSocket.inetAddress

    override val isClosed: Boolean
        get() = serverSocket.isClosed

    override suspend fun accept(): TCPSocket = withContext(Dispatchers.IO) {
        val clientSocket = serverSocket.accept()
        TCPSocketImpl(clientSocket)
    }

    override fun setSoTimeout(timeout: Int) {
        serverSocket.soTimeout = timeout
    }

    override fun setReceiveBufferSize(size: Int) {
        serverSocket.receiveBufferSize = size
    }

    override fun close() {
        try {
            serverSocket.close()
        } catch (_: Exception) {}
    }
}

/**
 * Implementation of UDPSocket using Java DatagramSocket.
 */
class UDPSocketImpl(
    private val socket: DatagramSocket
) : UDPSocket {

    private var connectedAddress: InetAddress? = null
    private var connectedPort: Int = 0

    override val localAddress: InetAddress
        get() = socket.localAddress

    override val localPort: Int
        get() = socket.localPort

    override val isClosed: Boolean
        get() = socket.isClosed

    override val isConnected: Boolean
        get() = socket.isConnected

    override suspend fun connect(address: String, port: Int) = withContext(Dispatchers.IO) {
        val addr = InetAddress.getByName(address)
        socket.connect(addr, port)
        connectedAddress = addr
        connectedPort = port
    }

    override suspend fun send(data: ByteArray, address: InetAddress, port: Int) =
        withContext(Dispatchers.IO) {
            val packet = DatagramPacket(data, data.size, address, port)
            socket.send(packet)
        }

    override suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        val address = connectedAddress
            ?: throw IOException("Socket is not connected")
        val packet = DatagramPacket(data, data.size, address, connectedPort)
        socket.send(packet)
    }

    override suspend fun receive(maxSize: Int): Pair<ByteArray, InetSocketAddress> =
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(maxSize)
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)

            val data = buffer.copyOf(packet.length)
            val sourceAddress = InetSocketAddress(packet.address, packet.port)

            Pair(data, sourceAddress)
        }

    override fun setSoTimeout(timeout: Int) {
        socket.soTimeout = timeout
    }

    override fun setSendBufferSize(size: Int) {
        socket.sendBufferSize = size
    }

    override fun setReceiveBufferSize(size: Int) {
        socket.receiveBufferSize = size
    }

    override fun setBroadcast(enable: Boolean) {
        socket.broadcast = enable
    }

    override fun close() {
        try {
            socket.close()
        } catch (_: Exception) {}
    }
}
