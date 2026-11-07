package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.ui.screens.TelemetryScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.OpenClawTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OpenClawTheme {
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
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isAgentRunning) CyberCyan
                                                else if (isServiceConnected) NeonEmerald
                                                else CriticalRed
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "OpenClaw Agent",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isAgentRunning) "RUNNING" else if (isServiceConnected) "ARMED" else "STANDBY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAgentRunning) CyberCyan else if (isServiceConnected) NeonEmerald else CriticalRed
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.WORKSPACE,
                                onClick = { viewModel.selectTab(AppTab.WORKSPACE) },
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Workspace") },
                                label = { Text("Agent", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_workspace")
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.INSPECTOR,
                                onClick = { viewModel.selectTab(AppTab.INSPECTOR) },
                                icon = { Icon(Icons.Default.AccountTree, contentDescription = "Inspector") },
                                label = { Text("Inspect", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_inspector")
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.FLEET_ADB,
                                onClick = { viewModel.selectTab(AppTab.FLEET_ADB) },
                                icon = { Icon(Icons.Default.Devices, contentDescription = "Fleet & ADB") },
                                label = { Text("Fleet ADB", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_fleet")
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.TELEMETRY,
                                onClick = { viewModel.selectTab(AppTab.TELEMETRY) },
                                icon = { Icon(Icons.Default.Security, contentDescription = "Rsyslog TLS") },
                                label = { Text("Rsyslog", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_telemetry")
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.DIAGNOSTICS,
                                onClick = { viewModel.selectTab(AppTab.DIAGNOSTICS) },
                                icon = { Icon(Icons.Default.Info, contentDescription = "OS Audit") },
                                label = { Text("OS Audit", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_diagnostics")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            AppTab.WORKSPACE -> WorkspaceScreen(viewModel = viewModel)
                            AppTab.INSPECTOR -> HierarchyInspectorScreen(viewModel = viewModel)
                            AppTab.FLEET_ADB -> FleetAdbScreen(viewModel = viewModel)
                            AppTab.TELEMETRY -> TelemetryScreen(viewModel = viewModel)
                            AppTab.DIAGNOSTICS -> AuditCapabilitiesScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
