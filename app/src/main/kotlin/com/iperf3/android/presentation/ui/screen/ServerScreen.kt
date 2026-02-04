@file:OptIn(ExperimentalMaterial3Api::class)

package com.iperf3.android.presentation.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iperf3.android.presentation.ui.theme.StatusColors
import com.iperf3.android.presentation.viewmodel.ServerViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ServerScreen(viewModel: ServerViewModel = hiltViewModel()) {
    val serverState by viewModel.serverState.collectAsState()
    var portText by remember(serverState.port) { mutableStateOf(serverState.port.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Port number TextField
        OutlinedTextField(
            value = portText,
            onValueChange = { value ->
                portText = value
                value.toIntOrNull()?.let { port ->
                    if (port in 1..65535) {
                        viewModel.updatePort(port)
                    }
                }
            },
            label = { Text("Server Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = !serverState.isRunning,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Start/Stop toggle button
        if (serverState.isRunning) {
            Button(
                onClick = { viewModel.stopServer() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusColors.Running
                ),
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Server",
                        modifier = Modifier.size(36.dp)
                    )
                    Text("Stop", style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            OutlinedButton(
                onClick = { viewModel.startServer() },
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Server",
                        modifier = Modifier.size(36.dp)
                    )
                    Text("Start", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status card
        ServerStatusCard(
            isRunning = serverState.isRunning,
            port = serverState.port,
            startTime = serverState.status.startTime,
            activeConnections = serverState.connections.size,
            errorMessage = serverState.status.errorMessage
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connection log header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connection Log",
                style = MaterialTheme.typography.titleMedium
            )
            if (serverState.log.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearLog() }) {
                    Text("Clear Log")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Connection log list
        val listState = rememberLazyListState()

        LaunchedEffect(serverState.log.size) {
            if (serverState.log.isNotEmpty()) {
                listState.animateScrollToItem(serverState.log.size - 1)
            }
        }

        if (serverState.log.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No log entries yet.\nStart the server to begin listening.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(serverState.log) { entry ->
                    ServerLogEntry(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun ServerStatusCard(
    isRunning: Boolean,
    port: Int,
    startTime: Long?,
    activeConnections: Int,
    errorMessage: String?
) {
    var uptimeText by remember { mutableStateOf("--") }

    LaunchedEffect(isRunning, startTime) {
        if (isRunning && startTime != null) {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / (1000 * 60)) % 60
                val hours = elapsed / (1000 * 60 * 60)
                uptimeText = when {
                    hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
                    minutes > 0 -> String.format("%dm %ds", minutes, seconds)
                    else -> String.format("%ds", seconds)
                }
                delay(1000L)
            }
        } else {
            uptimeText = "--"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status row with pulsing dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot(isRunning = isRunning)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Running" else if (errorMessage != null) "Error" else "Stopped",
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isRunning -> StatusColors.Running
                        errorMessage != null -> StatusColors.Error
                        else -> StatusColors.Stopped
                    }
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem(label = "Port", value = port.toString())
                StatusItem(label = "Uptime", value = uptimeText)
                StatusItem(label = "Connections", value = activeConnections.toString())
            }
        }
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PulsingDot(isRunning: Boolean) {
    val color by animateColorAsState(
        targetValue = if (isRunning) StatusColors.Running else StatusColors.Stopped,
        label = "dotColor"
    )

    if (isRunning) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulsingDot")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulsingAlpha"
        )

        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(color)
        )
    } else {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun ServerLogEntry(entry: String) {
    val icon = when {
        "connected" in entry.lowercase() && "disconnected" !in entry.lowercase() -> ">"
        "disconnected" in entry.lowercase() -> "<"
        "complete" in entry.lowercase() -> "*"
        "error" in entry.lowercase() -> "!"
        "starting" in entry.lowercase() || "ready" in entry.lowercase() -> "#"
        "stopped" in entry.lowercase() -> "-"
        else -> " "
    }

    val textColor = when {
        "error" in entry.lowercase() -> MaterialTheme.colorScheme.error
        "complete" in entry.lowercase() -> StatusColors.Running
        "stopped" in entry.lowercase() -> StatusColors.Stopped
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = textColor,
                modifier = Modifier.width(16.dp)
            )
            Text(
                text = entry,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = textColor
            )
        }
    }
}
