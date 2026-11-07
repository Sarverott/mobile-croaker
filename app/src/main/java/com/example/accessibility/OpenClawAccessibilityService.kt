package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.GlobalKeyType
import com.example.model.UiNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * OpenClaw Core Accessibility Service:
 * Operates on non-rooted devices across Android 7.0 (API 24) to Android 15 (API 35+).
 * Handles UI tree inspection, node action execution, coordinate gesture dispatch,
 * and system UI dialog detection.
 */
class OpenClawAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "OpenClawA11yService"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _currentForegroundPackage = MutableStateFlow<String>("com.example")
        val currentForegroundPackage: StateFlow<String> = _currentForegroundPackage.asStateFlow()

        private val _currentActivityClass = MutableStateFlow<String>("")
        val currentActivityClass: StateFlow<String> = _currentActivityClass.asStateFlow()

        private val _eventCounter = MutableStateFlow(0L)
        val eventCounter: StateFlow<Long> = _eventCounter.asStateFlow()

        private val _isSystemAlertDetected = MutableStateFlow(false)
        val isSystemAlertDetected: StateFlow<Boolean> = _isSystemAlertDetected.asStateFlow()

        private val _lastAlertMessage = MutableStateFlow<String?>(null)
        val lastAlertMessage: StateFlow<String?> = _lastAlertMessage.asStateFlow()

        @Volatile
        var instance: OpenClawAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        Log.i(TAG, "OpenClaw Accessibility Service connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        _eventCounter.value++

        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            _currentForegroundPackage.value = pkg
        }

        val cls = event.className?.toString()
        if (!cls.isNullOrBlank()) {
            _currentActivityClass.value = cls
        }

        // Monitor for system dialogs, ANRs, or permission prompts
        checkSystemDialogs(event)
    }

    override fun onInterrupt() {
        Log.w(TAG, "OpenClaw Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        _isServiceConnected.value = false
    }

    private fun checkSystemDialogs(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val isSystemPkg = pkg.contains("packageinstaller") ||
                pkg == "android" ||
                pkg.contains("systemui") ||
                pkg.contains("permissioncontroller")

        if (isSystemPkg) {
            val textList = event.text.mapNotNull { it?.toString() }
            if (textList.isNotEmpty()) {
                val fullText = textList.joinToString(" ")
                if (fullText.contains("permission", ignoreCase = true) ||
                    fullText.contains("allow", ignoreCase = true) ||
                    fullText.contains("isn't responding", ignoreCase = true) ||
                    fullText.contains("wait", ignoreCase = true)
                ) {
                    _isSystemAlertDetected.value = true
                    _lastAlertMessage.value = "System Dialog ($pkg): $fullText"
                }
            }
        }
    }

    /**
     * Attempts to automatically click "Allow" or "OK" on system permission/ANR dialogs.
     */
    fun autoResolveSystemDialog(): Boolean {
        val root = rootInActiveWindow ?: return false
        try {
            val candidates = listOf("Allow", "While using the app", "OK", "Wait", "Continue")
            for (label in candidates) {
                val nodes = root.findAccessibilityNodeInfosByText(label)
                for (node in nodes) {
                    if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        _isSystemAlertDetected.value = false
                        _lastAlertMessage.value = null
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in autoResolveSystemDialog", e)
        }
        return false
    }

    /**
     * Dumps the current active window view hierarchy as an immutable UiNode tree snapshot.
     */
    fun dumpCurrentHierarchy(): UiNode? {
        val root = rootInActiveWindow ?: return null
        return try {
            convertNode(root)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dump node hierarchy", e)
            null
        }
    }

    private fun convertNode(info: AccessibilityNodeInfo): UiNode {
        val bounds = Rect()
        info.getBoundsInScreen(bounds)

        val children = mutableListOf<UiNode>()
        for (i in 0 until info.childCount) {
            info.getChild(i)?.let { childInfo ->
                children.add(convertNode(childInfo))
            }
        }

        return UiNode(
            id = UUID.randomUUID().toString(),
            viewIdResourceName = info.viewIdResourceName,
            text = info.text?.toString(),
            contentDescription = info.contentDescription?.toString(),
            className = info.className?.toString(),
            packageName = info.packageName?.toString(),
            bounds = bounds,
            isClickable = info.isClickable,
            isEditable = info.isEditable,
            isScrollable = info.isScrollable,
            isVisibleToUser = info.isVisibleToUser,
            isEnabled = info.isEnabled,
            children = children
        )
    }

    /**
     * Level 1 Action: Click node by selector criteria.
     */
    fun clickNode(text: String? = null, viewId: String? = null, contentDesc: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchedNode = findNode(root, text, viewId, contentDesc) ?: return false

        // Try direct click on node or its clickable ancestors
        var curr: AccessibilityNodeInfo? = matchedNode
        while (curr != null) {
            if (curr.isClickable && curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            curr = curr.parent
        }

        // If direct node click action failed, fall back to center coordinate tap via dispatchGesture
        val bounds = Rect()
        matchedNode.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            return dispatchTapGesture(bounds.exactCenterX(), bounds.exactCenterY())
        }

        return false
    }

    /**
     * Level 1 Action: Set text into an editable view node.
     */
    fun setNodeText(textToSet: String, viewId: String? = null, textMatch: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchedNode = findNode(root, textMatch, viewId, null) ?: return false

        if (matchedNode.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToSet)
            }
            return matchedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    /**
     * Level 2 Fallback: Native coordinate tap via dispatchGesture (API 24+)
     */
    fun dispatchTapGesture(x: Float, y: Float, durationMs: Long = 100): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(50))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * Level 2 Fallback: Native coordinate swipe via dispatchGesture (API 24+)
     */
    fun dispatchSwipeGesture(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * Dispatches global navigation action (BACK, HOME, RECENTS).
     */
    fun performGlobal(globalKeyType: GlobalKeyType): Boolean {
        val action = when (globalKeyType) {
            GlobalKeyType.BACK -> GLOBAL_ACTION_BACK
            GlobalKeyType.HOME -> GLOBAL_ACTION_HOME
            GlobalKeyType.RECENTS -> GLOBAL_ACTION_RECENTS
            GlobalKeyType.NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
        }
        return performGlobalAction(action)
    }

    private fun findNode(
        root: AccessibilityNodeInfo,
        text: String?,
        viewId: String?,
        contentDesc: String?
    ): AccessibilityNodeInfo? {
        // Priority 1: Match by viewId
        if (!viewId.isNullOrBlank()) {
            val list = root.findAccessibilityNodeInfosByViewId(viewId)
            if (list.isNotEmpty()) return list[0]
        }
        // Priority 2: Match by text
        if (!text.isNullOrBlank()) {
            val list = root.findAccessibilityNodeInfosByText(text)
            if (list.isNotEmpty()) return list[0]
        }
        // Priority 3: Deep search for contentDescription
        if (!contentDesc.isNullOrBlank()) {
            return searchContentDesc(root, contentDesc)
        }
        return null
    }

    private fun searchContentDesc(node: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val match = searchContentDesc(child, desc)
                if (match != null) return match
            }
        }
        return null
    }
}
