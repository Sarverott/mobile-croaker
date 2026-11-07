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
import com.example.util.AssetHelper
import com.example.hardware.DeviceHardwareBridge
import android.media.AudioManager
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
    private val hwBridge = DeviceHardwareBridge.getInstance(context)

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
            method == "GET" && path == "/api/v1/hardware/clipboard" -> {
                val clip = hwBridge.getClipboardText()
                Triple(200, "application/json", JSONObject().put("clipboardText", clip).toString())
            }
            method == "POST" && path == "/api/v1/hardware/clipboard" -> {
                val obj = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val text = obj.optString("text", "")
                val ok = hwBridge.setClipboardText("AgentClip", text)
                Triple(200, "application/json", "{\"success\": $ok, \"length\": ${text.length}}")
            }
            method == "GET" && path == "/api/v1/hardware/telemetry" -> {
                val allHw = JSONObject().apply {
                    put("audio", hwBridge.getAudioTelemetry())
                    put("wifi", hwBridge.getWifiStatus())
                    put("bluetooth", hwBridge.getBluetoothStatus())
                    put("torch", hwBridge.isFlashlightOn())
                    put("location", hwBridge.getLastKnownLocation())
                }
                Triple(200, "application/json", allHw.toString())
            }
            method == "POST" && path == "/api/v1/hardware/torch" -> {
                val obj = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val enable = obj.optBoolean("enable", !hwBridge.isFlashlightOn())
                val ok = hwBridge.setFlashlight(enable)
                Triple(200, "application/json", "{\"success\": $ok, \"enabled\": $enable}")
            }
            method == "POST" && path == "/api/v1/telephony/sms" -> {
                val obj = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val addr = obj.optString("address", "")
                val text = obj.optString("text", "")
                val ok = hwBridge.sendSms(addr, text)
                Triple(200, "application/json", "{\"success\": $ok, \"address\": \"$addr\"}")
            }
            method == "GET" && path == "/api/v1/telephony/contacts" -> {
                val contacts = hwBridge.queryContacts(limit = 25)
                Triple(200, "application/json", JSONObject().put("contacts", contacts).toString())
            }
            method == "POST" && path == "/api/v1/hardware/button" -> {
                val obj = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val action = obj.optString("action", "")
                val ok = when (action) {
                    "volume_up" -> hwBridge.adjustVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE)
                    "volume_down" -> hwBridge.adjustVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER)
                    "power_dialog" -> hwBridge.pressPowerDialog()
                    "lock_screen" -> hwBridge.lockScreen()
                    "screenshot" -> hwBridge.takeAccessibilityScreenshot()
                    else -> false
                }
                Triple(200, "application/json", "{\"success\": $ok, \"action\": \"$action\"}")
            }
            method == "GET" && path.startsWith("/assets/web/") -> {
                val assetPath = "web/" + path.removePrefix("/assets/web/")
                val mime = if (path.endsWith(".css")) "text/css" else if (path.endsWith(".js")) "application/javascript" else "text/plain"
                val content = AssetHelper.loadAsset(context, assetPath)
                if (content.isNotEmpty()) {
                    Triple(200, mime, content)
                } else {
                    Triple(404, "application/json", "{\"error\": \"Asset not found\"}")
                }
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
        val asset = AssetHelper.loadAsset(context, "openapi/openapi.json")
        return if (asset.isNotEmpty()) asset else "{ \"openapi\": \"3.0.3\", \"info\": { \"title\": \"Mobile Croaker API\" } }"
    }

    private fun generateWebDashboardHtml(): String {
        val asset = AssetHelper.loadAsset(context, "web/dashboard.html")
        return if (asset.isNotEmpty()) asset else "<!DOCTYPE html><html><body><h1>Mobile Croaker Gateway</h1></body></html>"
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
