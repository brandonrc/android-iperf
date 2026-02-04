package com.iperf3.android.data.source.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iperf3.android.data.source.local.database.dao.IntervalDao
import com.iperf3.android.data.source.local.database.dao.TestResultDao
import com.iperf3.android.data.source.local.database.entity.IntervalEntity
import com.iperf3.android.data.source.local.database.entity.TestResultEntity

/**
 * Room database for the iperf3 Android app.
 *
 * This database stores test results and their associated interval data.
 * It uses cascading deletes so that when a test result is deleted,
 * all its intervals are automatically deleted as well.
 */
@Database(
    entities = [
        TestResultEntity::class,
        IntervalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Gets the DAO for test results.
     */
    abstract fun testResultDao(): TestResultDao

    /**
     * Gets the DAO for intervals.
     */
    abstract fun intervalDao(): IntervalDao

    companion object {
        const val DATABASE_NAME = "iperf3_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton database instance.
         *
         * This method is thread-safe and ensures only one instance
         * of the database is created.
         *
         * @param context Application context
         * @return The database instance
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Creates an in-memory database for testing.
         *
         * @param context Application context
         * @return An in-memory database instance
         */
        fun createInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
