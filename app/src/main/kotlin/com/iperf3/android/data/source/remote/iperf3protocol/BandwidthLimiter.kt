package com.iperf3.android.data.source.remote.iperf3protocol

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

/**
 * Token bucket algorithm implementation for bandwidth limiting.
 *
 * This class limits the rate at which data can be sent by implementing
 * a token bucket algorithm. Tokens are added to the bucket at a constant
 * rate (determined by the bandwidth limit), and sending data consumes tokens.
 *
 * If the bucket is empty, the sender must wait for more tokens to be generated.
 *
 * @property bandwidthBps Target bandwidth limit in bits per second
 * @property burstSize Maximum burst size in bytes (bucket capacity)
 */
class BandwidthLimiter(
    private val bandwidthBps: Long,
    private val burstSize: Long = calculateDefaultBurstSize(bandwidthBps)
) {
    // Bandwidth in bytes per second
    private val bandwidthBytesPerSecond: Double = bandwidthBps / 8.0

    // Current number of tokens in the bucket (in bytes)
    private var tokens: Double = burstSize.toDouble()

    // Last time tokens were updated
    private var lastUpdateTimeNanos: Long = System.nanoTime()

    // Total bytes consumed (for statistics)
    private val totalBytesConsumed = AtomicLong(0)

    // Lock for thread safety
    private val lock = Any()

    /**
     * Attempts to acquire tokens for sending data.
     *
     * If sufficient tokens are available, they are consumed immediately.
     * If not, this method suspends until enough tokens are available.
     *
     * @param bytes Number of bytes to send
     * @return Time waited in milliseconds (0 if no wait was needed)
     */
    suspend fun acquireTokens(bytes: Int): Long {
        if (bandwidthBps <= 0) {
            // No limit
            totalBytesConsumed.addAndGet(bytes.toLong())
            return 0
        }

        var waitTime = 0L

        synchronized(lock) {
            updateTokens()

            val bytesNeeded = bytes.toDouble()

            if (tokens >= bytesNeeded) {
                // Enough tokens available
                tokens -= bytesNeeded
                totalBytesConsumed.addAndGet(bytes.toLong())
            } else {
                // Need to wait for more tokens
                val tokensNeeded = bytesNeeded - tokens
                waitTime = ((tokensNeeded / bandwidthBytesPerSecond) * 1000).toLong()

                // Consume all available tokens
                tokens = 0.0
            }
        }

        if (waitTime > 0) {
            delay(waitTime)

            synchronized(lock) {
                // Update tokens after waiting
                updateTokens()
                totalBytesConsumed.addAndGet(bytes.toLong())
            }
        }

        return waitTime
    }

    /**
     * Checks if tokens are available without consuming them.
     *
     * @param bytes Number of bytes to check
     * @return True if enough tokens are available
     */
    fun hasTokens(bytes: Int): Boolean {
        if (bandwidthBps <= 0) return true

        synchronized(lock) {
            updateTokens()
            return tokens >= bytes
        }
    }

    /**
     * Returns the estimated wait time for acquiring the specified bytes.
     *
     * @param bytes Number of bytes to acquire
     * @return Estimated wait time in milliseconds
     */
    fun estimatedWaitTime(bytes: Int): Long {
        if (bandwidthBps <= 0) return 0

        synchronized(lock) {
            updateTokens()

            val bytesNeeded = bytes.toDouble()
            if (tokens >= bytesNeeded) return 0

            val tokensNeeded = bytesNeeded - tokens
            return ((tokensNeeded / bandwidthBytesPerSecond) * 1000).toLong()
        }
    }

    /**
     * Gets the current fill level of the token bucket.
     *
     * @return Fill level as a percentage (0.0 to 1.0)
     */
    fun getFillLevel(): Double {
        synchronized(lock) {
            updateTokens()
            return (tokens / burstSize).coerceIn(0.0, 1.0)
        }
    }

    /**
     * Gets the total bytes consumed by this limiter.
     */
    fun getTotalBytesConsumed(): Long = totalBytesConsumed.get()

    /**
     * Resets the limiter state.
     */
    fun reset() {
        synchronized(lock) {
            tokens = burstSize.toDouble()
            lastUpdateTimeNanos = System.nanoTime()
            totalBytesConsumed.set(0)
        }
    }

    /**
     * Updates the token count based on elapsed time.
     * Must be called while holding the lock.
     */
    private fun updateTokens() {
        val now = System.nanoTime()
        val elapsedNanos = now - lastUpdateTimeNanos
        val elapsedSeconds = elapsedNanos / 1_000_000_000.0

        // Add tokens based on elapsed time
        val tokensToAdd = bandwidthBytesPerSecond * elapsedSeconds
        tokens = (tokens + tokensToAdd).coerceAtMost(burstSize.toDouble())

        lastUpdateTimeNanos = now
    }

    companion object {
        /**
         * Calculates a reasonable default burst size based on bandwidth.
         *
         * The burst size is set to allow ~100ms of data at the target rate,
         * with a minimum of 64KB and maximum of 1MB.
         */
        fun calculateDefaultBurstSize(bandwidthBps: Long): Long {
            if (bandwidthBps <= 0) return 1024 * 1024 // 1MB for unlimited

            // Allow 100ms burst
            val burstBytes = (bandwidthBps / 8 / 10).coerceIn(64 * 1024L, 1024 * 1024L)
            return burstBytes
        }

        /**
         * Creates a limiter for the specified bandwidth in Mbps.
         */
        fun forMbps(mbps: Double): BandwidthLimiter {
            return BandwidthLimiter((mbps * 1_000_000).toLong())
        }

        /**
         * Creates a limiter for the specified bandwidth in Gbps.
         */
        fun forGbps(gbps: Double): BandwidthLimiter {
            return BandwidthLimiter((gbps * 1_000_000_000).toLong())
        }

        /**
         * Creates an unlimited (no-op) limiter.
         */
        fun unlimited(): BandwidthLimiter {
            return BandwidthLimiter(0)
        }
    }
}
