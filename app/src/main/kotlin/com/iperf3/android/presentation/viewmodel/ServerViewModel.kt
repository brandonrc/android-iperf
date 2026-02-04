package com.iperf3.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3.android.domain.model.ServerProgress
import com.iperf3.android.domain.model.ServerStatus
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

data class ServerConnectionInfo(
    val clientAddress: String,
    val bandwidth: String,
    val timestamp: Long
)

data class ServerUiState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val isRunning: Boolean = false,
    val port: Int = 5201,
    val connections: List<ServerConnectionInfo> = emptyList(),
    val log: List<String> = emptyList()
)

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val iperfRepository: IPerf3Repository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _serverState = MutableStateFlow(ServerUiState())
    val serverState: StateFlow<ServerUiState> = _serverState.asStateFlow()

    private var serverJob: Job? = null

    init {
        viewModelScope.launch {
            val port = preferencesRepository.getServerPort().first()
            _serverState.update { it.copy(port = port) }
        }
    }

    fun startServer(port: Int? = null) {
        val serverPort = port ?: _serverState.value.port

        serverJob?.cancel()
        _serverState.update { state ->
            state.copy(
                port = serverPort,
                log = state.log + "Starting server on port $serverPort...",
                connections = emptyList()
            )
        }

        serverJob = viewModelScope.launch {
            iperfRepository.startServer(port = serverPort).collect { progress ->
                when (progress) {
                    is ServerProgress.Starting -> {
                        _serverState.update { state ->
                            state.copy(
                                log = state.log + "Server starting on port ${progress.port}..."
                            )
                        }
                    }
                    is ServerProgress.Ready -> {
                        _serverState.update { state ->
                            state.copy(
                                status = progress.status,
                                isRunning = true,
                                log = state.log + "Server ready and listening"
                            )
                        }
                    }
                    is ServerProgress.ClientConnected -> {
                        val connectionInfo = ServerConnectionInfo(
                            clientAddress = progress.clientAddress,
                            bandwidth = "Connecting...",
                            timestamp = System.currentTimeMillis()
                        )
                        _serverState.update { state ->
                            state.copy(
                                connections = state.connections + connectionInfo,
                                log = state.log + "Client connected: ${progress.clientAddress}:${progress.clientPort}"
                            )
                        }
                    }
                    is ServerProgress.TestRunning -> {
                        _serverState.update { state ->
                            val updatedConnections = state.connections.map { conn ->
                                if (conn.clientAddress == progress.clientAddress) {
                                    conn.copy(bandwidth = progress.interval.formatBandwidth())
                                } else {
                                    conn
                                }
                            }
                            state.copy(connections = updatedConnections)
                        }
                    }
                    is ServerProgress.TestComplete -> {
                        _serverState.update { state ->
                            state.copy(
                                log = state.log + "Test complete for ${progress.clientAddress}: ${progress.result.formatAvgBandwidth()}"
                            )
                        }
                    }
                    is ServerProgress.ClientDisconnected -> {
                        _serverState.update { state ->
                            state.copy(
                                connections = state.connections.filter {
                                    it.clientAddress != progress.clientAddress
                                },
                                log = state.log + "Client disconnected: ${progress.clientAddress}"
                            )
                        }
                    }
                    is ServerProgress.Error -> {
                        _serverState.update { state ->
                            state.copy(
                                status = ServerStatus.error(progress.message),
                                isRunning = false,
                                log = state.log + "Error: ${progress.message}"
                            )
                        }
                    }
                    is ServerProgress.Stopped -> {
                        _serverState.update { state ->
                            state.copy(
                                status = ServerStatus.STOPPED,
                                isRunning = false,
                                connections = emptyList(),
                                log = state.log + "Server stopped"
                            )
                        }
                    }
                }
            }
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            iperfRepository.stopServer()
        }
        serverJob?.cancel()
        serverJob = null
        _serverState.update { state ->
            state.copy(
                status = ServerStatus.STOPPED,
                isRunning = false,
                connections = emptyList(),
                log = state.log + "Server stopped"
            )
        }
    }

    fun updatePort(port: Int) {
        _serverState.update { it.copy(port = port) }
    }

    fun clearLog() {
        _serverState.update { it.copy(log = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        serverJob?.cancel()
    }
}
