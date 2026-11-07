package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.MobileCroakerAccessibilityService
import com.example.agent.AgentActionEngine
import com.example.agent.YamlScenarioManager
import com.example.bridge.AdbBridgeClient
import com.example.bridge.AdbCommandResult
import com.example.identity.IdentityFormat
import com.example.identity.PhoneIdentity
import com.example.identity.PhoneIdentityManager
import com.example.llm.LlmAgentConfig
import com.example.llm.LlmStepLog
import com.example.llm.OpenAiLlmAgent
import com.example.model.ActionResult
import com.example.model.AgentWorkflow
import com.example.model.RemoteDevice
import com.example.model.SyslogEntry
import com.example.model.UiNode
import com.example.scripting.PyodideScriptEngine
import com.example.scripting.ScriptOutput
import com.example.server.MobileCroakerHttpServer
import com.example.telemetry.RsyslogClient
import com.example.telemetry.RsyslogConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    WORKSPACE("Agent"),
    YAML_SCENARIO("Scenarios"),
    LLM_AGENT("LLM Goal"),
    SCRIPT_LAB("Script Lab"),
    HTTP_MCP("Server & MCP"),
    FLEET_ADB("Fleet ADB"),
    TELEMETRY("Rsyslog"),
    IDENTITY("Identity"),
    DIAGNOSTICS("Audit & Info")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val agentEngine = AgentActionEngine.getInstance(application)
    private val rsyslogClient = RsyslogClient.instance
    private val adbBridge = AdbBridgeClient.instance
    private val httpServer = MobileCroakerHttpServer.getInstance(application)
    private val llmAgent = OpenAiLlmAgent.getInstance(application)
    val scriptEngine = PyodideScriptEngine(application, viewModelScope)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(AppTab.WORKSPACE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Accessibility state
    val isServiceConnected: StateFlow<Boolean> = MobileCroakerAccessibilityService.isServiceConnected
    val foregroundPackage: StateFlow<String> = MobileCroakerAccessibilityService.currentForegroundPackage
    val activityClass: StateFlow<String> = MobileCroakerAccessibilityService.currentActivityClass
    val isSystemAlertDetected: StateFlow<Boolean> = MobileCroakerAccessibilityService.isSystemAlertDetected
    val lastAlertMessage: StateFlow<String?> = MobileCroakerAccessibilityService.lastAlertMessage
    val eventCount: StateFlow<Long> = MobileCroakerAccessibilityService.eventCounter

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

    // HTTP & MCP Server state
    val isHttpServerRunning: StateFlow<Boolean> = httpServer.isRunning
    val httpServerUrl: StateFlow<String> = httpServer.serverUrl
    val httpRequestCount: StateFlow<Long> = httpServer.requestCount

    // LLM Agent state
    val llmConfig: StateFlow<LlmAgentConfig> = llmAgent.config
    val isLlmThinking: StateFlow<Boolean> = llmAgent.isAgentThinking
    val llmLogs: StateFlow<List<LlmStepLog>> = llmAgent.executionLogs

    // Phone Identity state
    private val _phoneIdentity = MutableStateFlow(PhoneIdentity())
    val phoneIdentity: StateFlow<PhoneIdentity> = _phoneIdentity.asStateFlow()

    // Scripting state
    val scriptLogs: StateFlow<List<ScriptOutput>> = scriptEngine.consoleLogs
    val isScriptExecuting: StateFlow<Boolean> = scriptEngine.isExecuting

    // Hierarchy Inspector state
    private val _currentHierarchy = MutableStateFlow<UiNode?>(null)
    val currentHierarchy: StateFlow<UiNode?> = _currentHierarchy.asStateFlow()

    private val _nodeSearchQuery = MutableStateFlow("")
    val nodeSearchQuery: StateFlow<String> = _nodeSearchQuery.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        if (workflowLibrary.value.isNotEmpty()) {
            agentEngine.selectWorkflow(workflowLibrary.value[0])
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _userFeedbackMessage.value = "Unable to open Settings: ${e.message}"
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
        val success = MobileCroakerAccessibilityService.instance?.autoResolveSystemDialog() ?: false
        _userFeedbackMessage.value = if (success) "System dialog resolved" else "No matching dismiss button found"
    }

    // YAML Scenario Management
    fun importYamlScenario(yamlContent: String) {
        val parsed = YamlScenarioManager.fromYaml(yamlContent)
        if (parsed.isSuccess) {
            val wf = parsed.getOrThrow()
            agentEngine.selectWorkflow(wf)
            _userFeedbackMessage.value = "Imported YAML scenario: ${wf.title}"
        } else {
            _userFeedbackMessage.value = "YAML Parse error: ${parsed.exceptionOrNull()?.message}"
        }
    }

    fun exportCurrentWorkflowToYaml(): String {
        val wf = activeWorkflow.value ?: return "# No workflow selected"
        return YamlScenarioManager.toYaml(wf)
    }

    // HTTP Server controls
    fun toggleHttpServer(port: Int = 8080) {
        if (isHttpServerRunning.value) {
            httpServer.stopServer()
            _userFeedbackMessage.value = "HTTP & MCP Server stopped."
        } else {
            httpServer.startServer(port)
            _userFeedbackMessage.value = "Server active on port $port"
        }
    }

    // LLM controls
    fun updateLlmConfig(config: LlmAgentConfig) {
        llmAgent.updateConfig(config)
        _userFeedbackMessage.value = "Updated LLM configuration for ${config.modelName}"
    }

    fun runLlmGoal(goal: String) {
        viewModelScope.launch {
            _userFeedbackMessage.value = "Starting LLM Goal: $goal"
            val success = llmAgent.executeGoal(goal) { progress ->
                _userFeedbackMessage.value = progress
            }
            _userFeedbackMessage.value = if (success) "LLM goal execution finished successfully!" else "LLM goal aborted."
        }
    }

    // Identity export / import
    fun updateIdentity(identity: PhoneIdentity) {
        _phoneIdentity.value = identity
        _userFeedbackMessage.value = "Identity profile updated: ${identity.callsign}"
    }

    fun exportIdentity(format: IdentityFormat): String {
        return PhoneIdentityManager.export(_phoneIdentity.value, format)
    }

    fun importIdentity(content: String, format: IdentityFormat) {
        val result = PhoneIdentityManager.import(content, format)
        if (result.isSuccess) {
            _phoneIdentity.value = result.getOrThrow()
            _userFeedbackMessage.value = "Identity imported (${format.name}): ${_phoneIdentity.value.callsign}"
        } else {
            _userFeedbackMessage.value = "Failed to import identity: ${result.exceptionOrNull()?.message}"
        }
    }

    // Scripting controls
    fun runPythonScript(code: String) {
        scriptEngine.executePython(code)
    }

    fun runJsScript(code: String) {
        scriptEngine.executeJavaScript(code)
    }

    fun runShellScript(code: String) {
        scriptEngine.executeShell(code)
    }

    fun clearScriptLogs() {
        scriptEngine.clearLogs()
    }

    // Telemetry & ADB controls
    fun updateRsyslogConfig(config: RsyslogConfig) {
        rsyslogClient.updateConfig(config)
    }

    fun testRsyslogConnection() {
        rsyslogClient.testConnection { _, message ->
            _userFeedbackMessage.value = message
        }
    }

    fun clearSyslog() {
        rsyslogClient.clearLogs()
    }

    fun addRemoteDevice(name: String, ip: String, port: Int) {
        adbBridge.addDevice(name, ip, port)
        _userFeedbackMessage.value = "Added worker: $name ($ip:$port)"
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
            _userFeedbackMessage.value = "APK Deployment: $successCount / ${results.size} OK"
        }
    }

    fun setNodeSearchQuery(query: String) {
        _nodeSearchQuery.value = query
    }

    fun refreshHierarchy() {
        val service = MobileCroakerAccessibilityService.instance
        if (service != null) {
            _currentHierarchy.value = service.dumpCurrentHierarchy()
            _userFeedbackMessage.value = "Node hierarchy refreshed"
        } else {
            _currentHierarchy.value = UiNode(
                id = "root-demo",
                viewIdResourceName = "android:id/content",
                text = "Mobile Croaker Active Workspace",
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
                    )
                )
            )
        }
    }

    fun testTapNode(node: UiNode) {
        val service = MobileCroakerAccessibilityService.instance
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

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }
}
