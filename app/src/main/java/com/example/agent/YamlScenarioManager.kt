package com.example.agent

import com.example.model.ActionType
import com.example.model.AgentWorkflow
import com.example.model.GlobalKeyType
import com.example.model.StepStatus
import com.example.model.WorkflowStep
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Mobile Croaker / RAUSON YAML & JSON Scenario Manager.
 * Enables import, export, dynamic compilation, and execution of codable automation workflows.
 */
object YamlScenarioManager {

    /**
     * Serializes an AgentWorkflow into a clean YAML document string.
     */
    fun toYaml(workflow: AgentWorkflow): String {
        val sb = StringBuilder()
        sb.append("# Mobile Croaker / RAUSON Workflow Scenario\n")
        sb.append("# Identity: Inceptorium Apokryf (apokryf.pl)\n")
        sb.append("# Inspired by project OpenClaw\n")
        sb.append("version: \"1.0\"\n")
        sb.append("id: \"${escapeYaml(workflow.id)}\"\n")
        sb.append("title: \"${escapeYaml(workflow.title)}\"\n")
        sb.append("description: \"${escapeYaml(workflow.description)}\"\n")
        sb.append("target_package: \"${escapeYaml(workflow.targetPackage)}\"\n")
        sb.append("is_looping: ${workflow.isLooping}\n")
        sb.append("steps:\n")

        for (step in workflow.steps) {
            sb.append("  - id: \"${step.id}\"\n")
            sb.append("    name: \"${escapeYaml(step.name)}\"\n")
            sb.append("    action: \"${step.actionType.name}\"\n")
            if (step.targetViewId != null) sb.append("    view_id: \"${escapeYaml(step.targetViewId)}\"\n")
            if (step.targetText != null) sb.append("    target_text: \"${escapeYaml(step.targetText)}\"\n")
            if (step.targetDescription != null) sb.append("    description: \"${escapeYaml(step.targetDescription)}\"\n")
            if (step.inputPayload != null) sb.append("    payload: \"${escapeYaml(step.inputPayload)}\"\n")
            if (step.coords != null) sb.append("    coords: [${step.coords.first.toInt()}, ${step.coords.second.toInt()}]\n")
            if (step.swipeEndCoords != null) sb.append("    swipe_end: [${step.swipeEndCoords.first.toInt()}, ${step.swipeEndCoords.second.toInt()}]\n")
            if (step.globalKey != null) sb.append("    global_key: \"${step.globalKey.name}\"\n")
            sb.append("    delay_ms: ${step.delayMs}\n")
        }

        return sb.toString()
    }

