package com.example.ui.screens

import android.webkit.WebView
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalWhite

enum class ScriptLang {
    PYTHON,
    JAVASCRIPT,
    SHELL
}

@Composable
fun ScriptLabScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val scriptLogs by viewModel.scriptLogs.collectAsState()
    val isExecuting by viewModel.isScriptExecuting.collectAsState()

    var selectedLang by remember { mutableStateOf(ScriptLang.PYTHON) }
    var codeInput by remember {
        mutableStateOf(
            """
            # Mobile Croaker Pyodide Automation Script
            import js
            js.MobileCroaker.logMessage("INFO", "Pyodide Python script running on RAUSON node")
            
            # Direct tap via bridge
            js.MobileCroaker.tap(540, 960)
            """.trimIndent()
        )
    }

    // Hidden WebView hosting Pyodide
    Box(modifier = Modifier.size(1.dp).clip(RoundedCornerShape(0.dp))) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    viewModel.scriptEngine.attachWebView(this)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SAFE SCRIPTING LAB (PYODIDE + JS + SHELL)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Execute custom automations in sandboxed runtime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        when (selectedLang) {
                            ScriptLang.PYTHON -> viewModel.runPythonScript(codeInput)
                            ScriptLang.JAVASCRIPT -> viewModel.runJsScript(codeInput)
                            ScriptLang.SHELL -> viewModel.runShellScript(codeInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    modifier = Modifier.testTag("run_script_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run Script", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Language Selectors
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedLang == ScriptLang.PYTHON,
                    onClick = {
                        selectedLang = ScriptLang.PYTHON
                        codeInput = """
                            # Mobile Croaker Pyodide Automation Script
                            import js
                            js.MobileCroaker.logMessage("INFO", "Pyodide Python script running on RAUSON node")
                            js.MobileCroaker.tap(540, 960)
                        """.trimIndent()
                    },
                    label = { Text("Python (Pyodide Wasm)", fontSize = 11.sp) }
                )

                FilterChip(
                    selected = selectedLang == ScriptLang.JAVASCRIPT,
                    onClick = {
                        selectedLang = ScriptLang.JAVASCRIPT
                        codeInput = """
                            // Mobile Croaker JavaScript Script
                            window.MobileCroaker.logMessage("INFO", "JavaScript automation active");
                            window.MobileCroaker.tap(540, 960);
                        """.trimIndent()
                    },
                    label = { Text("JavaScript", fontSize = 11.sp) }
                )

                FilterChip(
                    selected = selectedLang == ScriptLang.SHELL,
                    onClick = {
                        selectedLang = ScriptLang.SHELL
                        codeInput = """
                            # Mobile Croaker Shell Script via ADB bridge
                            input tap 540 960
                            input keyevent 4
                        """.trimIndent()
                    },
                    label = { Text("Bash / Shell", fontSize = 11.sp) }
                )
            }
        }

        // Editor
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SCRIPT CODE (${selectedLang.name})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("script_editor_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TerminalWhite,
                            fontSize = 12.sp
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Console Output
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CONSOLE OUTPUT STREAM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                OutlinedButton(
                    onClick = { viewModel.clearScriptLogs() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 10.sp)
                }
            }
        }

        items(scriptLogs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF222222), RoundedCornerShape(6.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${log.level}]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (log.level == "ERROR") Color.Red else StatusBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.message,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalWhite
                    )
                }
            }
        }
    }
}
