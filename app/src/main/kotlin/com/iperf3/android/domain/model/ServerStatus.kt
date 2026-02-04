package com.iperf3.android.domain.model

/**
 * Represents the current status of the iperf3 server.
 *
 * This class tracks the server's running state, connection information,
 * and statistics for monitoring server activity.
 *
 * @property isRunning Whether the server is currently running and accepting connections
 * @property port The port number the server is listening on
 * @property bindAddress The address the server is bound to (e.g., "0.0.0.0" for all interfaces)
 * @property activeConnections Number of currently active client connections
 * @property totalConnectionsServed Total number of connections served since server started
 * @property totalBytesTransferred Total bytes transferred across all connections
 * @property startTime Unix timestamp when the server was started, null if not running
 * @property lastClientAddress Address of the most recent client, null if no clients yet
 * @property errorMessage Error message if the server failed to start or encountered an error
 */
data class ServerStatus(
    val isRunning: Boolean = false,
    val port: Int = 5201,
    val bindAddress: String = "0.0.0.0",
    val activeConnections: Int = 0,
    val totalConnectionsServed: Long = 0,
    val totalBytesTransferred: Long = 0,
    val startTime: Long? = null,
    val lastClientAddress: String? = null,
    val errorMessage: String? = null
) {
    /**
     * Whether the server is in an error state
     */
    val hasError: Boolean
        get() = errorMessage != null

    /**
     * Uptime in milliseconds since the server was started
     */
    val uptimeMs: Long?
        get() = startTime?.let { System.currentTimeMillis() - it }

    /**
     * Formatted uptime string (e.g., "2h 30m 15s")
     */
    fun formatUptime(): String? {
        val uptime = uptimeMs ?: return null
        val seconds = (uptime / 1000) % 60
        val minutes = (uptime / (1000 * 60)) % 60
        val hours = (uptime / (1000 * 60 * 60))

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Formatted total data transferred
     */
    fun formatTotalData(): String {
        return when {
            totalBytesTransferred >= 1024L * 1024 * 1024 * 1024 ->
                String.format("%.2f TB", totalBytesTransferred / (1024.0 * 1024 * 1024 * 1024))
            totalBytesTransferred >= 1024L * 1024 * 1024 ->
                String.format("%.2f GB", totalBytesTransferred / (1024.0 * 1024 * 1024))
            totalBytesTransferred >= 1024 * 1024 ->
                String.format("%.2f MB", totalBytesTransferred / (1024.0 * 1024))
            totalBytesTransferred >= 1024 ->
                String.format("%.2f KB", totalBytesTransferred / 1024.0)
            else -> "$totalBytesTransferred bytes"
        }
    }

    companion object {
        /**
         * Default stopped server status
         */
        val STOPPED = ServerStatus(isRunning = false)

        /**
         * Creates a running server status
         */
        fun running(port: Int, bindAddress: String = "0.0.0.0"): ServerStatus {
            return ServerStatus(
                isRunning = true,
                port = port,
                bindAddress = bindAddress,
                startTime = System.currentTimeMillis()
            )
        }

        /**
         * Creates an error status
         */
        fun error(message: String, port: Int = 5201): ServerStatus {
            return ServerStatus(
                isRunning = false,
                port = port,
                errorMessage = message
            )
        }
    }
}

/**
 * Represents an active connection to the server
 */
data class ServerConnection(
    val connectionId: Int,
    val clientAddress: String,
    val clientPort: Int,
    val protocol: String,
    val connectedAt: Long,
    val bytesTransferred: Long = 0
) {
    /**
     * Connection duration in milliseconds
     */
    val durationMs: Long
        get() = System.currentTimeMillis() - connectedAt
}