    /**
     * Parses a YAML or JSON workflow string into a compiled AgentWorkflow.
     */
    fun fromYaml(content: String): Result<AgentWorkflow> {
        return try {
            val trimmed = content.trim()
            if (trimmed.startsWith("{")) {
                return fromJson(trimmed)
            }

            var id = "rauson-custom-" + UUID.randomUUID().toString().take(6)
            var title = "Custom Scenario"
            var description = "Imported YAML Scenario"
            var targetPackage = "com.android.settings"
            var isLooping = false
            val steps = mutableListOf<WorkflowStep>()

            var currentStepMap = mutableMapOf<String, String>()
            var inStepsBlock = false

            fun commitStep() {
                if (currentStepMap.isNotEmpty()) {
                    val stepId = currentStepMap["id"] ?: ("step-" + steps.size)
                    val stepName = currentStepMap["name"] ?: ("Step " + (steps.size + 1))
                    val actionStr = currentStepMap["action"] ?: ActionType.CLICK_NODE.name
                    val actionType = try { ActionType.valueOf(actionStr.uppercase()) } catch (_: Exception) { ActionType.CLICK_NODE }
                    val coords = parseCoords(currentStepMap["coords"])
                    val swipeEnd = parseCoords(currentStepMap["swipe_end"])
                    val globalKey = currentStepMap["global_key"]?.let {
                        try { GlobalKeyType.valueOf(it.uppercase()) } catch (_: Exception) { null }
                    }
                    val delay = currentStepMap["delay_ms"]?.toLongOrNull() ?: 500L

                    steps.add(
                        WorkflowStep(
                            id = stepId,
                            name = stepName,
                            actionType = actionType,
                            targetViewId = currentStepMap["view_id"],
                            targetText = currentStepMap["target_text"],
                            targetDescription = currentStepMap["description"],
                            inputPayload = currentStepMap["payload"],
                            coords = coords,
                            swipeEndCoords = swipeEnd,
                            globalKey = globalKey,
                            delayMs = delay,
                            status = StepStatus.IDLE
                        )
                    )
                    currentStepMap = mutableMapOf()
                }
            }

            for (line in content.lines()) {
                val lineTrimmed = line.trim()
                if (lineTrimmed.startsWith("#") || lineTrimmed.isEmpty()) continue

                if (lineTrimmed == "steps:") {
                    inStepsBlock = true
                    continue
                }

                if (!inStepsBlock) {
                    val colonIdx = lineTrimmed.indexOf(':')
                    if (colonIdx != -1) {
                        val key = lineTrimmed.substring(0, colonIdx).trim()
                        val value = unquote(lineTrimmed.substring(colonIdx + 1).trim())
                        when (key) {
                            "id" -> id = value
                            "title", "name" -> title = value
                            "description" -> description = value
                            "target_package", "package" -> targetPackage = value
                            "is_looping", "loop" -> isLooping = value.toBoolean()
                        }
                    }
                } else {
                    if (lineTrimmed.startsWith("- ")) {
                        commitStep()
                        val rest = lineTrimmed.substring(2).trim()
                        val colon = rest.indexOf(':')
                        if (colon != -1) {
                            val k = rest.substring(0, colon).trim()
                            val v = unquote(rest.substring(colon + 1).trim())
                            currentStepMap[k] = v
                        }
                    } else {
                        val colon = lineTrimmed.indexOf(':')
                        if (colon != -1) {
                            val k = lineTrimmed.substring(0, colon).trim()
                            val v = unquote(lineTrimmed.substring(colon + 1).trim())
                            currentStepMap[k] = v
                        }
                    }
                }
            }
            commitStep()

            if (steps.isEmpty()) {
                // Add fallback step if empty
                steps.add(
                    WorkflowStep(
                        id = "step-1",
                        name = "Launch Application",
                        actionType = ActionType.LAUNCH_APP,
                        delayMs = 1000L
                    )
                )
            }

            Result.success(
                AgentWorkflow(
                    id = id,
                    title = title,
                    description = description,
                    targetPackage = targetPackage,
                    steps = steps,
                    isLooping = isLooping
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fromJson(jsonStr: String): Result<AgentWorkflow> {
        return try {
            val json = JSONObject(jsonStr)
            val id = json.optString("id", UUID.randomUUID().toString())
            val title = json.optString("title", "Imported JSON Scenario")
            val desc = json.optString("description", "")
            val targetPkg = json.optString("target_package", "com.android.settings")
            val isLooping = json.optBoolean("is_looping", false)

            val stepsArr = json.optJSONArray("steps") ?: JSONArray()
            val stepsList = mutableListOf<WorkflowStep>()

            for (i in 0 until stepsArr.length()) {
                val stepObj = stepsArr.getJSONObject(i)
                val stepId = stepObj.optString("id", "step-$i")
                val stepName = stepObj.optString("name", "Step ${i + 1}")
                val actionType = try {
                    ActionType.valueOf(stepObj.optString("action", "CLICK_NODE").uppercase())
                } catch (_: Exception) {
                    ActionType.CLICK_NODE
                }
                val coordsArr = stepObj.optJSONArray("coords")
                val coords = if (coordsArr != null && coordsArr.length() >= 2) {
                    Pair(coordsArr.getDouble(0).toFloat(), coordsArr.getDouble(1).toFloat())
                } else null

                val swipeArr = stepObj.optJSONArray("swipe_end")
                val swipeEnd = if (swipeArr != null && swipeArr.length() >= 2) {
                    Pair(swipeArr.getDouble(0).toFloat(), swipeArr.getDouble(1).toFloat())
                } else null

                stepsList.add(
                    WorkflowStep(
                        id = stepId,
                        name = stepName,
                        actionType = actionType,
                        targetViewId = stepObj.optString("view_id").takeIf { it.isNotBlank() },
                        targetText = stepObj.optString("target_text").takeIf { it.isNotBlank() },
                        targetDescription = stepObj.optString("description").takeIf { it.isNotBlank() },
                        inputPayload = stepObj.optString("payload").takeIf { it.isNotBlank() },
                        coords = coords,
                        swipeEndCoords = swipeEnd,
                        delayMs = stepObj.optLong("delay_ms", 500L)
                    )
                )
            }

            Result.success(
                AgentWorkflow(
                    id = id,
                    title = title,
                    description = desc,
                    targetPackage = targetPkg,
                    steps = stepsList,
                    isLooping = isLooping
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCoords(str: String?): Pair<Float, Float>? {
        if (str.isNullOrBlank()) return null
        val cleaned = str.replace("[", "").replace("]", "").trim()
        val parts = cleaned.split(",")
        if (parts.size >= 2) {
            val x = parts[0].trim().toFloatOrNull()
            val y = parts[1].trim().toFloatOrNull()
            if (x != null && y != null) return Pair(x, y)
        }
        return null
    }

    private fun escapeYaml(s: String): String {
        return s.replace("\"", "\\\"")
    }

    private fun unquote(s: String): String {
        var str = s.trim()
        if (str.startsWith("\"") && str.endsWith("\"") && str.length >= 2) {
            str = str.substring(1, str.length - 1)
        } else if (str.startsWith("'") && str.endsWith("'") && str.length >= 2) {
            str = str.substring(1, str.length - 1)
        }
        return str
    }
}
