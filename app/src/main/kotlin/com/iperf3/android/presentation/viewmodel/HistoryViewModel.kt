package com.iperf3.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3.android.domain.model.TestResult
import com.iperf3.android.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val results: StateFlow<List<TestResult>> = historyRepository.getAllResults()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _exportedJson = MutableStateFlow<String?>(null)
    val exportedJson: StateFlow<String?> = _exportedJson.asStateFlow()

    fun deleteResult(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteResult(id)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            historyRepository.deleteAll()
        }
    }

    fun exportResults() {
        viewModelScope.launch {
            val json = historyRepository.exportToJson()
            _exportedJson.value = json
        }
    }

    fun clearExportedJson() {
        _exportedJson.value = null
    }
}
