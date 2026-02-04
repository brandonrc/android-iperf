package com.iperf3.android.data.source.remote.iperf3protocol

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.iperf3.android.data.source.remote.iperf3protocol.message.*
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Handles iperf3 control connection message encoding and decoding.
 *
 * The iperf3 protocol uses a control connection (TCP) for:
 * 1. Initial handshake and cookie exchange
 * 2. Test parameter negotiation (JSON)
 * 3. State transitions
 * 4. Results exchange (JSON)
 *
 * Message format:
 * - State messages: Single byte containing state code
 * - JSON messages: 4-byte length prefix (big-endian) + JSON string
 * - Cookie: 37-byte string (36 chars + null terminator)
 */
class ControlMessageHandler @Inject constructor() {

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

    /**
     * Reads a state byte from the control connection.
     *
     * @param source The buffered source to read from
     * @return The state code as an integer
     * @throws IOException if reading fails
     */
    suspend fun readState(source: BufferedSource): Int {
        val byte = source.readByte()
        return byte.toInt()
    }

    /**
     * Writes a state byte to the control connection.
     *
     * @param sink The buffered sink to write to
     * @param state The state code to send
     */
    suspend fun writeState(sink: BufferedSink, state: Int) {
        sink.writeByte(state)
        sink.flush()
    }

    /**
     * Reads the cookie from the server.
     * The cookie is a 37-byte string (including null terminator).
     *
     * @param source The buffered source to read from
     * @return The cookie string
     */
    suspend fun readCookie(source: BufferedSource): String {
        val cookieBytes = ByteArray(IPerf3Constants.COOKIE_SIZE)
        source.readFully(cookieBytes)
        // Remove null terminator
        return cookieBytes.decodeToString().trimEnd('\u0000')
    }

    /**
     * Writes the cookie to the server/client.
     *
     * @param sink The buffered sink to write to
     * @param cookie The cookie string to send
     */
    suspend fun writeCookie(sink: BufferedSink, cookie: String) {
        val cookieBytes = ByteArray(IPerf3Constants.COOKIE_SIZE)
        val cookieData = cookie.toByteArray(Charsets.US_ASCII)
        System.arraycopy(cookieData, 0, cookieBytes, 0,
            minOf(cookieData.size, IPerf3Constants.COOKIE_SIZE - 1))
        sink.write(cookieBytes)
        sink.flush()
    }

    /**
     * Reads a JSON message from the control connection.
     *
     * Message format: 4-byte length prefix (big-endian) + JSON string
     *
     * @param source The buffered source to read from
     * @return The JSON string
     * @throws IOException if reading fails or message is too large
     */
    suspend fun readJsonMessage(source: BufferedSource): String {
        // Read 4-byte length prefix (big-endian)
        val lengthBytes = ByteArray(4)
        source.readFully(lengthBytes)
        val length = ByteBuffer.wrap(lengthBytes)
            .order(ByteOrder.BIG_ENDIAN)
            .int

        // Validate length
        if (length <= 0 || length > IPerf3Constants.MAX_CONTROL_MESSAGE_SIZE) {
            throw IOException("Invalid JSON message length: $length")
        }

        // Read JSON data
        val jsonBytes = ByteArray(length)
        source.readFully(jsonBytes)
        return jsonBytes.decodeToString()
    }

    /**
     * Writes a JSON message to the control connection.
     *
     * @param sink The buffered sink to write to
     * @param json The JSON string to send
     */
    suspend fun writeJsonMessage(sink: BufferedSink, json: String) {
        val jsonBytes = json.toByteArray(Charsets.UTF_8)

        // Write 4-byte length prefix (big-endian)
        val lengthBytes = ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(jsonBytes.size)
            .array()

        sink.write(lengthBytes)
        sink.write(jsonBytes)
        sink.flush()
    }

    /**
     * Reads and parses test parameters JSON from the control connection.
     *
     * @param source The buffered source to read from
     * @return Parsed TestParams object
     */
    suspend fun readTestParams(source: BufferedSource): TestParams {
        val json = readJsonMessage(source)
        return parseTestParams(json)
    }

    /**
     * Writes test parameters as JSON to the control connection.
     *
     * @param sink The buffered sink to write to
     * @param params The test parameters to send
     */
    suspend fun writeTestParams(sink: BufferedSink, params: TestParams) {
        val json = gson.toJson(params)
        writeJsonMessage(sink, json)
    }

    /**
     * Parses a JSON string into TestParams.
     *
     * @param json The JSON string to parse
     * @return Parsed TestParams object
     */
    fun parseTestParams(json: String): TestParams {
        return gson.fromJson(json, TestParams::class.java)
    }

    /**
     * Serializes TestParams to JSON string.
     *
     * @param params The TestParams object to serialize
     * @return JSON string
     */
    fun serializeTestParams(params: TestParams): String {
        return gson.toJson(params)
    }

    /**
     * Reads and parses iperf3 results JSON.
     *
     * @param source The buffered source to read from
     * @return Parsed IPerf3Results object
     */
    suspend fun readResults(source: BufferedSource): IPerf3Results {
        val json = readJsonMessage(source)
        return parseResults(json)
    }

    /**
     * Writes iperf3 results as JSON.
     *
     * @param sink The buffered sink to write to
     * @param results The results to send
     */
    suspend fun writeResults(sink: BufferedSink, results: IPerf3Results) {
        val json = gson.toJson(results)
        writeJsonMessage(sink, json)
    }

    /**
     * Parses iperf3 results JSON string.
     *
     * @param json The JSON string to parse
     * @return Parsed IPerf3Results object
     */
    fun parseResults(json: String): IPerf3Results {
        return gson.fromJson(json, IPerf3Results::class.java)
    }

    /**
     * Serializes IPerf3Results to JSON string.
     *
     * @param results The results to serialize
     * @return JSON string
     */
    fun serializeResults(results: IPerf3Results): String {
        return gson.toJson(results)
    }

    /**
     * Generates a random cookie for session identification.
     *
     * The cookie is a 36-character alphanumeric string.
     *
     * @return A random cookie string
     */
    fun generateCookie(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..36)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Creates an error response message.
     *
     * @param errorMessage The error description
     * @return JSON string containing the error
     */
    fun createErrorMessage(errorMessage: String): String {
        return gson.toJson(mapOf("error" to errorMessage))
    }

    companion object {
        /**
         * Validates that a state code is valid.
         */
        fun isValidState(state: Int): Boolean {
            return state in listOf(
                IPerf3Constants.State.TEST_START,
                IPerf3Constants.State.TEST_RUNNING,
                IPerf3Constants.State.TEST_END,
                IPerf3Constants.State.PARAM_EXCHANGE,
                IPerf3Constants.State.CREATE_STREAMS,
                IPerf3Constants.State.SERVER_TERMINATE,
                IPerf3Constants.State.CLIENT_TERMINATE,
                IPerf3Constants.State.EXCHANGE_RESULTS,
                IPerf3Constants.State.DISPLAY_RESULTS,
                IPerf3Constants.State.IPERF_START,
                IPerf3Constants.State.IPERF_DONE,
                IPerf3Constants.State.ACCESS_DENIED,
                IPerf3Constants.State.SERVER_ERROR
            )
        }
    }
}
