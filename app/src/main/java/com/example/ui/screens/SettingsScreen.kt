package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalMuted
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalWhite
import com.example.util.AppPreferences

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }

    val savedPort by prefs.serverPort.collectAsState()
    val savedLlmUrl by prefs.llmEndpoint.collectAsState()
    val savedLlmModel by prefs.llmModel.collectAsState()
    val savedLlmTemp by prefs.llmTemperature.collectAsState()
    val savedRsyslogHost by prefs.rsyslogHost.collectAsState()
    val savedRsyslogPort by prefs.rsyslogPort.collectAsState()
    val savedFastClipboard by prefs.fastClipboardMode.collectAsState()
    val savedTier by prefs.preferredFallbackTier.collectAsState()
    val savedCallsign by prefs.nodeCallsign.collectAsState()
    val savedOperator by prefs.operatorName.collectAsState()
    val savedLanguage by prefs.language.collectAsState()

    var portText by remember(savedPort) { mutableStateOf(savedPort.toString()) }
    var llmUrlText by remember(savedLlmUrl) { mutableStateOf(savedLlmUrl) }
    var llmModelText by remember(savedLlmModel) { mutableStateOf(savedLlmModel) }
    var llmTempSlider by remember(savedLlmTemp) { mutableFloatStateOf(savedLlmTemp) }
    var rsyslogHostText by remember(savedRsyslogHost) { mutableStateOf(savedRsyslogHost) }
    var rsyslogPortText by remember(savedRsyslogPort) { mutableStateOf(savedRsyslogPort.toString()) }
    var fastClipboardToggle by remember(savedFastClipboard) { mutableStateOf(savedFastClipboard) }
    var selectedTier by remember(savedTier) { mutableStateOf(savedTier) }
    var callsignText by remember(savedCallsign) { mutableStateOf(savedCallsign) }
    var operatorText by remember(savedOperator) { mutableStateOf(savedOperator) }
    var selectedLang by remember(savedLanguage) { mutableStateOf(savedLanguage) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalWhite
                    )
                    Text(
                        text = "Customize autonomous ports, LLM server URLs, telemetry targets, and system behaviors.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TerminalMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 1. Gateway & Network
        item {
            SettingsCard(title = "GATEWAY & NETWORK TELEMETRY") {
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text(stringResource(R.string.settings_server_port), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = terminalTextFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = rsyslogHostText,
                        onValueChange = { rsyslogHostText = it },
                        label = { Text(stringResource(R.string.settings_rsyslog_host), fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(2f),
                        colors = terminalTextFieldColors()
                    )
                    OutlinedTextField(
                        value = rsyslogPortText,
                        onValueChange = { rsyslogPortText = it },
                        label = { Text("Port", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(1f),
                        colors = terminalTextFieldColors()
                    )
                }
            }
        }

        // 2. LLM Reasoning Agent
        item {
            SettingsCard(title = "AUTONOMOUS LLM REASONING") {
                OutlinedTextField(
                    value = llmUrlText,
                    onValueChange = { llmUrlText = it },
                    label = { Text(stringResource(R.string.settings_llm_url), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = terminalTextFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = llmModelText,
                    onValueChange = { llmModelText = it },
                    label = { Text(stringResource(R.string.settings_llm_model), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = terminalTextFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.settings_llm_temp)}: ${String.format("%.2f", llmTempSlider)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TerminalWhite
                    )
                }
                Slider(
                    value = llmTempSlider,
                    onValueChange = { llmTempSlider = it },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = StatusGreen,
                        activeTrackColor = StatusGreen,
                        inactiveTrackColor = TerminalBorder
                    )
                )
            }
        }

        // 3. Fallback & Automation Preferences
        item {
            SettingsCard(title = "AUTOMATION & ACCESSIBILITY") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_fast_clipboard),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TerminalWhite
                        )
                        Text(
                            text = "Copies text to clipboard and pastes instantly instead of character typing.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TerminalMuted
                        )
                    }
                    Switch(
                        checked = fastClipboardToggle,
                        onCheckedChange = { fastClipboardToggle = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = StatusGreen,
                            uncheckedThumbColor = TerminalMuted,
                            uncheckedTrackColor = TerminalBorder
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = selectedTier,
                    onValueChange = { selectedTier = it },
                    label = { Text(stringResource(R.string.settings_fallback_tier), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = terminalTextFieldColors()
                )
            }
        }

        // 4. Identity & Locale
        item {
            SettingsCard(title = "NODE IDENTITY & LOCALIZATION") {
                OutlinedTextField(
                    value = callsignText,
                    onValueChange = { callsignText = it },
                    label = { Text(stringResource(R.string.settings_node_callsign), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = terminalTextFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = operatorText,
                    onValueChange = { operatorText = it },
                    label = { Text(stringResource(R.string.settings_operator_name), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = terminalTextFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = selectedLang,
                    onValueChange = { selectedLang = it },
                    label = { Text(stringResource(R.string.settings_language), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = terminalTextFieldColors()
                )
            }
        }

        // Save & Reset Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val port = portText.toIntOrNull() ?: 8080
                        val rPort = rsyslogPortText.toIntOrNull() ?: 514
                        prefs.updateSettings(
                            port = port,
                            llmUrl = llmUrlText,
                            model = llmModelText,
                            temp = llmTempSlider,
                            rsyslogHost = rsyslogHostText,
                            rsyslogPort = rPort,
                            fastClipboard = fastClipboardToggle,
                            tier = selectedTier,
                            callsign = callsignText,
                            operator = operatorText,
                            lang = selectedLang
                        )
                        Toast.makeText(context, "Settings Saved Successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                OutlinedButton(
                    onClick = {
                        prefs.resetToDefaults()
                        Toast.makeText(context, "Reset to Factory Defaults", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                ) {
                    Text(
                        text = stringResource(R.string.action_reset),
                        fontFamily = FontFamily.Monospace,
                        color = TerminalWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun terminalTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StatusGreen,
    unfocusedBorderColor = TerminalBorder,
    focusedLabelColor = StatusGreen,
    unfocusedLabelColor = TerminalMuted,
    focusedTextColor = TerminalWhite,
    unfocusedTextColor = TerminalWhite,
    cursorColor = StatusGreen
)
