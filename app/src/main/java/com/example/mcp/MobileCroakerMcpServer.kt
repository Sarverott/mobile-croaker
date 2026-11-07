package com.example.mcp

import android.content.Context
import com.example.accessibility.MobileCroakerAccessibilityService
import com.example.agent.AgentActionEngine
import com.example.agent.YamlScenarioManager
import com.example.bridge.AdbBridgeClient
import com.example.bridge.CapacitorBridgeWrapper
import com.example.model.GlobalKeyType
import com.example.telemetry.RsyslogClient
import com.example.model.SyslogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mobile Croaker Model Context Protocol (MCP) Server.
 * Exposes the Android device workspace as an MCP tool provider for AI agents (Claude, OpenAI, Gemini, etc.).
 * Conforms to JSON-RPC 2.0 MCP Specification.
 */
class MobileCroakerMcpServer(private val context: Context) {

    private val capacitorBridge = CapacitorBridgeWrapper(context)

    /**
     * Handles incoming MCP JSON-RPC 2.0 request.
     */
    suspend fun handleMcpRequest(jsonRpcRequest: String): String = withContext(Dispatchers.Default) {
        try {
            val req = JSONObject(jsonRpcRequest)
            val id = req.opt("id")
            val method = req.optString("method")
            val params = req.optJSONObject("params") ?: JSONObject()

            when (method) {
                "tools/list" -> {
                    val toolsList = getToolsList()
                    jsonRpcSuccess(id, JSONObject().put("tools", toolsList))
                }
                "tools/call" -> {
                    val toolName = params.optString("name")
                    val arguments = params.optJSONObject("arguments") ?: JSONObject()
                    val result = executeTool(toolName, arguments)
                    jsonRpcSuccess(id, result)
                }
                "initialize" -> {
                    val initResult = JSONObject().apply {
                        put("protocolVersion", "2024-11-05")
                        put("serverInfo", JSONObject().apply {
                            put("name", "MobileCroaker-MCP-Server")
                            put("version", "1.0.0")
                            put("identity", "Inceptorium Apokryf (apokryf.pl) / RAUSON")
                        })
                        put("capabilities", JSONObject().apply {
                            put("tools", JSONObject().put("listChanged", false))
                        })
                    }
                    jsonRpcSuccess(id, initResult)
                }
                "ping" -> {
                    jsonRpcSuccess(id, JSONObject().put("status", "pong"))
                }
                else -> {
                    jsonRpcError(id, -32601, "Method '$method' not found")
                }
            }
        } catch (e: Exception) {
            jsonRpcError(null, -32700, "Parse error: ${e.message}")
        }
    }

