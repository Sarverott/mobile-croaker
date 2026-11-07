package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.AuditCapabilitiesScreen
import com.example.ui.screens.FleetAdbScreen
import com.example.ui.screens.HierarchyInspectorScreen
import com.example.ui.screens.HttpMcpScreen
import com.example.ui.screens.IdentityScreen
import com.example.ui.screens.IntegrationsScreen
import com.example.ui.screens.LlmAgentScreen
import com.example.ui.screens.ScriptLabScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TelemetryScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.screens.YamlScenarioScreen
import com.example.ui.theme.MobileCroakerTheme
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalWhite

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MobileCroakerTheme {
                val currentTab by viewModel.currentTab.collectAsState()
                val isServiceConnected by viewModel.isServiceConnected.collectAsState()
                val isAgentRunning by viewModel.isAgentRunning.collectAsState()
                val feedbackMessage by viewModel.userFeedbackMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(feedbackMessage) {
                    feedbackMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearFeedback()
                    }
                }

                BackHandler(enabled = currentTab != AppTab.WORKSPACE) {
                    viewModel.selectTab(AppTab.WORKSPACE)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isAgentRunning) TerminalWhite
                                                    else if (isServiceConnected) StatusGreen
                                                    else StatusRed
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Mobile Croaker",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TerminalWhite
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isAgentRunning) "RUNNING" else if (isServiceConnected) "ARMED" else "STANDBY",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAgentRunning) TerminalWhite else if (isServiceConnected) StatusGreen else StatusRed
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )

                            // Quick Sub-Module Horizontal Scroll Ribbon
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AppTab.values().forEach { tab ->
                                    val isSelected = currentTab == tab
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectTab(tab) },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else TerminalWhite
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TerminalWhite,
                                            containerColor = TerminalSurface
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = TerminalBorder,
                                            selectedBorderColor = TerminalWhite,
                                            enabled = true,
                                            selected = isSelected
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    },
                    bottomBar = {
                        // Standard Primary NavigationBar for core 5 destinations
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.WORKSPACE,
                                onClick = { viewModel.selectTab(AppTab.WORKSPACE) },
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Agent") },
                                label = { Text("Agent", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalWhite,
                                    selectedTextColor = TerminalWhite,
                                    indicatorColor = TerminalWhite.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_workspace")
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.YAML_SCENARIO,
                                onClick = { viewModel.selectTab(AppTab.YAML_SCENARIO) },
                                icon = { Icon(Icons.Default.Code, contentDescription = "Scenarios") },
                                label = { Text("YAML", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalWhite,
                                    selectedTextColor = TerminalWhite,
                                    indicatorColor = TerminalWhite.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_yaml")
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.LLM_AGENT,
                                onClick = { viewModel.selectTab(AppTab.LLM_AGENT) },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "LLM") },
                                label = { Text("LLM Goal", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalWhite,
                                    selectedTextColor = TerminalWhite,
                                    indicatorColor = TerminalWhite.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_llm")
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.SCRIPT_LAB,
                                onClick = { viewModel.selectTab(AppTab.SCRIPT_LAB) },
                                icon = { Icon(Icons.Default.Terminal, contentDescription = "Script Lab") },
                                label = { Text("Pyodide", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalWhite,
                                    selectedTextColor = TerminalWhite,
                                    indicatorColor = TerminalWhite.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_script")
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.HTTP_MCP,
                                onClick = { viewModel.selectTab(AppTab.HTTP_MCP) },
                                icon = { Icon(Icons.Default.Http, contentDescription = "Server & MCP") },
                                label = { Text("MCP/REST", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalWhite,
                                    selectedTextColor = TerminalWhite,
                                    indicatorColor = TerminalWhite.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_mcp")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            AppTab.WORKSPACE -> WorkspaceScreen(viewModel = viewModel)
                            AppTab.YAML_SCENARIO -> YamlScenarioScreen(viewModel = viewModel)
                            AppTab.LLM_AGENT -> LlmAgentScreen(viewModel = viewModel)
                            AppTab.SCRIPT_LAB -> ScriptLabScreen(viewModel = viewModel)
                            AppTab.HTTP_MCP -> HttpMcpScreen(viewModel = viewModel)
                            AppTab.FLEET_ADB -> FleetAdbScreen(viewModel = viewModel)
                            AppTab.TELEMETRY -> TelemetryScreen(viewModel = viewModel)
                            AppTab.IDENTITY -> IdentityScreen(viewModel = viewModel)
                            AppTab.HARDWARE -> IntegrationsScreen()
                            AppTab.SETTINGS -> SettingsScreen()
                            AppTab.DIAGNOSTICS -> AuditCapabilitiesScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
