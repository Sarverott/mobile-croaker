package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.llm.LlmStepLog
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalWhite

@Composable
fun LlmAgentScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val config by viewModel.llmConfig.collectAsState()
    val isThinking by viewModel.isLlmThinking.collectAsState()
    val logs by viewModel.llmLogs.collectAsState()

    var showConfig by remember { mutableStateOf(false) }
    var goalInput by remember { mutableStateOf("Open settings, find Battery settings, and inspect battery usage.") }

    var urlInput by remember(config) { mutableStateOf(config.baseUrl) }
    var modelInput by remember(config) { mutableStateOf(config.modelName) }
    var keyInput by remember(config) { mutableStateOf(config.apiKey) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Model Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StatusBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "OPENAI-COMPATIBLE LLM AGENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusBlue,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "${config.modelName} • ${config.baseUrl.substringAfter("://").substringBefore("/")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = TerminalWhite
                                )
                            }
                        }

                        IconButton(onClick = { showConfig = !showConfig }) {
                            Icon(Icons.Default.Settings, contentDescription = "Config", tint = TerminalWhite)
                        }
                    }

                    if (showConfig) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Base URL (e.g. http://192.168.1.50:11434/v1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = { modelInput = it },
                            label = { Text("Model Name (e.g. llama3.2, qwen2.5-coder, gpt-4o-mini)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            label = { Text("API Key (Optional for local Ollama/vLLM)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.updateLlmConfig(
                                    config.copy(
                                        baseUrl = urlInput,
                                        modelName = modelInput,
                                        apiKey = keyInput
                                    )
                                )
                                showConfig = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalWhite),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save LLM Settings", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Autonomous Goal Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "AUTONOMOUS GOAL DIRECTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("llm_goal_input"),
                        placeholder = { Text("Enter natural language objective for the agent...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.runLlmGoal(goalInput) },
                        enabled = !isThinking && goalInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_llm_goal_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isThinking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reasoning Loop Active...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Engage Autonomous LLM Loop", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Reasoning Feed
        item {
            Text(
                text = "STEP-BY-STEP REASONING FEED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active reasoning trace. Enter a goal above to start the autonomous loop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(logs) { log ->
                LlmStepRow(log = log)
            }
        }
    }
}

@Composable
fun LlmStepRow(log: LlmStepLog) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Step ${log.step} • Action: ${log.actionTaken}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                )
                Text(
                    text = log.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Reasoning: ${log.reasoning}",
                style = MaterialTheme.typography.bodySmall,
                color = TerminalWhite
            )
        }
    }
}