    private suspend fun executeTool(name: String, args: JSONObject): JSONObject {
        RsyslogClient.instance.log(SyslogSeverity.INFORMATIONAL, "MCP_TOOL_EXEC", "MCP calling tool: $name")
        return when (name) {
            "mobile_croaker_inspect_hierarchy" -> {
                val hierarchyJson = capacitorBridge.inspectHierarchy()
                toolTextResponse(hierarchyJson)
            }
            "mobile_croaker_tap" -> {
                val result = capacitorBridge.click(args.toString())
                toolTextResponse(result)
            }
            "mobile_croaker_type" -> {
                val result = capacitorBridge.inputText(args.toString())
                toolTextResponse(result)
            }
            "mobile_croaker_swipe" -> {
                val x1 = args.optDouble("startX", 540.0).toFloat()
                val y1 = args.optDouble("startY", 1400.0).toFloat()
                val x2 = args.optDouble("endX", 540.0).toFloat()
                val y2 = args.optDouble("endY", 400.0).toFloat()
                val duration = args.optLong("durationMs", 350L)

                val a11y = MobileCroakerAccessibilityService.instance
                val success = if (a11y != null) {
                    a11y.dispatchSwipeGesture(x1, y1, x2, y2, duration)
                } else {
                    val res = AdbBridgeClient.instance.fallbackSwipe(x1, y1, x2, y2, duration)
                    res.isSuccess
                }
                toolTextResponse("{\"success\": $success, \"action\": \"swipe\", \"from\": [$x1, $y1], \"to\": [$x2, $y2]}")
            }
            "mobile_croaker_global_action" -> {
                val keyStr = args.optString("action", "BACK").uppercase()
                val key = try { GlobalKeyType.valueOf(keyStr) } catch (_: Exception) { GlobalKeyType.BACK }
                val a11y = MobileCroakerAccessibilityService.instance
                val success = a11y?.performGlobal(key) ?: false
                toolTextResponse("{\"success\": $success, \"global_action\": \"${key.name}\"}")
            }
            "mobile_croaker_launch_app" -> {
                val pkg = args.optString("package_name")
                val result = capacitorBridge.launchApp(pkg)
                toolTextResponse(result)
            }
            "mobile_croaker_execute_scenario" -> {
                val yamlStr = args.optString("scenario_yaml")
                val parsed = YamlScenarioManager.fromYaml(yamlStr)
                if (parsed.isSuccess) {
                    val wf = parsed.getOrThrow()
                    AgentActionEngine.getInstance(context).selectWorkflow(wf)
                    AgentActionEngine.getInstance(context).startExecution()
                    toolTextResponse("{\"success\": true, \"message\": \"Started execution of scenario: ${wf.title}\"}")
                } else {
                    toolTextResponse("{\"success\": false, \"error\": \"YAML compilation failed: ${parsed.exceptionOrNull()?.message}\"}")
                }
            }
            "mobile_croaker_adb_shell" -> {
                val cmd = args.optString("command")
                val res = AdbBridgeClient.instance.executeCommand(cmd)
                toolTextResponse("{\"success\": ${res.isSuccess}, \"exitCode\": ${res.exitCode}, \"output\": \"${res.output.replace("\"", "\\\"")}\"}")
            }
            "mobile_croaker_get_telemetry" -> {
                val status = JSONObject().apply {
                    put("serviceConnected", MobileCroakerAccessibilityService.isServiceConnected.value)
                    put("foregroundPackage", MobileCroakerAccessibilityService.currentForegroundPackage.value)
                    put("activityClass", MobileCroakerAccessibilityService.currentActivityClass.value)
                    put("systemAlertDetected", MobileCroakerAccessibilityService.isSystemAlertDetected.value)
                    put("eventCount", MobileCroakerAccessibilityService.eventCounter.value)
                }
                toolTextResponse(status.toString())
            }
            else -> {
                toolTextResponse("{\"error\": \"Unknown tool $name\"}")
            }
        }
    }

    private fun toolTextResponse(text: String): JSONObject {
        return JSONObject().apply {
            val contentArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", text)
                })
            }
            put("content", contentArr)
            put("isError", false)
        }
    }

    private fun getToolsList(): JSONArray {
        val array = JSONArray()

        array.put(createToolMeta(
            "mobile_croaker_inspect_hierarchy",
            "Dumps the entire visible UI tree from foreground application, including text, viewId, bounds, and clickable states.",
            JSONObject().put("type", "object").put("properties", JSONObject())
        ))

        array.put(createToolMeta(
            "mobile_croaker_tap",
            "Clicks or taps an on-screen element with automatic 4-tier fallback (Accessibility Node -> Gesture Dispatch -> ADB Bridge -> CV Geometry).",
            JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("text", JSONObject().put("type", "string").put("description", "Visible text of the element"))
                    put("viewId", JSONObject().put("type", "string").put("description", "Android resource view ID"))
                    put("contentDescription", JSONObject().put("type", "string").put("description", "Accessibility label or content description"))
                    put("x", JSONObject().put("type", "number").put("description", "Target X coordinate"))
                    put("y", JSONObject().put("type", "number").put("description", "Target Y coordinate"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_type",
            "Inputs text into the focused or specified editable field.",
            JSONObject().apply {
                put("type", "object")
                put("required", JSONArray().put("text"))
                put("properties", JSONObject().apply {
                    put("text", JSONObject().put("type", "string").put("description", "Text to type"))
                    put("viewId", JSONObject().put("type", "string").put("description", "Target editable field view ID"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_swipe",
            "Dispatches a swipe gesture across specified screen coordinates.",
            JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("startX", JSONObject().put("type", "number").put("description", "Start X"))
                    put("startY", JSONObject().put("type", "number").put("description", "Start Y"))
                    put("endX", JSONObject().put("type", "number").put("description", "End X"))
                    put("endY", JSONObject().put("type", "number").put("description", "End Y"))
                    put("durationMs", JSONObject().put("type", "number").put("description", "Duration in ms"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_global_action",
            "Triggers Android system global navigation action (BACK, HOME, RECENTS, NOTIFICATIONS).",
            JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().put("type", "string").put("enum", JSONArray(listOf("BACK", "HOME", "RECENTS", "NOTIFICATIONS"))))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_launch_app",
            "Launches an external application by Android package name.",
            JSONObject().apply {
                put("type", "object")
                put("required", JSONArray().put("package_name"))
                put("properties", JSONObject().apply {
                    put("package_name", JSONObject().put("type", "string").put("description", "e.g. com.android.settings"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_execute_scenario",
            "Parses and immediately executes a multi-step YAML automation scenario.",
            JSONObject().apply {
                put("type", "object")
                put("required", JSONArray().put("scenario_yaml"))
                put("properties", JSONObject().apply {
                    put("scenario_yaml", JSONObject().put("type", "string").put("description", "YAML string containing workflow steps"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_adb_shell",
            "Executes ad-hoc shell command via ADB bridge (e.g. 'input tap 500 500', 'pm install', 'screencap').",
            JSONObject().apply {
                put("type", "object")
                put("required", JSONArray().put("command"))
                put("properties", JSONObject().apply {
                    put("command", JSONObject().put("type", "string").put("description", "Shell command"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_get_telemetry",
            "Retrieves current device and accessibility telemetry state.",
            JSONObject().put("type", "object").put("properties", JSONObject())
        ))

        return array
    }

    private fun createToolMeta(name: String, desc: String, schema: JSONObject): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("description", desc)
            put("inputSchema", schema)
        }
    }

    private fun jsonRpcSuccess(id: Any?, result: JSONObject): String {
        return JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id ?: JSONObject.NULL)
            put("result", result)
        }.toString()
    }

    private fun jsonRpcError(id: Any?, code: Int, message: String): String {
        return JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id ?: JSONObject.NULL)
            put("error", JSONObject().apply {
                put("code", code)
                put("message", message)
            })
        }.toString()
    }
}
