package com.iperf3.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3.android.domain.repository.HistoryRepository
import com.iperf3.android.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val defaultDuration: StateFlow<Int> = preferencesRepository.getDefaultDuration()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 10)

    val defaultStreams: StateFlow<Int> = preferencesRepository.getDefaultParallelStreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val defaultProtocol: StateFlow<String> = preferencesRepository.getDefaultProtocol()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "TCP")

    val defaultBandwidthLimit: StateFlow<Long> = preferencesRepository.getDefaultBandwidthLimit()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val serverPort: StateFlow<Int> = preferencesRepository.getServerPort()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5201)

    val darkModeEnabled: StateFlow<Boolean> = preferencesRepository.getDarkModeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val showNotifications: StateFlow<Boolean> = preferencesRepository.getShowNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val historyRetentionDays: StateFlow<Int> = preferencesRepository.getHistoryRetentionDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

    val autoTestEnabled: StateFlow<Boolean> = preferencesRepository.getAutoTestEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoTestInterval: StateFlow<Long> = preferencesRepository.getAutoTestInterval()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3_600_000L)

    val autoTestServerHost: StateFlow<String> = preferencesRepository.getAutoTestServerHost()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val historyCount: StateFlow<Int> = historyRepository.getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun updateDefaultDuration(seconds: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultDuration(seconds) }
    }

    fun updateDefaultStreams(streams: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultParallelStreams(streams) }
    }

    fun updateDefaultProtocol(protocol: String) {
        viewModelScope.launch { preferencesRepository.setDefaultProtocol(protocol) }
    }

    fun updateDefaultBandwidthLimit(bps: Long) {
        viewModelScope.launch { preferencesRepository.setDefaultBandwidthLimit(bps) }
    }

    fun updateServerPort(port: Int) {
        viewModelScope.launch { preferencesRepository.setServerPort(port) }
    }

    fun updateDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkModeEnabled(enabled) }
    }

    fun updateShowNotifications(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowNotifications(show) }
    }

    fun updateHistoryRetentionDays(days: Int) {
        viewModelScope.launch { preferencesRepository.setHistoryRetentionDays(days) }
    }

    fun updateAutoTestEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoTestEnabled(enabled) }
    }

    fun updateAutoTestInterval(intervalMs: Long) {
        viewModelScope.launch { preferencesRepository.setAutoTestInterval(intervalMs) }
    }

    fun updateAutoTestServerHost(host: String) {
        viewModelScope.launch { preferencesRepository.setAutoTestServerHost(host) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.deleteAll() }
    }

    fun resetDefaults() {
        viewModelScope.launch { preferencesRepository.clearAll() }
    }
}
