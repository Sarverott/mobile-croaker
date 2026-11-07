package com.example.model

import android.graphics.Rect

/**
 * Clean snapshot of an on-screen UI node retrieved via AccessibilityNodeInfo
 */
data class UiNode(
    val id: String,
    val viewIdResourceName: String?,
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val packageName: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isVisibleToUser: Boolean,
    val isEnabled: Boolean,
    val children: List<UiNode> = emptyList()
) {
    val centerX: Float get() = bounds.exactCenterX()
    val centerY: Float get() = bounds.exactCenterY()
    val displayLabel: String
        get() = text?.takeIf { it.isNotBlank() }
            ?: contentDescription?.takeIf { it.isNotBlank() }
            ?: viewIdResourceName?.substringAfterLast("/")
            ?: className?.substringAfterLast(".")
            ?: "Node [${bounds.left},${bounds.top}]"
}

enum class ExecutionTier(val label: String, val description: String) {
    ACCESSIBILITY_NODE("Accessibility Node", "Direct node action via AccessibilityNodeInfo (Fastest, Event-driven)"),
    ACCESSIBILITY_GESTURE("Gesture Dispatch", "Coordinate stroke via AccessibilityService.dispatchGesture (API 24+)"),
    ADB_BRIDGE("ADB Shell Bridge", "Shell fallback command via wireless/local ADB daemon ('input tap/swipe')"),
    CV_HEURISTIC("CV / Geometry Fallback", "Coordinate resolution from screen bounding-box or canvas heuristics")
}

enum class GlobalKeyType {
    BACK, HOME, RECENTS, NOTIFICATIONS
}

enum class StepStatus {
    IDLE, RUNNING, COMPLETED_PRIMARY, COMPLETED_FALLBACK, FAILED
}

data class WorkflowStep(
    val id: String,
    val name: String,
    val actionType: ActionType,
    val targetViewId: String? = null,
    val targetText: String? = null,
    val targetDescription: String? = null,
    val inputPayload: String? = null,
    val coords: Pair<Float, Float>? = null,
    val swipeEndCoords: Pair<Float, Float>? = null,
    val globalKey: GlobalKeyType? = null,
    val delayMs: Long = 500,
    var status: StepStatus = StepStatus.IDLE,
    var tierUsed: ExecutionTier? = null,
    var errorDetails: String? = null
)

enum class ActionType {
    LAUNCH_APP,
    CLICK_NODE,
    INPUT_TEXT,
    COORDINATE_TAP,
    SWIPE,
    GLOBAL_ACTION,
    WAIT_FOR_ELEMENT,
    DELAY,
    ADB_SHELL_COMMAND
}

data class AgentWorkflow(
    val id: String,
    val title: String,
    val description: String,
    val targetPackage: String,
    val steps: List<WorkflowStep>,
    val isLooping: Boolean = false
)

data class ActionResult(
    val success: Boolean,
    val tierUsed: ExecutionTier,
    val message: String,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class RemoteDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 5555,
    val isConnected: Boolean = false,
    val batteryPct: Int = 85,
    val androidVersion: String = "14",
    val apiLevel: Int = 34,
    val lastPingMs: Long = 42,
    val architecture: String = "arm64-v8a"
)

enum class SyslogSeverity(val code: Int, val label: String) {
    EMERGENCY(0, "EMERG"),
    ALERT(1, "ALERT"),
    CRITICAL(2, "CRIT"),
    ERROR(3, "ERROR"),
    WARNING(4, "WARN"),
    NOTICE(5, "NOTICE"),
    INFORMATIONAL(6, "INFO"),
    DEBUG(7, "DEBUG")
}

data class SyslogEntry(
    val id: String,
    val timestamp: String,
    val severity: SyslogSeverity,
    val appName: String,
    val message: String,
    val isTlsEncrypted: Boolean = true,
    val transmitted: Boolean = true
)
