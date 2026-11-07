package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.OpenClawAccessibilityService
import com.example.agent.AgentActionEngine
import com.example.bridge.AdbBridgeClient
import com.example.bridge.AdbCommandResult
import com.example.model.ActionResult
import com.example.model.AgentWorkflow
import com.example.model.RemoteDevice
import com.example.model.SyslogEntry
import com.example.model.UiNode
import com.example.telemetry.RsyslogClient
import com.example.telemetry.RsyslogConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    WORKSPACE("Workspace"),
    INSPECTOR("Inspector"),
    FLEET_ADB("Fleet & ADB"),
    TELEMETRY("Rsyslog TLS"),
    DIAGNOSTICS("OS Audit")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val agentEngine = AgentActionEngine.getInstance(application)
    private val rsyslogClient = RsyslogClient.instance
    private val adbBridge = AdbBridgeClient.instance

    // Current navigation tab
    private val _currentTab = MutableStateFlow(AppTab.WORKSPACE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Accessibility state
    val isServiceConnected: StateFlow<Boolean> = OpenClawAccessibilityService.isServiceConnected
    val foregroundPackage: StateFlow<String> = OpenClawAccessibilityService.currentForegroundPackage
    val activityClass: StateFlow<String> = OpenClawAccessibilityService.currentActivityClass
    val isSystemAlertDetected: StateFlow<Boolean> = OpenClawAccessibilityService.isSystemAlertDetected
    val lastAlertMessage: StateFlow<String?> = OpenClawAccessibilityService.lastAlertMessage
    val eventCount: StateFlow<Long> = OpenClawAccessibilityService.eventCounter

    // Agent Engine state
    val activeWorkflow: StateFlow<AgentWorkflow?> = agentEngine.activeWorkflow
    val isAgentRunning: StateFlow<Boolean> = agentEngine.isRunning
    val currentStepIndex: StateFlow<Int> = agentEngine.currentStepIndex
    val actionHistory: StateFlow<List<ActionResult>> = agentEngine.actionHistory
    val workflowLibrary: StateFlow<List<AgentWorkflow>> = agentEngine.workflowLibrary

    // Telemetry state
    val syslogLogs: StateFlow<List<SyslogEntry>> = rsyslogClient.logs
    val syslogConfig: StateFlow<RsyslogConfig> = rsyslogClient.config
    val syslogConnectionStatus: StateFlow<String> = rsyslogClient.connectionStatus
    val isSyslogTransmitting: StateFlow<Boolean> = rsyslogClient.isTransmitting

    // ADB Fleet state
    val fleet: StateFlow<List<RemoteDevice>> = adbBridge.fleet
    val selectedDeviceId: StateFlow<String> = adbBridge.selectedDeviceId
    val adbHistory: StateFlow<List<AdbCommandResult>> = adbBridge.commandHistory

    // Hierarchy Inspector state
    private val _currentHierarchy = MutableStateFlow<UiNode?>(null)
    val currentHierarchy: StateFlow<UiNode?> = _currentHierarchy.asStateFlow()

    private val _nodeSearchQuery = MutableStateFlow("")
    val nodeSearchQuery: StateFlow<String> = _nodeSearchQuery.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        // Auto-select first workflow
        if (workflowLibrary.value.isNotEmpty()) {
            agentEngine.selectWorkflow(workflowLibrary.value[0])
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (tab == AppTab.INSPECTOR) {
            refreshHierarchy()
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _userFeedbackMessage.value = "Unable to open Accessibility Settings: ${e.message}"
        }
    }

    fun selectWorkflow(workflow: AgentWorkflow) {
        agentEngine.selectWorkflow(workflow)
    }

    fun startAgent() {
        agentEngine.startExecution()
    }

    fun stopAgent() {
        agentEngine.stopExecution()
    }

    fun autoResolveSystemDialog() {
        val success = OpenClawAccessibilityService.instance?.autoResolveSystemDialog() ?: false
        _userFeedbackMessage.value = if (success) "System dialog resolved successfully" else "No matching dismiss button found"
    }

    fun refreshHierarchy() {
        val service = OpenClawAccessibilityService.instance
        if (service != null) {
            _currentHierarchy.value = service.dumpCurrentHierarchy()
            _userFeedbackMessage.value = "Node hierarchy refreshed"
        } else {
            // Emulate placeholder root node when service is waiting for user activation
            _currentHierarchy.value = UiNode(
                id = "root-demo",
                viewIdResourceName = "android:id/content",
                text = "OpenClaw Active Workspace",
                contentDescription = "Autonomous workspace container",
                className = "android.widget.FrameLayout",
                packageName = foregroundPackage.value,
                bounds = android.graphics.Rect(0, 0, 1080, 2400),
                isClickable = false,
                isEditable = false,
                isScrollable = false,
                isVisibleToUser = true,
                isEnabled = true,
                children = listOf(
                    UiNode(
                        id = "node-1",
                        viewIdResourceName = "com.example:id/start_agent_btn",
                        text = "Start Autonomous Task",
                        contentDescription = "Primary action trigger",
                        className = "android.widget.Button",
                        packageName = "com.example",
                        bounds = android.graphics.Rect(120, 400, 960, 560),
                        isClickable = true,
                        isEditable = false,
                        isScrollable = false,
                        isVisibleToUser = true,
                        isEnabled = true
                    ),
                    UiNode(
                        id = "node-2",
                        viewIdResourceName = "com.example:id/search_input",
                        text = "Search external app elements",
                        contentDescription = "Query input bar",
                        className = "android.widget.EditText",
                        packageName = "com.example",
                        bounds = android.graphics.Rect(120, 600, 960, 740),
                        isClickable = true,
                        isEditable = true,
                        isScrollable = false,
                        isVisibleToUser = true,
                        isEnabled = true
                    )
                )
            )
        }
    }

    fun setNodeSearchQuery(query: String) {
        _nodeSearchQuery.value = query
    }

    fun testTapNode(node: UiNode) {
        val service = OpenClawAccessibilityService.instance
        if (service != null) {
            val clicked = service.clickNode(text = node.text, viewId = node.viewIdResourceName)
            _userFeedbackMessage.value = if (clicked) "Node tapped via Tier 1" else "Node tap failed, tried Tier 2"
        } else {
            viewModelScope.launch {
                val res = adbBridge.fallbackTap(node.centerX, node.centerY)
                _userFeedbackMessage.value = "Simulated ADB tap at (${node.centerX.toInt()}, ${node.centerY.toInt()}): ${res.output}"
            }
        }
    }

    fun updateRsyslogConfig(config: RsyslogConfig) {
        rsyslogClient.updateConfig(config)
    }

    fun testRsyslogConnection() {
        rsyslogClient.testConnection { success, message ->
            _userFeedbackMessage.value = message
        }
    }

    fun clearSyslog() {
        rsyslogClient.clearLogs()
    }

    fun addRemoteDevice(name: String, ip: String, port: Int) {
        adbBridge.addDevice(name, ip, port)
        _userFeedbackMessage.value = "Added device: $name ($ip:$port)"
    }

    fun pingDevice(id: String) {
        adbBridge.pingDevice(id)
    }

    fun removeDevice(id: String) {
        adbBridge.removeDevice(id)
    }

    fun selectDevice(id: String) {
        adbBridge.selectDevice(id)
    }

    fun executeAdbShell(command: String) {
        viewModelScope.launch {
            val res = adbBridge.executeCommand(command)
            _userFeedbackMessage.value = "Shell: [${res.command}] exit=${res.exitCode}"
        }
    }

    fun deployApkToFleet(apkPath: String, targetDeviceIds: List<String>) {
        viewModelScope.launch {
            _userFeedbackMessage.value = "Deploying APK to ${targetDeviceIds.size} device(s)..."
            val results = adbBridge.deployApkToFleet(apkPath, targetDeviceIds)
            val successCount = results.count { it.isSuccess }
            _userFeedbackMessage.value = "APK Deployment complete: $successCount / ${results.size} successful."
        }
    }

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }
}
