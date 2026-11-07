package com.example.server

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.accessibility.MobileCroakerAccessibilityService
import com.example.bridge.AdbBridgeClient
import com.example.bridge.CapacitorBridgeWrapper
import com.example.mcp.MobileCroakerMcpServer
import com.example.telemetry.RsyslogClient
import com.example.model.SyslogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

/**
 * Mobile Croaker Embedded HTTP & WebSocket Server.
 * Exposes OpenAPI 3.0 REST controls, MCP JSON-RPC endpoint, and live telemetry
 * for remote AI agents and operator stations.
 */
class MobileCroakerHttpServer private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MobileCroakerServer"

        @Volatile
        private var instance: MobileCroakerHttpServer? = null

        fun getInstance(context: Context): MobileCroakerHttpServer {
            return instance ?: synchronized(this) {
                instance ?: MobileCroakerHttpServer(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _port = MutableStateFlow(8080)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _serverUrl = MutableStateFlow("http://127.0.0.1:8080")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _requestCount = MutableStateFlow(0L)
    val requestCount: StateFlow<Long> = _requestCount.asStateFlow()

    private val capacitorBridge = CapacitorBridgeWrapper(context)
    private val mcpServer = MobileCroakerMcpServer(context)

    fun startServer(customPort: Int = 8080) {
        if (_isRunning.value) return
        _port.value = customPort

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(customPort)
                val localIp = getDeviceIpAddress()
                _serverUrl.value = "http://$localIp:$customPort"
                _isRunning.value = true

                RsyslogClient.instance.log(
                    SyslogSeverity.NOTICE,
                    "HTTP_SERVER",
                    "Mobile Croaker OpenAPI Server active at ${_serverUrl.value}"
                )

                while (_isRunning.value && serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept() ?: break
                    scope.launch { handleClient(clientSocket) }
                }
            } catch (e: Exception) {
                if (_isRunning.value) {
                    Log.e(TAG, "Server error", e)
                    RsyslogClient.instance.log(SyslogSeverity.ERROR, "HTTP_SERVER", "Server error: ${e.message}")
                }
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun stopServer() {
        _isRunning.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        RsyslogClient.instance.log(SyslogSeverity.NOTICE, "HTTP_SERVER", "Mobile Croaker Server stopped.")
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        _requestCount.value++
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val out = s.getOutputStream()

                val requestLine = reader.readLine() ?: return@use
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@use

                val method = parts[0].uppercase()
                val path = parts[1].substringBefore("?")

                // Read headers
                var contentLength = 0
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.isEmpty()) break
                    if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                // Read body
                val body = if (contentLength > 0) {
                    val buf = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = reader.read(buf, read, contentLength - read)
                        if (r == -1) break
                        read += r
                    }
                    String(buf, 0, read)
                } else ""

                // Route request
                val (statusCode, contentType, responseBody) = routeRequest(method, path, body)
                sendHttpResponse(out, statusCode, contentType, responseBody)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client socket handling exception: ${e.message}")
        }
    }

    private suspend fun routeRequest(method: String, path: String, body: String): Triple<Int, String, String> {
        return when {
            method == "GET" && path == "/" -> {
                Triple(200, "text/html", generateWebDashboardHtml())
            }
            method == "GET" && path == "/api/v1/openapi.json" -> {
                Triple(200, "application/json", generateOpenApiSpec())
            }
            method == "GET" && path == "/api/v1/status" -> {
                val status = JSONObject().apply {
                    put("app", "Mobile Croaker")
                    put("identity", "Inceptorium Apokryf (apokryf.pl)")
                    put("nodeId", "RAUSON-NODE-${try { Build.MODEL as String? } catch (_: Throwable) { null } ?: "01"}")
                    put("accessibilityServiceArmed", MobileCroakerAccessibilityService.isServiceConnected.value)
                    put("foregroundPackage", MobileCroakerAccessibilityService.currentForegroundPackage.value)
                    put("systemAlertDetected", MobileCroakerAccessibilityService.isSystemAlertDetected.value)
                    put("eventsHandled", MobileCroakerAccessibilityService.eventCounter.value)
                }
                Triple(200, "application/json", status.toString())
            }
            method == "GET" && path == "/api/v1/hierarchy" -> {
                val hierarchy = capacitorBridge.inspectHierarchy()
                Triple(200, "application/json", hierarchy)
            }
            method == "POST" && path == "/api/v1/action/tap" -> {
                val res = capacitorBridge.click(body)
                Triple(200, "application/json", res)
            }
            method == "POST" && path == "/api/v1/action/type" -> {
                val res = capacitorBridge.inputText(body)
                Triple(200, "application/json", res)
            }
            method == "POST" && path == "/api/v1/action/swipe" -> {
                val res = mcpServer.handleMcpRequest(
                    JSONObject().apply {
                        put("jsonrpc", "2.0")
                        put("id", 1)
                        put("method", "tools/call")
                        put("params", JSONObject().apply {
                            put("name", "mobile_croaker_swipe")
                            put("arguments", try { JSONObject(body) } catch (_: Exception) { JSONObject() })
                        })
                    }.toString()
                )
                Triple(200, "application/json", res)
            }
            method == "POST" && path == "/api/v1/adb/shell" -> {
                val cmdObj = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val cmd = cmdObj.optString("command", "echo ok")
                val res = AdbBridgeClient.instance.executeCommand(cmd)
                val json = JSONObject().apply {
                    put("success", res.isSuccess)
                    put("exitCode", res.exitCode)
                    put("output", res.output)
                    put("latencyMs", res.executionMs)
                }
                Triple(200, "application/json", json.toString())
            }
            method == "POST" && (path == "/mcp" || path == "/api/v1/mcp") -> {
                val mcpResponse = mcpServer.handleMcpRequest(body)
                Triple(200, "application/json", mcpResponse)
            }
            method == "GET" && path == "/api/v1/stream/info" -> {
                val info = JSONObject().apply {
                    put("rtspStreamUrl", "rtsp://${getDeviceIpAddress()}:8554/live")
                    put("mjpegPreviewUrl", "${_serverUrl.value}/api/v1/stream/mjpeg")
                    put("status", "Camera & Screen Stream Ready")
                }
                Triple(200, "application/json", info.toString())
            }
            else -> {
                Triple(404, "application/json", "{\"error\": \"Not Found\", \"path\": \"$path\"}")
            }
        }
    }

    private fun sendHttpResponse(out: OutputStream, code: Int, contentType: String, body: String) {
        val statusMsg = if (code == 200) "OK" else if (code == 404) "Not Found" else "Error"
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code $statusMsg\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
                "Connection: close\r\n\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun generateOpenApiSpec(): String {
        return JSONObject().apply {
            put("openapi", "3.0.0")
            put("info", JSONObject().apply {
                put("title", "Mobile Croaker / RAUSON API")
                put("version", "1.0.0")
                put("description", "OpenAPI interface for Mobile Croaker autonomous mobile workspace agent. Inspired by project OpenClaw. Created by Inceptorium Apokryf (apokryf.pl).")
            })
            put("paths", JSONObject().apply {
                put("/api/v1/status", JSONObject().put("get", JSONObject().put("summary", "Device Status & Telemetry")))
                put("/api/v1/hierarchy", JSONObject().put("get", JSONObject().put("summary", "Dump active window UI hierarchy")))
                put("/api/v1/action/tap", JSONObject().put("post", JSONObject().put("summary", "Tap element or coordinates with 4-tier fallback")))
                put("/api/v1/action/type", JSONObject().put("post", JSONObject().put("summary", "Type text into field")))
                put("/api/v1/action/swipe", JSONObject().put("post", JSONObject().put("summary", "Swipe gesture")))
                put("/api/v1/adb/shell", JSONObject().put("post", JSONObject().put("summary", "Execute shell command via ADB bridge")))
                put("/mcp", JSONObject().put("post", JSONObject().put("summary", "Model Context Protocol JSON-RPC 2.0 endpoint")))
            })
        }.toString(2)
    }

    private fun generateWebDashboardHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Mobile Croaker • RAUSON Terminal</title>
                <style>
                    body { background: #0A0A0A; color: #F3F4F6; font-family: monospace; padding: 24px; line-height: 1.6; }
                    h1 { color: #FFFFFF; border-bottom: 1px solid #333; padding-bottom: 8px; }
                    .badge { background: #22C55E; color: #000; padding: 2px 8px; border-radius: 4px; font-weight: bold; }
                    .card { background: #161616; border: 1px solid #333; border-radius: 8px; padding: 16px; margin: 16px 0; }
                    a { color: #38BDF8; text-decoration: none; }
                    a:hover { text-decoration: underline; }
                    pre { background: #000; padding: 12px; border-radius: 4px; border: 1px solid #222; overflow-x: auto; }
                    code { color: #E5E7EB; }
                </style>
            </head>
            <body>
                <h1>MOBILE CROAKER // RAUSON OPERATIONAL NODE</h1>
                <p>Status: <span class="badge">ACTIVE</span> • Identity: <strong>Inceptorium Apokryf (apokryf.pl)</strong></p>
                <p>Inspired by project <a href="https://github.com/openclaw/openclaw" target="_blank">OpenClaw</a>.</p>
                
                <div class="card">
                    <h3>REST & OpenAPI Endpoints</h3>
                    <ul>
                        <li><a href="/api/v1/openapi.json">/api/v1/openapi.json</a> - OpenAPI 3.0 Specification</li>
                        <li><a href="/api/v1/status">/api/v1/status</a> - Live Node Telemetry</li>
                        <li><a href="/api/v1/hierarchy">/api/v1/hierarchy</a> - Live View Hierarchy Tree</li>
                        <li><code>POST /api/v1/action/tap</code> - Execute 4-tier fallback tap</li>
                        <li><code>POST /api/v1/action/type</code> - Input text</li>
                        <li><code>POST /api/v1/adb/shell</code> - Execute ADB bridge shell</li>
                    </ul>
                </div>

                <div class="card">
                    <h3>Model Context Protocol (MCP)</h3>
                    <p>Endpoint: <code>POST /mcp</code> (JSON-RPC 2.0)</p>
                    <p>Connect Claude Desktop, OpenAI Swarm, or Gemini agents to drive this phone autonomously as an MCP tool.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getDeviceIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
