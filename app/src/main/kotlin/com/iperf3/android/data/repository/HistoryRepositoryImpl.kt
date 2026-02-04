package com.iperf3.android.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iperf3.android.data.source.local.database.dao.IntervalDao
import com.iperf3.android.data.source.local.database.dao.TestResultDao
import com.iperf3.android.data.source.local.mapper.IntervalMapper
import com.iperf3.android.data.source.local.mapper.TestResultMapper
import com.iperf3.android.di.IoDispatcher
import com.iperf3.android.domain.model.IntervalResult
import com.iperf3.android.domain.model.TestResult
import com.iperf3.android.domain.repository.HistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [HistoryRepository] backed by Room database.
 *
 * Handles persistence of test results and their associated interval data,
 * including export/import functionality via JSON serialization.
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val testResultDao: TestResultDao,
    private val intervalDao: IntervalDao,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : HistoryRepository {

    override suspend fun saveTestResult(result: TestResult): Long = withContext(ioDispatcher) {
        val entity = TestResultMapper.toEntity(result)
        val insertedId = testResultDao.insert(entity)

        if (result.intervals.isNotEmpty()) {
            val intervalEntities = IntervalMapper.toEntityList(result.intervals, insertedId)
            intervalDao.insertAll(intervalEntities)
        }

        insertedId
    }

    override fun getAllResults(): Flow<List<TestResult>> {
        return testResultDao.getAll()
            .map { entities -> TestResultMapper.toDomainList(entities) }
            .flowOn(ioDispatcher)
    }

    override fun getResult(id: Long): Flow<TestResult?> {
        return testResultDao.getByIdFlow(id)
            .map { entity -> entity?.let { TestResultMapper.toDomain(it) } }
            .flowOn(ioDispatcher)
    }

    override fun getIntervals(testId: Long): Flow<List<IntervalResult>> {
        return intervalDao.getByTestId(testId)
            .map { entities -> IntervalMapper.toDomainList(entities) }
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteResult(id: Long) = withContext(ioDispatcher) {
        testResultDao.deleteById(id)
    }

    override suspend fun deleteOlderThan(days: Int): Int = withContext(ioDispatcher) {
        val cutoffTimestamp = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        testResultDao.deleteOlderThan(cutoffTimestamp)
    }

    override suspend fun deleteAll(): Int = withContext(ioDispatcher) {
        testResultDao.deleteAll()
    }

    override fun getCount(): Flow<Int> {
        return testResultDao.getCount()
            .flowOn(ioDispatcher)
    }

    override fun getResultsForServer(serverHost: String): Flow<List<TestResult>> {
        return testResultDao.getByServer(serverHost)
            .map { entities -> TestResultMapper.toDomainList(entities) }
            .flowOn(ioDispatcher)
    }

    override fun getMostRecent(): Flow<TestResult?> {
        return testResultDao.getMostRecent()
            .map { entity -> entity?.let { TestResultMapper.toDomain(it) } }
            .flowOn(ioDispatcher)
    }

    override fun getResultsInRange(startTime: Long, endTime: Long): Flow<List<TestResult>> {
        return testResultDao.getInRange(startTime, endTime)
            .map { entities -> TestResultMapper.toDomainList(entities) }
            .flowOn(ioDispatcher)
    }

    override suspend fun exportToJson(ids: List<Long>?): String = withContext(ioDispatcher) {
        val entities = if (ids != null) {
            testResultDao.getByIds(ids)
        } else {
            val allIds = testResultDao.getAllIds()
            testResultDao.getByIds(allIds)
        }

        val results = entities.map { entity ->
            val intervalEntities = intervalDao.getByTestIdSync(entity.id)
            TestResultMapper.toDomainWithIntervals(entity, intervalEntities)
        }

        gson.toJson(results)
    }

    override suspend fun importFromJson(json: String): Int = withContext(ioDispatcher) {
        val type = object : TypeToken<List<TestResult>>() {}.type
        val results: List<TestResult> = gson.fromJson(json, type)

        var importedCount = 0
        for (result in results) {
            val entity = TestResultMapper.toEntity(result.copy(id = 0))
            val insertedId = testResultDao.insert(entity)

            if (result.intervals.isNotEmpty()) {
                val intervalEntities = result.intervals.map { interval ->
                    IntervalMapper.toEntity(interval.copy(id = 0), insertedId)
                }
                intervalDao.insertAll(intervalEntities)
            }

            importedCount++
        }

        importedCount
    }
}
