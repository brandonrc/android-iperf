@file:OptIn(ExperimentalMaterial3Api::class)

package com.iperf3.android.presentation.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iperf3.android.presentation.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val defaultDuration by viewModel.defaultDuration.collectAsState()
    val defaultStreams by viewModel.defaultStreams.collectAsState()
    val defaultProtocol by viewModel.defaultProtocol.collectAsState()
    val defaultBandwidthLimit by viewModel.defaultBandwidthLimit.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val showNotifications by viewModel.showNotifications.collectAsState()
    val historyRetentionDays by viewModel.historyRetentionDays.collectAsState()
    val historyCount by viewModel.historyCount.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var portText by remember(serverPort) { mutableStateOf(serverPort.toString()) }
    var bandwidthText by remember(defaultBandwidthLimit) {
        mutableStateOf(
            if (defaultBandwidthLimit > 0) {
                (defaultBandwidthLimit / 1_000_000).toString()
            } else {
                ""
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ---- Test Defaults Section ----
        SectionHeader(title = "Test Defaults")

        // Default duration slider
        Text(
            text = "Default Duration: ${defaultDuration}s",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = defaultDuration.toFloat(),
            onValueChange = { viewModel.updateDefaultDuration(it.roundToInt()) },
            valueRange = 1f..60f,
            steps = 58,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Default streams slider
        Text(
            text = "Default Streams: $defaultStreams",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = defaultStreams.toFloat(),
            onValueChange = { viewModel.updateDefaultStreams(it.roundToInt()) },
            valueRange = 1f..16f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Default protocol segmented button
        Text(
            text = "Default Protocol",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val protocols = listOf("TCP", "UDP")
            protocols.forEachIndexed { index, protocol ->
                SegmentedButton(
                    selected = defaultProtocol == protocol,
                    onClick = { viewModel.updateDefaultProtocol(protocol) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = protocols.size
                    )
                ) {
                    Text(protocol)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bandwidth limit
        OutlinedTextField(
            value = bandwidthText,
            onValueChange = { value ->
                bandwidthText = value
                val mbps = value.toLongOrNull()
                if (mbps != null && mbps >= 0) {
                    viewModel.updateDefaultBandwidthLimit(mbps * 1_000_000)
                } else if (value.isEmpty()) {
                    viewModel.updateDefaultBandwidthLimit(0L)
                }
            },
            label = { Text("Bandwidth Limit (Mbps)") },
            placeholder = { Text("0 = unlimited") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Server Section ----
        SectionHeader(title = "Server")

        OutlinedTextField(
            value = portText,
            onValueChange = { value ->
                portText = value
                value.toIntOrNull()?.let { port ->
                    if (port in 1..65535) {
                        viewModel.updateServerPort(port)
                    }
                }
            },
            label = { Text("Default Server Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Appearance Section ----
        SectionHeader(title = "Appearance")

        SettingsSwitchRow(
            title = "Dark Mode",
            subtitle = "Use dark theme",
            checked = darkModeEnabled,
            onCheckedChange = { viewModel.updateDarkModeEnabled(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Notifications Section ----
        SectionHeader(title = "Notifications")

        SettingsSwitchRow(
            title = "Show Notifications",
            subtitle = "Display notifications during tests",
            checked = showNotifications,
            onCheckedChange = { viewModel.updateShowNotifications(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Data Section ----
        SectionHeader(title = "Data")

        // History retention dropdown
        RetentionDropdown(
            selectedDays = historyRetentionDays,
            onDaysSelected = { viewModel.updateHistoryRetentionDays(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Clear history button
        OutlinedButton(
            onClick = { showClearHistoryDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear History ($historyCount results)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---- About Section ----
        SectionHeader(title = "About")

        SettingsInfoRow(title = "App Version", value = "1.0.0")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsInfoRow(title = "Protocol", value = "Made with iperf3 protocol")

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Clear history confirmation dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History") },
            text = {
                Text("Are you sure you want to delete all $historyCount test results? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearHistory()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RetentionDropdown(
    selectedDays: Int,
    onDaysSelected: (Int) -> Unit
) {
    val options = listOf(7, 14, 30, 90)
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("History Retention: $selectedDays days")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { days ->
                DropdownMenuItem(
                    text = { Text("$days days") },
                    onClick = {
                        onDaysSelected(days)
                        expanded = false
                    }
                )
            }
        }
    }
}
