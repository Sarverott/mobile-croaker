package com.example.llm

import android.content.Context
import android.util.Log
import com.example.accessibility.MobileCroakerAccessibilityService
import com.example.bridge.CapacitorBridgeWrapper
import com.example.telemetry.RsyslogClient
import com.example.model.SyslogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LlmAgentConfig(
    val baseUrl: String = "http://192.168.1.100:11434/v1", // Ollama / LocalAI default
    val apiKey: String = "",
    val modelName: String = "llama3.2:latest",
    val temperature: Double = 0.2,
    val maxTokens: Int = 1024,
    val isEnabled: Boolean = true
)

data class LlmStepLog(
    val step: Int,
    val reasoning: String,
    val actionTaken: String,
    val status: String
)

/**
 * Mobile Croaker OpenAI-Compatible LLM Autonomous Agent.
 * Connects to Ollama, vLLM, LocalAI, LM Studio, or OpenAI-compatible backends
 * to drive goal-oriented multi-step autonomous UI navigation.
 */
class OpenAiLlmAgent private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MobileCroakerLlm"

        @Volatile
        private var instance: OpenAiLlmAgent? = null

        fun getInstance(context: Context): OpenAiLlmAgent {
            return instance ?: synchronized(this) {
                instance ?: OpenAiLlmAgent(context.applicationContext).also { instance = it }
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _config = MutableStateFlow(LlmAgentConfig())
    val config: StateFlow<LlmAgentConfig> = _config.asStateFlow()

    private val _isAgentThinking = MutableStateFlow(false)
    val isAgentThinking: StateFlow<Boolean> = _isAgentThinking.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<LlmStepLog>>(emptyList())
    val executionLogs: StateFlow<List<LlmStepLog>> = _executionLogs.asStateFlow()

    private val capacitorBridge = CapacitorBridgeWrapper(context)

    fun updateConfig(newConfig: LlmAgentConfig) {
        _config.value = newConfig
    }

    /**
     * Executes autonomous goal loop using the configured OpenAI-compatible model.
     */
    suspend fun executeGoal(goal: String, maxSteps: Int = 6, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        _isAgentThinking.value = true
        _executionLogs.value = emptyList()

        RsyslogClient.instance.log(
            SyslogSeverity.INFORMATIONAL,
            "LLM_AGENT",
            "Autonomous goal engaged: '$goal' using model ${_config.value.modelName}"
        )

        try {
            val history = JSONArray()
            val systemPrompt = """
                You are Mobile Croaker, an autonomous mobile agent operating on an Android device (RAUSON node by Inceptorium Apokryf).
                Your goal: $goal
                
                You have tools to interact with the phone:
                1. {"action": "CLICK", "text": "target text"} or {"action": "CLICK", "viewId": "id"} or {"action": "CLICK", "x": x, "y": y}
                2. {"action": "TYPE", "text": "text to enter"}
                3. {"action": "SWIPE", "startX": 540, "startY": 1400, "endX": 540, "endY": 400}
                4. {"action": "BACK"}
                5. {"action": "HOME"}
                6. {"action": "DONE", "summary": "Goal completed successfully"}
                
                Always respond with valid JSON containing:
                "thought": "brief reasoning about what you see on screen",
                "action": "CLICK" | "TYPE" | "SWIPE" | "BACK" | "HOME" | "DONE",
                and required parameters.
            """.trimIndent()

            history.put(JSONObject().put("role", "system").put("content", systemPrompt))

            for (step in 1..maxSteps) {
                // 1. Inspect current UI hierarchy
                val hierarchyJson = capacitorBridge.inspectHierarchy()
                val currentPkg = MobileCroakerAccessibilityService.currentForegroundPackage.value

                val userMessage = """
                    [Step $step of $maxSteps]
                    Current Foreground App: $currentPkg
                    Screen View Hierarchy Snapshot:
                    $hierarchyJson
                    
                    What is the next action to achieve the goal: "$goal"?
                """.trimIndent()

                history.put(JSONObject().put("role", "user").put("content", userMessage))

                onProgress("Step $step: Querying LLM (${_config.value.modelName})...")

                // 2. Query LLM
                val responseJson = callChatCompletions(history)
                val assistantMsg = responseJson.optString("content", "{}")
                history.put(JSONObject().put("role", "assistant").put("content", assistantMsg))

                // Parse action
                val actionObj = extractJsonAction(assistantMsg)
                val actionType = actionObj.optString("action", "DONE").uppercase()
                val thought = actionObj.optString("thought", "Proceeding with action")

                val logEntry = LlmStepLog(
                    step = step,
                    reasoning = thought,
                    actionTaken = actionType,
                    status = "Executing..."
                )
                withContext(Dispatchers.Main) {
                    _executionLogs.value = _executionLogs.value + logEntry
                }

                if (actionType == "DONE") {
                    onProgress("Goal completed! ${actionObj.optString("summary")}")
                    return@withContext true
                }

                // 3. Execute action on device
                val executeResult = when (actionType) {
                    "CLICK" -> capacitorBridge.click(actionObj.toString())
                    "TYPE" -> capacitorBridge.inputText(actionObj.toString())
                    "SWIPE" -> {
                        val a11y = MobileCroakerAccessibilityService.instance
                        a11y?.dispatchSwipeGesture(540f, 1400f, 540f, 400f, 350)
                        "{\"success\": true}"
                    }
                    "BACK" -> {
                        MobileCroakerAccessibilityService.instance?.performGlobal(com.example.model.GlobalKeyType.BACK)
                        "{\"success\": true}"
                    }
                    "HOME" -> {
                        MobileCroakerAccessibilityService.instance?.performGlobal(com.example.model.GlobalKeyType.HOME)
                        "{\"success\": true}"
                    }
                    else -> "{\"success\": false, \"error\": \"Unknown action\"}"
                }

                RsyslogClient.instance.log(
                    SyslogSeverity.INFORMATIONAL,
                    "LLM_ACTION",
                    "Step $step action [$actionType]: $executeResult"
                )

                kotlinx.coroutines.delay(1000)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "LLM Autonomous Execution error", e)
            RsyslogClient.instance.log(SyslogSeverity.ERROR, "LLM_AGENT", "Autonomous goal failed: ${e.message}")
            false
        } finally {
            _isAgentThinking.value = false
        }
    }

    private fun callChatCompletions(messages: JSONArray): JSONObject {
        val cfg = _config.value
        val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"

        val requestBodyJson = JSONObject().apply {
            put("model", cfg.modelName)
            put("messages", messages)
            put("temperature", cfg.temperature)
            put("max_tokens", cfg.maxTokens)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)

        if (cfg.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${cfg.apiKey}")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("LLM server error: HTTP ${response.code} - ${response.body?.string()}")
            }
            val resStr = response.body?.string() ?: "{}"
            val resJson = JSONObject(resStr)
            val choices = resJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                return choices.getJSONObject(0).optJSONObject("message") ?: JSONObject()
            }
            return JSONObject()
        }
    }

    private fun extractJsonAction(rawText: String): JSONObject {
        return try {
            val start = rawText.indexOf("{")
            val end = rawText.lastIndexOf("}")
            if (start != -1 && end != -1 && end > start) {
                JSONObject(rawText.substring(start, end + 1))
            } else {
                JSONObject().put("action", "DONE").put("summary", rawText)
            }
        } catch (_: Exception) {
            JSONObject().put("action", "DONE").put("summary", rawText)
        }
    }
}
