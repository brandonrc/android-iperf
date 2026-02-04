package com.iperf3.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main Application class for iPerf3 Android.
 *
 * This class is annotated with @HiltAndroidApp to enable Hilt dependency injection
 * throughout the application. It also configures WorkManager for scheduled tests
 * and sets up notification channels.
 */
@HiltAndroidApp
class IPerf3Application : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Test progress channel
            val testChannel = NotificationChannel(
                CHANNEL_TEST_PROGRESS,
                getString(R.string.notification_channel_test),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_test_desc)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(testChannel)
        }
    }

    companion object {
        const val CHANNEL_TEST_PROGRESS = "test_progress"
    }
}
