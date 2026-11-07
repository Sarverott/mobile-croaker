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
import com.example.hardware.DeviceHardwareBridge
import android.media.AudioManager

class MobileCroakerMcpServer(private val context: Context) {

    private val capacitorBridge = CapacitorBridgeWrapper(context)
    private val hwBridge = DeviceHardwareBridge.getInstance(context)

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
            "mobile_croaker_manage_clipboard" -> {
                val action = args.optString("action", "get").lowercase()
                if (action == "set") {
                    val text = args.optString("text", "")
                    val ok = hwBridge.setClipboardText("AgentClip", text)
                    toolTextResponse("{\"success\": $ok, \"action\": \"set\", \"length\": ${text.length}}")
                } else {
                    val clipText = hwBridge.getClipboardText()
                    val escaped = JSONObject().put("clipboardText", clipText).toString()
                    toolTextResponse(escaped)
                }
            }
            "mobile_croaker_hardware_control" -> {
                val subsystem = args.optString("subsystem").lowercase()
                when (subsystem) {
                    "torch", "flashlight" -> {
                        val state = args.optBoolean("enable", !hwBridge.isFlashlightOn())
                        val ok = hwBridge.setFlashlight(state)
                        toolTextResponse("{\"success\": $ok, \"subsystem\": \"torch\", \"enabled\": $state}")
                    }
                    "speakerphone" -> {
                        val state = args.optBoolean("enable", true)
                        val ok = hwBridge.setSpeakerphone(state)
                        toolTextResponse("{\"success\": $ok, \"subsystem\": \"speakerphone\", \"enabled\": $state}")
                    }
                    "volume" -> {
                        val direction = args.optInt("direction", AudioManager.ADJUST_SAME)
                        val stream = args.optInt("stream", AudioManager.STREAM_MUSIC)
                        val ok = hwBridge.adjustVolume(stream, direction)
                        toolTextResponse("{\"success\": $ok, \"subsystem\": \"volume\", \"telemetry\": ${hwBridge.getAudioTelemetry()}}")
                    }
                    "bluetooth" -> {
                        toolTextResponse(hwBridge.getBluetoothStatus().toString())
                    }
                    "wifi" -> {
                        toolTextResponse(hwBridge.getWifiStatus().toString())
                    }
                    "location" -> {
                        toolTextResponse(hwBridge.getLastKnownLocation().toString())
                    }
                    "vibrate" -> {
                        val duration = args.optLong("durationMs", 300L)
                        hwBridge.vibrate(duration)
                        toolTextResponse("{\"success\": true, \"vibratedMs\": $duration}")
                    }
                    else -> {
                        val allHw = JSONObject().apply {
                            put("audio", hwBridge.getAudioTelemetry())
                            put("wifi", hwBridge.getWifiStatus())
                            put("bluetooth", hwBridge.getBluetoothStatus())
                            put("torch", hwBridge.isFlashlightOn())
                        }
                        toolTextResponse(allHw.toString())
                    }
                }
            }
            "mobile_croaker_telephony_sms" -> {
                val action = args.optString("action", "query_contacts").lowercase()
                when (action) {
                    "send_sms" -> {
                        val address = args.optString("address", "")
                        val text = args.optString("text", "")
                        val ok = hwBridge.sendSms(address, text)
                        toolTextResponse("{\"success\": $ok, \"action\": \"send_sms\", \"address\": \"$address\"}")
                    }
                    "dial_call" -> {
                        val number = args.optString("number", "")
                        val ok = hwBridge.dialCall(number)
                        toolTextResponse("{\"success\": $ok, \"action\": \"dial_call\", \"number\": \"$number\"}")
                    }
                    "add_contact" -> {
                        val name = args.optString("name", "New Contact")
                        val number = args.optString("number", "")
                        val ok = hwBridge.addContact(name, number)
                        toolTextResponse("{\"success\": $ok, \"action\": \"add_contact\", \"name\": \"$name\"}")
                    }
                    else -> {
                        val query = args.optString("query").takeIf { it.isNotBlank() }
                        val limit = args.optInt("limit", 10)
                        val contacts = hwBridge.queryContacts(query, limit)
                        toolTextResponse(JSONObject().put("contacts", contacts).toString())
                    }
                }
            }
            "mobile_croaker_device_buttons" -> {
                val action = args.optString("action").lowercase()
                val ok = when (action) {
                    "volume_up" -> hwBridge.adjustVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE)
                    "volume_down" -> hwBridge.adjustVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER)
                    "power_dialog" -> hwBridge.pressPowerDialog()
                    "lock_screen" -> hwBridge.lockScreen()
                    "screenshot", "take_screenshot" -> hwBridge.takeAccessibilityScreenshot()
                    else -> false
                }
                toolTextResponse("{\"success\": $ok, \"action\": \"$action\"}")
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
            "mobile_croaker_manage_clipboard",
            "Read or write system clipboard text for instant typing substitution.",
            JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().put("type", "string").put("description", "'get' to read clipboard or 'set' to write"))
                    put("text", JSONObject().put("type", "string").put("description", "Text to place on clipboard when action is 'set'"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_hardware_control",
            "Control and query hardware subsystems: torch/flashlight, speakerphone, volume, bluetooth, wifi, location, and vibration.",
            JSONObject().apply {
                put("type", "object")
                put("required", JSONArray().put("subsystem"))
                put("properties", JSONObject().apply {
                    put("subsystem", JSONObject().put("type", "string").put("description", "Subsystem: 'torch', 'speakerphone', 'volume', 'bluetooth', 'wifi', 'location', or 'vibrate'"))
                    put("enable", JSONObject().put("type", "boolean").put("description", "State toggle for torch or speakerphone"))
                    put("direction", JSONObject().put("type", "integer").put("description", "Volume adjustment (-1 down, 1 up, 0 status)"))
                    put("durationMs", JSONObject().put("type", "integer").put("description", "Vibration duration in milliseconds"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_telephony_sms",
            "Manage phone telephony, contacts, and SMS messaging.",
            JSONObject().apply {
                put("type", "object")
                put("required", JSONArray().put("action"))
                put("properties", JSONObject().apply {
                    put("action", JSONObject().put("type", "string").put("description", "Action: 'send_sms', 'dial_call', 'query_contacts', or 'add_contact'"))
                    put("address", JSONObject().put("type", "string").put("description", "Phone number for SMS"))
                    put("text", JSONObject().put("type", "string").put("description", "Message text for SMS"))
                    put("number", JSONObject().put("type", "string").put("description", "Phone number to dial"))
                    put("name", JSONObject().put("type", "string").put("description", "Contact name"))
                    put("query", JSONObject().put("type", "string").put("description", "Search query for contacts"))
                })
            }
        ))

        array.put(createToolMeta(
            "mobile_croaker_device_buttons",
            "Trigger hardware keys: volume up, volume down, power dialog, lock screen, and accessibility screenshot.",
            JSONObject().apply {
                put("type", "object")
                put("required", JSONArray().put("action"))
                put("properties", JSONObject().apply {
                    put("action", JSONObject().put("type", "string").put("description", "Action: 'volume_up', 'volume_down', 'power_dialog', 'lock_screen', or 'screenshot'"))
                })
            }
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
