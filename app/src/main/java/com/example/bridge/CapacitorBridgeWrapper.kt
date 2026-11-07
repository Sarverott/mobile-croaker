package com.example.bridge

import android.content.Context
import android.content.Intent
import com.example.accessibility.OpenClawAccessibilityService
import com.example.model.ExecutionTier
import com.example.model.GlobalKeyType
import com.example.telemetry.RsyslogClient
import com.example.model.SyslogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenClaw Capacitor / Web Native Bridge Interface.
 * Allows external web agents, Capacitor plugins, or React/Vue apps to drive
 * full phone UI automation, hierarchy extraction, and fallback actions.
 */
class CapacitorBridgeWrapper(private val context: Context) {

    /**
     * Inspect active window hierarchy and return clean JSON structure.
     */
    fun inspectHierarchy(): String {
        val service = OpenClawAccessibilityService.instance
        if (service == null) {
            return JSONObject().apply {
                put("success", false)
                put("error", "Accessibility Service is not enabled")
            }.toString()
        }

        val rootNode = service.dumpCurrentHierarchy()
        val json = JSONObject().apply {
            put("success", true)
            put("foregroundPackage", OpenClawAccessibilityService.currentForegroundPackage.value)
            put("activityClass", OpenClawAccessibilityService.currentActivityClass.value)
            put("isSystemAlertDetected", OpenClawAccessibilityService.isSystemAlertDetected.value)
            put("tree", rootNode?.let { serializeNode(it) } ?: JSONObject.NULL)
        }
        return json.toString()
    }

    private fun serializeNode(node: com.example.model.UiNode): JSONObject {
        return JSONObject().apply {
            put("id", node.id)
            put("viewId", node.viewIdResourceName)
            put("text", node.text)
            put("contentDescription", node.contentDescription)
            put("className", node.className)
            put("isClickable", node.isClickable)
            put("isEditable", node.isEditable)
            put("bounds", JSONObject().apply {
                put("left", node.bounds.left)
                put("top", node.bounds.top)
                put("right", node.bounds.right)
                put("bottom", node.bounds.bottom)
                put("centerX", node.centerX)
                put("centerY", node.centerY)
            })
            if (node.children.isNotEmpty()) {
                val array = JSONArray()
                node.children.forEach { array.put(serializeNode(it)) }
                put("children", array)
            }
        }
    }

    /**
     * Executes click with automatic multi-tier fallback:
     * 1. Direct Node Click -> 2. Gesture Tap -> 3. ADB Bridge Tap
     */
    suspend fun click(paramsJson: String): String = withContext(Dispatchers.Default) {
        val params = try { JSONObject(paramsJson) } catch (e: Exception) { JSONObject() }
        val viewId = params.optString("viewId").takeIf { it.isNotBlank() }
        val text = params.optString("text").takeIf { it.isNotBlank() }
        val desc = params.optString("contentDescription").takeIf { it.isNotBlank() }
        val x = params.optDouble("x", -1.0).toFloat()
        val y = params.optDouble("y", -1.0).toFloat()

        val service = OpenClawAccessibilityService.instance

        // Level 1: Try accessibility node click
        if (service != null) {
            val nodeSuccess = service.clickNode(text = text, viewId = viewId, contentDesc = desc)
            if (nodeSuccess) {
                RsyslogClient.instance.log(SyslogSeverity.INFORMATIONAL, "CAPACITOR_BRIDGE", "Click succeeded via Tier 1: Accessibility Node")
                return@withContext resultJson(true, ExecutionTier.ACCESSIBILITY_NODE, "Clicked element via Accessibility Node")
            }

            // Level 2: Try gesture coordinate tap if x, y provided
            if (x >= 0 && y >= 0) {
                val gestureSuccess = service.dispatchTapGesture(x, y)
                if (gestureSuccess) {
                    RsyslogClient.instance.log(SyslogSeverity.INFORMATIONAL, "CAPACITOR_BRIDGE", "Click succeeded via Tier 2: Gesture Coordinate ($x, $y)")
                    return@withContext resultJson(true, ExecutionTier.ACCESSIBILITY_GESTURE, "Tapped coordinates ($x, $y) via Gesture Dispatch")
                }
            }
        }

        // Level 3: Fall back to ADB Shell Bridge
        val adbClient = AdbBridgeClient.instance
        val targetX = if (x >= 0) x else 500f
        val targetY = if (y >= 0) y else 800f
        val adbRes = adbClient.fallbackTap(targetX, targetY)

        RsyslogClient.instance.log(SyslogSeverity.WARNING, "CAPACITOR_BRIDGE", "Click resolved via Tier 3: ADB Bridge Fallback")
        return@withContext resultJson(adbRes.isSuccess, ExecutionTier.ADB_BRIDGE, "Executed fallback click via ADB Bridge: ${adbRes.output}")
    }

    /**
     * Input text with fallback.
     */
    suspend fun inputText(paramsJson: String): String = withContext(Dispatchers.Default) {
        val params = try { JSONObject(paramsJson) } catch (e: Exception) { JSONObject() }
        val text = params.optString("text", "")
        val viewId = params.optString("viewId").takeIf { it.isNotBlank() }

        val service = OpenClawAccessibilityService.instance
        if (service != null && service.setNodeText(text, viewId = viewId)) {
            return@withContext resultJson(true, ExecutionTier.ACCESSIBILITY_NODE, "Text set via AccessibilityNodeInfo")
        }

        // Fallback to ADB
        val res = AdbBridgeClient.instance.fallbackInputText(text)
        return@withContext resultJson(res.isSuccess, ExecutionTier.ADB_BRIDGE, "Text typed via ADB Shell Bridge fallback")
    }

    /**
     * Launches external target mobile app.
     */
    fun launchApp(packageName: String): String {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                RsyslogClient.instance.log(SyslogSeverity.NOTICE, "CAPACITOR_BRIDGE", "Launched external app: $packageName")
                resultJson(true, ExecutionTier.ACCESSIBILITY_NODE, "Launched $packageName")
            } else {
                resultJson(false, ExecutionTier.ACCESSIBILITY_NODE, "App not installed: $packageName")
            }
        } catch (e: Exception) {
            resultJson(false, ExecutionTier.ACCESSIBILITY_NODE, "Launch failed: ${e.message}")
        }
    }

    private fun resultJson(success: Boolean, tier: ExecutionTier, message: String): String {
        return JSONObject().apply {
            put("success", success)
            put("tier", tier.name)
            put("tierLabel", tier.label)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }
}
