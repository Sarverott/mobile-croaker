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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalWhite

@Composable
fun YamlScenarioScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val clipboardManager = LocalClipboardManager.current
    val activeWorkflow by viewModel.activeWorkflow.collectAsState()

    var yamlText by remember {
        mutableStateOf(
            """
            # Mobile Croaker / RAUSON Scenario
            version: "1.0"
            id: "rauson-battery-audit"
            title: "Settings Battery Audit"
            description: "Launches settings and audits device battery state"
            target_package: "com.android.settings"
            steps:
              - name: "Launch Settings"
                action: "LAUNCH_APP"
                delay_ms: 1200
              - name: "Find Battery Entry"
                action: "CLICK_NODE"
                target_text: "Battery"
                coords: [540, 380]
                delay_ms: 800
              - name: "Scroll Down Telemetry"
                action: "SWIPE"
                coords: [540, 1400]
                swipe_end: [540, 400]
                delay_ms: 1000
              - name: "Return to Workspace"
                action: "GLOBAL_ACTION"
                global_key: "BACK"
                delay_ms: 500
            """.trimIndent()
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CODABLE YAML SCENARIO MANAGER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Import, export, and edit automation scenarios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            yamlText = viewModel.exportCurrentWorkflowToYaml()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.importYamlScenario(yamlText)
                            viewModel.startAgent()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("run_yaml_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compile & Run", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Quick Template Selectors
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        yamlText = """
                            # Template: Quick Smoke Navigation
                            version: "1.0"
                            id: "smoke-nav"
                            title: "Smoke Navigation"
                            target_package: "com.android.settings"
                            steps:
                              - name: "Launch App"
                                action: "LAUNCH_APP"
                                delay_ms: 1000
                              - name: "Tap Center"
                                action: "COORDINATE_TAP"
                                coords: [540, 960]
                              - name: "Back Key"
                                action: "GLOBAL_ACTION"
                                global_key: "BACK"
                        """.trimIndent()
                    },
                    label = { Text("Smoke Template", fontSize = 11.sp) }
                )

                FilterChip(
                    selected = false,
                    onClick = {
                        yamlText = """
                            # Template: ADB Shell Command Flow
                            version: "1.0"
                            id: "adb-flow"
                            title: "Fleet ADB Diagnostic"
                            target_package: "com.example"
                            steps:
                              - name: "Check Battery Dumpsys"
                                action: "ADB_SHELL_COMMAND"
                                payload: "dumpsys battery | grep level"
                              - name: "Wakeup Screen"
                                action: "ADB_SHELL_COMMAND"
                                payload: "input keyevent 224"
                        """.trimIndent()
                    },
                    label = { Text("ADB Flow", fontSize = 11.sp) }
                )

                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(yamlText))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp)
                }
            }
        }

        // Editor Area
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCENARIO CODE (YAML / JSON)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Button(
                            onClick = { viewModel.importYamlScenario(yamlText) },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalWhite),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Only", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = yamlText,
                        onValueChange = { yamlText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .testTag("yaml_editor_field"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TerminalWhite,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}
