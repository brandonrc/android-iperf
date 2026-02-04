package com.iperf3.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3.android.domain.model.IntervalResult
import com.iperf3.android.domain.model.TestConfiguration
import com.iperf3.android.domain.model.TestProgress
import com.iperf3.android.domain.model.TestResult
import com.iperf3.android.domain.repository.HistoryRepository
import com.iperf3.android.domain.repository.IPerf3Repository
import com.iperf3.android.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestUiState(
    val serverHost: String = "",
    val serverPort: Int = 5201,
    val protocol: String = "TCP",
    val duration: Int = 10,
    val streams: Int = 1,
    val reverse: Boolean = false,
    val bidirectional: Boolean = false,
    val progress: TestProgress = TestProgress.Idle,
    val intervals: List<IntervalResult> = emptyList(),
    val currentBandwidthMbps: Double = 0.0,
    val error: String? = null
)

@HiltViewModel
class TestViewModel @Inject constructor(
    private val iperfRepository: IPerf3Repository,
    private val historyRepository: HistoryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _testState = MutableStateFlow(TestUiState())
    val testState: StateFlow<TestUiState> = _testState.asStateFlow()

    private var testJob: Job? = null

    init {
        loadDefaults()
    }

    fun loadDefaults() {
        viewModelScope.launch {
            val host = preferencesRepository.getLastServerHost().first()
            val port = preferencesRepository.getServerPort().first()
            val duration = preferencesRepository.getDefaultDuration().first()
            val streams = preferencesRepository.getDefaultParallelStreams().first()
            val protocol = preferencesRepository.getDefaultProtocol().first()

            _testState.update { state ->
                state.copy(
                    serverHost = host,
                    serverPort = port,
                    duration = duration,
                    streams = streams,
                    protocol = protocol
                )
            }
        }
    }

    fun startTest(config: TestConfiguration? = null) {
        val testConfig = config ?: buildConfigFromState()

        val errors = testConfig.validate()
        if (errors.isNotEmpty()) {
            _testState.update { it.copy(error = errors.joinToString("; ")) }
            return
        }

        testJob?.cancel()
        _testState.update { state ->
            state.copy(
                progress = TestProgress.Idle,
                intervals = emptyList(),
                currentBandwidthMbps = 0.0,
                error = null
            )
        }

        testJob = viewModelScope.launch {
            preferencesRepository.setLastServerHost(testConfig.serverHost)

            iperfRepository.runClientTest(testConfig).collect { progress ->
                when (progress) {
                    is TestProgress.Interval -> {
                        _testState.update { state ->
                            state.copy(
                                progress = progress,
                                intervals = state.intervals + progress.interval,
                                currentBandwidthMbps = progress.interval.mbps
                            )
                        }
                    }
                    is TestProgress.Complete -> {
                        _testState.update { state ->
                            state.copy(
                                progress = progress,
                                currentBandwidthMbps = progress.result.avgMbps
                            )
                        }
                        historyRepository.saveTestResult(progress.result)
                    }
                    is TestProgress.Error -> {
                        _testState.update { state ->
                            state.copy(
                                progress = progress,
                                error = progress.message
                            )
                        }
                        progress.partialResult?.let { partial ->
                            historyRepository.saveTestResult(partial)
                        }
                    }
                    is TestProgress.Cancelled -> {
                        _testState.update { state ->
                            state.copy(progress = progress)
                        }
                        progress.partialResult?.let { partial ->
                            historyRepository.saveTestResult(partial)
                        }
                    }
                    else -> {
                        _testState.update { state ->
                            state.copy(progress = progress)
                        }
                    }
                }
            }
        }
    }

    fun stopTest() {
        viewModelScope.launch {
            iperfRepository.cancelClientTest()
        }
        testJob?.cancel()
        testJob = null
        _testState.update { state ->
            state.copy(
                progress = TestProgress.Cancelled()
            )
        }
    }

    fun updateConfig(
        serverHost: String? = null,
        serverPort: Int? = null,
        protocol: String? = null,
        duration: Int? = null,
        streams: Int? = null,
        reverse: Boolean? = null,
        bidirectional: Boolean? = null
    ) {
        _testState.update { state ->
            state.copy(
                serverHost = serverHost ?: state.serverHost,
                serverPort = serverPort ?: state.serverPort,
                protocol = protocol ?: state.protocol,
                duration = duration ?: state.duration,
                streams = streams ?: state.streams,
                reverse = reverse ?: state.reverse,
                bidirectional = bidirectional ?: state.bidirectional,
                error = null
            )
        }
    }

    fun clearError() {
        _testState.update { it.copy(error = null) }
    }

    private fun buildConfigFromState(): TestConfiguration {
        val state = _testState.value
        return TestConfiguration(
            serverHost = state.serverHost,
            serverPort = state.serverPort,
            protocol = TestConfiguration.Protocol.fromString(state.protocol),
            duration = state.duration * 1000L,
            numStreams = state.streams,
            reverse = state.reverse,
            bidirectional = state.bidirectional
        )
    }

    override fun onCleared() {
        super.onCleared()
        testJob?.cancel()
    }
}
