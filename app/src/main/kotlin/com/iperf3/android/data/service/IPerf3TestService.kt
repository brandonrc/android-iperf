package com.iperf3.android.data.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.iperf3.android.IPerf3Application.Companion.CHANNEL_TEST_PROGRESS
import com.iperf3.android.R
import com.iperf3.android.domain.model.TestConfiguration
import com.iperf3.android.domain.model.TestProgress
import com.iperf3.android.domain.repository.IPerf3Repository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that runs iPerf3 bandwidth tests in the background.
 *
 * This service keeps the test alive even when the user navigates away from the app.
 * It displays an ongoing notification with real-time bandwidth updates and posts
 * a completion or error notification when the test finishes.
 */
@AndroidEntryPoint
class IPerf3TestService : Service() {

    @Inject
    lateinit var repository: IPerf3Repository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var testJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TEST -> {
                val config = extractConfig(intent)
                if (config != null) {
                    startForegroundNotification()
                    runTest(config)
                } else {
                    Log.e(TAG, "Failed to extract test configuration from intent")
                    stopSelf()
                }
            }
            ACTION_STOP_TEST -> {
                stopTest()
            }
        }
        return START_NOT_STICKY
    }

    private fun extractConfig(intent: Intent): TestConfiguration? {
        val host = intent.getStringExtra(EXTRA_SERVER_HOST) ?: return null
        val port = intent.getIntExtra(EXTRA_SERVER_PORT, TestConfiguration.DEFAULT_PORT)
        val protocolStr = intent.getStringExtra(EXTRA_PROTOCOL) ?: "TCP"
        val duration = intent.getLongExtra(EXTRA_DURATION, TestConfiguration.DEFAULT_DURATION_MS)
        val numStreams = intent.getIntExtra(EXTRA_NUM_STREAMS, 1)
        val bandwidthLimit = intent.getLongExtra(EXTRA_BANDWIDTH_LIMIT, -1L)
            .takeIf { it >= 0 }
        val reverse = intent.getBooleanExtra(EXTRA_REVERSE, false)
        val bidirectional = intent.getBooleanExtra(EXTRA_BIDIRECTIONAL, false)
        val reportingInterval = intent.getLongExtra(
            EXTRA_REPORTING_INTERVAL,
            TestConfiguration.DEFAULT_INTERVAL_MS
        )
        val testName = intent.getStringExtra(EXTRA_TEST_NAME) ?: ""

        return TestConfiguration(
            serverHost = host,
            serverPort = port,
            protocol = TestConfiguration.Protocol.fromString(protocolStr),
            duration = duration,
            numStreams = numStreams,
            bandwidthLimit = bandwidthLimit,
            reverse = reverse,
            bidirectional = bidirectional,
            reportingInterval = reportingInterval,
            testName = testName
        )
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_TEST_PROGRESS)
            .setContentTitle("iPerf3 Test")
            .setContentText("Starting test...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun runTest(config: TestConfiguration) {
        testJob?.cancel()
        testJob = serviceScope.launch {
            repository.runClientTest(config).collect { progress ->
                when (progress) {
                    is TestProgress.Connecting -> {
                        updateNotification(
                            "Connecting to ${progress.serverHost}:${progress.serverPort}..."
                        )
                    }
                    is TestProgress.Connected -> {
                        updateNotification("Connected, configuring test...")
                    }
                    is TestProgress.Started -> {
                        updateNotification("Test in progress...")
                    }
                    is TestProgress.Interval -> {
                        val bandwidth = progress.interval.formatBandwidth()
                        updateNotification(
                            "Test in progress... $bandwidth",
                            progress.progressPercent
                        )
                    }
                    is TestProgress.Complete -> {
                        val bandwidth = progress.result.formatAvgBandwidth()
                        showCompletionNotification("Test complete: $bandwidth")
                        stopSelf()
                    }
                    is TestProgress.Error -> {
                        showErrorNotification(progress.message)
                        stopSelf()
                    }
                    is TestProgress.Cancelled -> {
                        showCompletionNotification("Test cancelled")
                        stopSelf()
                    }
                    is TestProgress.Idle -> {
                        // No action needed
                    }
                }
            }
        }
    }

    private fun updateNotification(text: String, progressPercent: Int = -1) {
        val builder = NotificationCompat.Builder(this, CHANNEL_TEST_PROGRESS)
            .setContentTitle("iPerf3 Test")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)

        if (progressPercent in 0..100) {
            builder.setProgress(100, progressPercent, false)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showCompletionNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_TEST_PROGRESS)
            .setContentTitle("iPerf3 Test")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID_RESULT, notification)
    }

    private fun showErrorNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_TEST_PROGRESS)
            .setContentTitle("iPerf3 Test Failed")
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID_RESULT, notification)
    }

    private fun stopTest() {
        testJob?.cancel()
        testJob = null
        serviceScope.launch {
            repository.cancelClientTest()
        }
        stopSelf()
    }

    override fun onDestroy() {
        testJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "IPerf3TestService"

        private const val ACTION_START_TEST = "com.iperf3.android.action.START_TEST"
        private const val ACTION_STOP_TEST = "com.iperf3.android.action.STOP_TEST"

        private const val EXTRA_SERVER_HOST = "extra_server_host"
        private const val EXTRA_SERVER_PORT = "extra_server_port"
        private const val EXTRA_PROTOCOL = "extra_protocol"
        private const val EXTRA_DURATION = "extra_duration"
        private const val EXTRA_NUM_STREAMS = "extra_num_streams"
        private const val EXTRA_BANDWIDTH_LIMIT = "extra_bandwidth_limit"
        private const val EXTRA_REVERSE = "extra_reverse"
        private const val EXTRA_BIDIRECTIONAL = "extra_bidirectional"
        private const val EXTRA_REPORTING_INTERVAL = "extra_reporting_interval"
        private const val EXTRA_TEST_NAME = "extra_test_name"

        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ID_RESULT = 1002

        /**
         * Starts the foreground service with the given test configuration.
         *
         * @param context The context to start the service from
         * @param config The test configuration to execute
         */
        fun startTest(context: Context, config: TestConfiguration) {
            val intent = Intent(context, IPerf3TestService::class.java).apply {
                action = ACTION_START_TEST
                putExtra(EXTRA_SERVER_HOST, config.serverHost)
                putExtra(EXTRA_SERVER_PORT, config.serverPort)
                putExtra(EXTRA_PROTOCOL, config.protocol.name)
                putExtra(EXTRA_DURATION, config.duration)
                putExtra(EXTRA_NUM_STREAMS, config.numStreams)
                config.bandwidthLimit?.let { putExtra(EXTRA_BANDWIDTH_LIMIT, it) }
                putExtra(EXTRA_REVERSE, config.reverse)
                putExtra(EXTRA_BIDIRECTIONAL, config.bidirectional)
                putExtra(EXTRA_REPORTING_INTERVAL, config.reportingInterval)
                putExtra(EXTRA_TEST_NAME, config.testName)
            }
            context.startForegroundService(intent)
        }

        /**
         * Stops any running test and the foreground service.
         *
         * @param context The context to stop the service from
         */
        fun stopTest(context: Context) {
            val intent = Intent(context, IPerf3TestService::class.java).apply {
                action = ACTION_STOP_TEST
            }
            context.startService(intent)
        }
    }
}
