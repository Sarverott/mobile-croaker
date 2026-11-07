package com.example.scripting

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.accessibility.MobileCroakerAccessibilityService
import com.example.bridge.AdbBridgeClient
import com.example.bridge.CapacitorBridgeWrapper
import com.example.telemetry.RsyslogClient
import com.example.model.SyslogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScriptOutput(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val message: String
)

/**
 * Sandboxed Scripting Engine supporting:
 * 1. Pyodide (Python compiled to WebAssembly)
 * 2. JavaScript / TypeScript
 * 3. Shell / ADB Scripting
 * With native MobileCroaker bridge injection.
 */
class PyodideScriptEngine(private val context: Context, private val scope: CoroutineScope) {

    private val _consoleLogs = MutableStateFlow<List<ScriptOutput>>(emptyList())
    val consoleLogs: StateFlow<List<ScriptOutput>> = _consoleLogs.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val capacitorBridge = CapacitorBridgeWrapper(context)
    private var webView: WebView? = null

    init {
        log("ENGINE", "Scripting Engine initialized (Pyodide Wasm + JS + Shell)")
    }

    fun attachWebView(view: WebView) {
        this.webView = view
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.webChromeClient = WebChromeClient()
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                log("PYODIDE", "Sandbox ready. Pyodide runtime armed.")
            }
        }
        view.addJavascriptInterface(MobileCroakerJsBridge(), "MobileCroaker")
        view.loadDataWithBaseURL("https://pyodide.org", buildSandboxHtml(), "text/html", "UTF-8", null)
    }

    fun executePython(script: String) {
        if (_isExecuting.value) return
        _isExecuting.value = true
        log("PYTHON", "Running script via Pyodide...")

        val escapedScript = script.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        val jsCall = """
            (async function() {
                try {
                    window.postConsoleLog('info', 'Executing Python script...');
                    if (!window.pyodideReady) {
                        window.postConsoleLog('warn', 'Pyodide loading in progress, please wait a moment...');
                        await window.initPyodideRuntime();
                    }
                    await window.pyodide.runPythonAsync("$escapedScript");
                    window.postConsoleLog('info', 'Python script execution finished.');
                } catch (e) {
                    window.postConsoleLog('error', 'Python Error: ' + e.message);
                }
            })();
        """.trimIndent()

        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript(jsCall) { _ ->
                _isExecuting.value = false
            }
        }
    }

    fun executeJavaScript(script: String) {
        if (_isExecuting.value) return
        _isExecuting.value = true
        log("JS", "Executing JavaScript script...")

        val wrappedJs = """
            (async function() {
                try {
                    $script
                    window.postConsoleLog('info', 'JavaScript execution finished.');
                } catch (e) {
                    window.postConsoleLog('error', 'JS Error: ' + e.message);
                }
            })();
        """.trimIndent()

        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript(wrappedJs) { _ ->
                _isExecuting.value = false
            }
        }
    }

    fun executeShell(script: String) {
        scope.launch(Dispatchers.IO) {
            _isExecuting.value = true
            log("SHELL", "Running shell script:\n$script")
            for (line in script.lines()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val res = AdbBridgeClient.instance.executeCommand(trimmed)
                log("SHELL", "$ $trimmed -> (exit ${res.exitCode}): ${res.output}")
            }
            _isExecuting.value = false
        }
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
    }

    private fun log(tag: String, msg: String) {
        val entry = ScriptOutput(level = tag, message = msg)
        val list = _consoleLogs.value.toMutableList()
        list.add(0, entry)
        _consoleLogs.value = list.take(100)
    }

    inner class MobileCroakerJsBridge {
        @JavascriptInterface
        fun tap(x: Float, y: Float): Boolean {
            scope.launch {
                val a11y = MobileCroakerAccessibilityService.instance
                if (a11y != null) {
                    a11y.dispatchTapGesture(x, y)
                } else {
                    AdbBridgeClient.instance.fallbackTap(x, y)
                }
            }
            log("ACTION", "Tap at ($x, $y)")
            return true
        }

        @JavascriptInterface
        fun type(text: String): Boolean {
            scope.launch {
                capacitorBridge.inputText("{\"text\": \"$text\"}")
            }
            log("ACTION", "Type: '$text'")
            return true
        }

        @JavascriptInterface
        fun swipe(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
            scope.launch {
                val a11y = MobileCroakerAccessibilityService.instance
                if (a11y != null) {
                    a11y.dispatchSwipeGesture(x1, y1, x2, y2, 350)
                } else {
                    AdbBridgeClient.instance.fallbackSwipe(x1, y1, x2, y2, 350)
                }
            }
            log("ACTION", "Swipe ($x1,$y1)->($x2,$y2)")
            return true
        }

        @JavascriptInterface
        fun inspect(): String {
            val json = capacitorBridge.inspectHierarchy()
            log("INSPECT", "View hierarchy dumped")
            return json
        }

        @JavascriptInterface
        fun logMessage(level: String, msg: String) {
            log(level.uppercase(), msg)
        }
    }

    private fun buildSandboxHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <script src="https://cdn.jsdelivr.net/pyodide/v0.26.4/full/pyodide.js"></script>
                <script>
                    window.pyodideReady = false;
                    window.postConsoleLog = function(level, msg) {
                        if (window.MobileCroaker) {
                            window.MobileCroaker.logMessage(level, msg);
                        } else {
                            console.log('[' + level + '] ' + msg);
                        }
                    };
                    
                    async function initPyodideRuntime() {
                        try {
                            window.postConsoleLog('info', 'Booting Pyodide WebAssembly runtime...');
                            window.pyodide = await loadPyodide();
                            window.pyodideReady = true;
                            window.postConsoleLog('info', 'Pyodide ready! Python 3.12 active in sandbox.');
                        } catch (e) {
                            window.postConsoleLog('warn', 'Offline mode or CDN unavailable: ' + e.message);
                        }
                    }
                    
                    window.addEventListener('DOMContentLoaded', initPyodideRuntime);
                </script>
            </head>
            <body>
                <div id="status">Sandbox Initialized</div>
            </body>
            </html>
        """.trimIndent()
    }
}
