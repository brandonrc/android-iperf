@file:OptIn(ExperimentalMaterial3Api::class)

package com.iperf3.android.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import com.iperf3.android.domain.model.IntervalResult
import com.iperf3.android.domain.model.TestProgress
import com.iperf3.android.presentation.ui.component.ResultCard
import com.iperf3.android.presentation.ui.component.SpeedGauge
import com.iperf3.android.presentation.viewmodel.TestUiState
import com.iperf3.android.presentation.viewmodel.TestViewModel
import com.iperf3.android.domain.util.ReportGenerator

/**
 * Main test screen for running iPerf3 bandwidth tests.
 *
 * Layout (top to bottom):
 *  1. Server host + port input row
 *  2. Protocol toggle (TCP / UDP)
 *  3. Speed gauge (~40 % of screen)
 *  4. START / STOP button
 *  5. Progress indicator + elapsed time while running
 *  6. Result cards when complete
 *  7. Expandable "Advanced" options
 */
@Composable
fun TestScreen(
    viewModel: TestViewModel = hiltViewModel(),
) {
    val state by viewModel.testState.collectAsState()

    TestScreenContent(
        state = state,
        onHostChange = { viewModel.updateConfig(serverHost = it) },
        onPortChange = { port ->
            port.toIntOrNull()?.let { viewModel.updateConfig(serverPort = it) }
        },
        onProtocolChange = { viewModel.updateConfig(protocol = it) },
        onDurationChange = { viewModel.updateConfig(duration = it) },
        onStreamsChange = { viewModel.updateConfig(streams = it) },
        onReverseChange = { viewModel.updateConfig(reverse = it) },
        onBidirectionalChange = { viewModel.updateConfig(bidirectional = it) },
        onStartTest = { viewModel.startTest() },
        onStopTest = { viewModel.stopTest() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TestScreenContent(
    state: TestUiState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onProtocolChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onStreamsChange: (Int) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    onBidirectionalChange: (Boolean) -> Unit,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
) {
    val context = LocalContext.current
    val isRunning = state.progress.isRunning
    val isComplete = state.progress is TestProgress.Complete
    val isError = state.progress is TestProgress.Error
    var advancedExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ---- 1. Server host + port ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.serverHost,
                onValueChange = onHostChange,
                label = { Text("Server Host") },
                singleLine = true,
                enabled = !isRunning,
                modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
                value = state.serverPort.toString(),
                onValueChange = onPortChange,
                label = { Text("Port") },
                singleLine = true,
                enabled = !isRunning,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- 2. Protocol toggle ----
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.protocol == "TCP",
                onClick = { onProtocolChange("TCP") },
                label = { Text("TCP") },
                enabled = !isRunning,
            )
            FilterChip(
                selected = state.protocol == "UDP",
                onClick = { onProtocolChange("UDP") },
                label = { Text("UDP") },
                enabled = !isRunning,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---- 3. Speed gauge ----
        SpeedGauge(
            currentSpeed = state.currentBandwidthMbps.toFloat(),
            maxSpeed = 1000f,
            isActive = isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ---- 4. START / STOP button ----
        FilledTonalButton(
            onClick = { if (isRunning) onStopTest() else onStartTest() },
            shape = CircleShape,
            modifier = Modifier.size(80.dp),
        ) {
            if (isRunning) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop test",
                    modifier = Modifier.size(36.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Start test",
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Text(
            text = if (isRunning) "STOP" else "START",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- 5. Progress indicator + elapsed time ----
        if (isRunning) {
            RunningSection(state)
        }

        // ---- Error message ----
        if (isError) {
            val errorMsg = (state.progress as TestProgress.Error).message
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp),
            )
        }

        // ---- 6. Result cards ----
        if (isComplete) {
            val result = (state.progress as TestProgress.Complete).result
            Spacer(modifier = Modifier.height(8.dp))

            ResultsSection(
                result = result,
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = {
                    val report = ReportGenerator.generateTextReport(result)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "iPerf3 Test Report - ${result.formatAvgBandwidth()}")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share report",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Report")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- 7. Advanced options ----
        TextButton(
            onClick = { advancedExpanded = !advancedExpanded },
            enabled = !isRunning,
        ) {
            Icon(
                imageVector = if (advancedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (advancedExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Advanced")
        }

        AnimatedVisibility(
            visible = advancedExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            AdvancedSection(
                duration = state.duration,
                streams = state.streams,
                reverse = state.reverse,
                bidirectional = state.bidirectional,
                onDurationChange = onDurationChange,
                onStreamsChange = onStreamsChange,
                onReverseChange = onReverseChange,
                onBidirectionalChange = onBidirectionalChange,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------- Running section ----------

@Composable
private fun RunningSection(state: TestUiState) {
    val progress = state.progress

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (progress) {
            is TestProgress.Connecting -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connecting to ${progress.serverHost}:${progress.serverPort}...",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            is TestProgress.Connected -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connected. Configuring test...",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            is TestProgress.Started -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Test started",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            is TestProgress.Interval -> {
                LinearProgressIndicator(
                    progress = { progress.estimatedProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))

                val elapsedSec = progress.elapsedTime / 1000.0
                Text(
                    text = "Elapsed: ${String.format("%.1f", elapsedSec)}s  |  ${progress.progressPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            else -> {
                // Idle, Complete, Error, Cancelled are not "running" but guard for safety
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ---------- Results section ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultsSection(
    result: com.iperf3.android.domain.model.TestResult,
) {
    val isUdp = result.isUdp

    Text(
        text = "Results",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2,
    ) {
        // Avg Bandwidth
        ResultCard(
            title = "Avg Bandwidth",
            value = formatBandwidthValue(result.avgBandwidth),
            unit = formatBandwidthUnit(result.avgBandwidth),
            icon = Icons.Filled.Speed,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f),
        )

        // Min Bandwidth
        ResultCard(
            title = "Min",
            value = formatBandwidthValue(result.minBandwidth),
            unit = formatBandwidthUnit(result.minBandwidth),
            icon = Icons.Filled.ArrowDownward,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f),
        )

        // Max Bandwidth
        ResultCard(
            title = "Max",
            value = formatBandwidthValue(result.maxBandwidth),
            unit = formatBandwidthUnit(result.maxBandwidth),
            icon = Icons.Filled.ArrowUpward,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f),
        )

        // Quality Score
        ResultCard(
            title = "Quality",
            value = String.format("%.0f", result.qualityScore),
            unit = "/ 100",
            icon = Icons.Filled.Star,
            color = qualityColor(result.qualityScore),
            modifier = Modifier.weight(1f),
        )

        // Data Transferred
        ResultCard(
            title = "Transferred",
            value = formatTransferredValue(result.totalBytes),
            unit = formatTransferredUnit(result.totalBytes),
            icon = Icons.Filled.DataUsage,
            color = Color(0xFF9C27B0),
            modifier = Modifier.weight(1f),
        )

        // Duration
        ResultCard(
            title = "Duration",
            value = String.format("%.1f", result.durationSeconds),
            unit = "sec",
            icon = Icons.Filled.Timer,
            color = Color(0xFF607D8B),
            modifier = Modifier.weight(1f),
        )

        // UDP-specific cards
        if (isUdp) {
            // Jitter
            ResultCard(
                title = "Jitter",
                value = result.jitter?.let { String.format("%.3f", it) } ?: "--",
                unit = "ms",
                icon = Icons.Filled.SwapVert,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f),
            )

            // Packet Loss
            ResultCard(
                title = "Packet Loss",
                value = result.packetLoss?.let { String.format("%.2f", it) } ?: "--",
                unit = "%",
                icon = Icons.Filled.Warning,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f),
            )
        }

        // TCP retransmits
        if (result.isTcp && result.retransmits != null) {
            ResultCard(
                title = "Retransmits",
                value = result.retransmits.toString(),
                unit = "pkts",
                icon = Icons.Filled.Compress,
                color = Color(0xFFFF5722),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ---------- Advanced section ----------

@Composable
private fun AdvancedSection(
    duration: Int,
    streams: Int,
    reverse: Boolean,
    bidirectional: Boolean,
    onDurationChange: (Int) -> Unit,
    onStreamsChange: (Int) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    onBidirectionalChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        // Duration slider
        Text(
            text = "Duration: ${duration}s",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = duration.toFloat(),
            onValueChange = { onDurationChange(it.toInt()) },
            valueRange = 1f..120f,
            steps = 0,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Streams slider
        Text(
            text = "Parallel Streams: $streams",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = streams.toFloat(),
            onValueChange = { onStreamsChange(it.toInt()) },
            valueRange = 1f..16f,
            steps = 14, // 16 - 1 - 1 = 14 intermediate steps for integers 2..15
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Reverse toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Reverse Mode (Download)", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = reverse, onCheckedChange = onReverseChange)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bidirectional toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Bidirectional", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = bidirectional, onCheckedChange = onBidirectionalChange)
        }
    }
}

// ---------- Formatting helpers ----------

private fun formatBandwidthValue(bps: Double): String {
    return when {
        bps >= 1_000_000_000 -> String.format("%.2f", bps / 1_000_000_000)
        bps >= 1_000_000 -> String.format("%.2f", bps / 1_000_000)
        bps >= 1_000 -> String.format("%.2f", bps / 1_000)
        else -> String.format("%.0f", bps)
    }
}

private fun formatBandwidthUnit(bps: Double): String {
    return when {
        bps >= 1_000_000_000 -> "Gbps"
        bps >= 1_000_000 -> "Mbps"
        bps >= 1_000 -> "Kbps"
        else -> "bps"
    }
}

private fun formatTransferredValue(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> String.format("%.2f", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024 * 1024 -> String.format("%.2f", bytes / (1024.0 * 1024))
        bytes >= 1024 -> String.format("%.2f", bytes / 1024.0)
        else -> bytes.toString()
    }
}

private fun formatTransferredUnit(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> "GB"
        bytes >= 1024 * 1024 -> "MB"
        bytes >= 1024 -> "KB"
        else -> "B"
    }
}

private fun qualityColor(score: Float): Color {
    return when {
        score >= 90f -> Color(0xFF4CAF50)
        score >= 75f -> Color(0xFF8BC34A)
        score >= 50f -> Color(0xFFFFC107)
        score >= 25f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}
