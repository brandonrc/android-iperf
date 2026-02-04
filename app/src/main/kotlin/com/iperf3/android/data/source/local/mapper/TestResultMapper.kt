package com.iperf3.android.data.source.local.mapper

import com.iperf3.android.data.source.local.database.entity.IntervalEntity
import com.iperf3.android.data.source.local.database.entity.TestResultEntity
import com.iperf3.android.domain.model.IntervalResult
import com.iperf3.android.domain.model.TestResult

/**
 * Mapper functions for converting between TestResult domain models
 * and TestResultEntity database entities.
 */
object TestResultMapper {

    /**
     * Converts a TestResult domain model to a TestResultEntity.
     *
     * Note: The intervals list is not included in the entity.
     * Intervals should be saved separately using IntervalMapper.
     */
    fun toEntity(domain: TestResult): TestResultEntity {
        return TestResultEntity(
            id = domain.id,
            testName = domain.testName,
            serverHost = domain.serverHost,
            serverPort = domain.serverPort,
            timestamp = domain.timestamp,
            protocol = domain.protocol,
            durationMs = domain.duration,
            totalBytes = domain.totalBytes,
            avgBandwidthBps = domain.avgBandwidth,
            minBandwidthBps = domain.minBandwidth,
            maxBandwidthBps = domain.maxBandwidth,
            jitterMs = domain.jitter,
            packetLossPercent = domain.packetLoss,
            totalPackets = domain.totalPackets,
            lostPackets = domain.lostPackets,
            retransmits = domain.retransmits,
            qualityScore = domain.qualityScore,
            numStreams = domain.numStreams,
            reverseMode = domain.reverseMode,
            bidirectional = domain.bidirectional,
            rawJson = domain.rawJson,
            errorMessage = domain.errorMessage
        )
    }

    /**
     * Converts a TestResultEntity to a TestResult domain model.
     *
     * Note: The intervals list will be empty. Intervals should be
     * fetched separately and combined using toDomainWithIntervals().
     */
    fun toDomain(entity: TestResultEntity): TestResult {
        return TestResult(
            id = entity.id,
            testName = entity.testName,
            serverHost = entity.serverHost,
            serverPort = entity.serverPort,
            timestamp = entity.timestamp,
            protocol = entity.protocol,
            duration = entity.durationMs,
            totalBytes = entity.totalBytes,
            avgBandwidth = entity.avgBandwidthBps,
            minBandwidth = entity.minBandwidthBps,
            maxBandwidth = entity.maxBandwidthBps,
            jitter = entity.jitterMs,
            packetLoss = entity.packetLossPercent,
            totalPackets = entity.totalPackets,
            lostPackets = entity.lostPackets,
            retransmits = entity.retransmits,
            qualityScore = entity.qualityScore,
            numStreams = entity.numStreams,
            reverseMode = entity.reverseMode,
            bidirectional = entity.bidirectional,
            intervals = emptyList(),
            rawJson = entity.rawJson,
            errorMessage = entity.errorMessage
        )
    }

    /**
     * Converts a TestResultEntity and its IntervalEntities to a complete
     * TestResult domain model.
     */
    fun toDomainWithIntervals(
        entity: TestResultEntity,
        intervalEntities: List<IntervalEntity>
    ): TestResult {
        val intervals = intervalEntities.map { IntervalMapper.toDomain(it) }
        return TestResult(
            id = entity.id,
            testName = entity.testName,
            serverHost = entity.serverHost,
            serverPort = entity.serverPort,
            timestamp = entity.timestamp,
            protocol = entity.protocol,
            duration = entity.durationMs,
            totalBytes = entity.totalBytes,
            avgBandwidth = entity.avgBandwidthBps,
            minBandwidth = entity.minBandwidthBps,
            maxBandwidth = entity.maxBandwidthBps,
            jitter = entity.jitterMs,
            packetLoss = entity.packetLossPercent,
            totalPackets = entity.totalPackets,
            lostPackets = entity.lostPackets,
            retransmits = entity.retransmits,
            qualityScore = entity.qualityScore,
            numStreams = entity.numStreams,
            reverseMode = entity.reverseMode,
            bidirectional = entity.bidirectional,
            intervals = intervals,
            rawJson = entity.rawJson,
            errorMessage = entity.errorMessage
        )
    }

    /**
     * Converts a list of TestResultEntity to a list of TestResult domain models.
     */
    fun toDomainList(entities: List<TestResultEntity>): List<TestResult> {
        return entities.map { toDomain(it) }
    }
}

/**
 * Mapper functions for converting between IntervalResult domain models
 * and IntervalEntity database entities.
 */
object IntervalMapper {

    /**
     * Converts an IntervalResult domain model to an IntervalEntity.
     */
    fun toEntity(domain: IntervalResult, testId: Long): IntervalEntity {
        return IntervalEntity(
            id = domain.id,
            testId = testId,
            streamId = domain.streamId,
            startTime = domain.startTime,
            endTime = domain.endTime,
            bytesTransferred = domain.bytesTransferred,
            bitsPerSecond = domain.bitsPerSecond,
            retransmits = domain.retransmits,
            congestionWindow = domain.congestionWindow,
            jitterMs = domain.jitter,
            packets = domain.packets,
            lostPackets = domain.lostPackets,
            outOfOrderPackets = domain.outOfOrderPackets
        )
    }

    /**
     * Converts an IntervalEntity to an IntervalResult domain model.
     */
    fun toDomain(entity: IntervalEntity): IntervalResult {
        return IntervalResult(
            id = entity.id,
            testId = entity.testId,
            streamId = entity.streamId,
            startTime = entity.startTime,
            endTime = entity.endTime,
            bytesTransferred = entity.bytesTransferred,
            bitsPerSecond = entity.bitsPerSecond,
            retransmits = entity.retransmits,
            congestionWindow = entity.congestionWindow,
            jitter = entity.jitterMs,
            packets = entity.packets,
            lostPackets = entity.lostPackets,
            outOfOrderPackets = entity.outOfOrderPackets
        )
    }

    /**
     * Converts a list of IntervalResult domain models to IntervalEntities.
     */
    fun toEntityList(domains: List<IntervalResult>, testId: Long): List<IntervalEntity> {
        return domains.map { toEntity(it, testId) }
    }

    /**
     * Converts a list of IntervalEntity to IntervalResult domain models.
     */
    fun toDomainList(entities: List<IntervalEntity>): List<IntervalResult> {
        return entities.map { toDomain(it) }
    }
}
