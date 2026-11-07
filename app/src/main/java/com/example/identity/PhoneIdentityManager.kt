package com.example.identity

import android.os.Build
import java.util.UUID

data class PhoneIdentity(
    val callsign: String = "RAUSON-Merlin-01",
    val operator: String = "Inceptorium Apokryf (apokryf.pl)",
    val unit: String = "RAUSON Operations (RA-SEE / UCDI / HAO / SEIAIC)",
    val nodeId: String = "node-" + UUID.randomUUID().toString().take(8),
    val role: String = "Autonomous UI Worker & Navigation Node",
    val inspiration: String = "Project OpenClaw (https://github.com/openclaw/openclaw)",
    val hardwareModel: String = try { Build.MODEL as String? } catch (_: Throwable) { null } ?: "Android Phone",
    val androidVersion: String = try { Build.VERSION.RELEASE as String? } catch (_: Throwable) { null } ?: "14",
    val rsyslogHost: String = "192.168.1.100:6514",
    val mcpPort: Int = 8080,
    val llmEndpoint: String = "http://192.168.1.100:11434/v1",
    val missionNotes: String = "Autonomous cross-app testing and interface automation. Operate securely on non-rooted hardware via 4-tier fallback stack."
)

enum class IdentityFormat {
    MARKDOWN,
    YAML,
    JSON,
    TOML,
    CSV
}

/**
 * Manages phone identity profiles and multi-format export/import
 * across Markdown, YAML, JSON, TOML, and CSV.
 */
object PhoneIdentityManager {

    fun export(identity: PhoneIdentity, format: IdentityFormat): String {
        return when (format) {
            IdentityFormat.MARKDOWN -> toMarkdown(identity)
            IdentityFormat.YAML -> toYaml(identity)
            IdentityFormat.JSON -> toJson(identity)
            IdentityFormat.TOML -> toToml(identity)
            IdentityFormat.CSV -> toCsv(identity)
        }
    }

    fun toMarkdown(id: PhoneIdentity): String {
        return """
            # MOBILE CROAKER // NODE IDENTITY CARD
            > **Project Inspiration:** [OpenClaw](https://github.com/openclaw/openclaw)
            > **Operator:** ${id.operator}
            > **Unit:** ${id.unit}
            
            ## Node Specifications
            | Property | Value |
            | :--- | :--- |
            | **Callsign** | `${id.callsign}` |
            | **Node UUID** | `${id.nodeId}` |
            | **Role** | ${id.role} |
            | **Device Hardware** | ${id.hardwareModel} |
            | **OS Version** | Android ${id.androidVersion} |
            
            ## Network & Tooling Configuration
            - **Encrypted Rsyslog Target:** `${id.rsyslogHost}`
            - **Model Context Protocol (MCP):** `port ${id.mcpPort}`
            - **OpenAI-Compatible LLM:** `${id.llmEndpoint}`
            
            ## Mission Operational Directive
            ${id.missionNotes}
        """.trimIndent()
    }

    fun toYaml(id: PhoneIdentity): String {
        return """
            # Mobile Croaker Node Identity
            callsign: "${id.callsign}"
            operator: "${id.operator}"
            unit: "${id.unit}"
            node_id: "${id.nodeId}"
            role: "${id.role}"
            inspiration: "${id.inspiration}"
            hardware_model: "${id.hardwareModel}"
            android_version: "${id.androidVersion}"
            network:
              rsyslog_host: "${id.rsyslogHost}"
              mcp_port: ${id.mcpPort}
              llm_endpoint: "${id.llmEndpoint}"
            mission_notes: "${id.missionNotes.replace("\"", "\\\"")}"
        """.trimIndent()
    }

    fun toJson(id: PhoneIdentity): String {
        return """
            {
              "callsign": "${id.callsign}",
              "operator": "${id.operator}",
              "unit": "${id.unit}",
              "nodeId": "${id.nodeId}",
              "role": "${id.role}",
              "inspiration": "${id.inspiration}",
              "hardwareModel": "${id.hardwareModel}",
              "androidVersion": "${id.androidVersion}",
              "network": {
                "rsyslogHost": "${id.rsyslogHost}",
                "mcpPort": ${id.mcpPort},
                "llmEndpoint": "${id.llmEndpoint}"
              },
              "missionNotes": "${id.missionNotes.replace("\"", "\\\"")}"
            }
        """.trimIndent()
    }

    fun toToml(id: PhoneIdentity): String {
        return """
            # Mobile Croaker TOML Configuration
            [node]
            callsign = "${id.callsign}"
            node_id = "${id.nodeId}"
            role = "${id.role}"
            operator = "${id.operator}"
            unit = "${id.unit}"
            inspiration = "${id.inspiration}"
            
            [device]
            hardware_model = "${id.hardwareModel}"
            android_version = "${id.androidVersion}"
            
            [network]
            rsyslog_host = "${id.rsyslogHost}"
            mcp_port = ${id.mcpPort}
            llm_endpoint = "${id.llmEndpoint}"
            
            [mission]
            notes = "${id.missionNotes}"
        """.trimIndent()
    }

    fun toCsv(id: PhoneIdentity): String {
        return """
            key,value
            callsign,"${id.callsign}"
            operator,"${id.operator}"
            unit,"${id.unit}"
            node_id,"${id.nodeId}"
            role,"${id.role}"
            hardware_model,"${id.hardwareModel}"
            android_version,"${id.androidVersion}"
            rsyslog_host,"${id.rsyslogHost}"
            mcp_port,"${id.mcpPort}"
            llm_endpoint,"${id.llmEndpoint}"
        """.trimIndent()
    }

    fun import(content: String, format: IdentityFormat): Result<PhoneIdentity> {
        return try {
            val trimmed = content.trim()
            if (trimmed.startsWith("{")) {
                val json = org.json.JSONObject(trimmed)
                return Result.success(
                    PhoneIdentity(
                        callsign = json.optString("callsign", "RAUSON-Node"),
                        operator = json.optString("operator", "Inceptorium Apokryf (apokryf.pl)"),
                        unit = json.optString("unit", "RAUSON"),
                        nodeId = json.optString("nodeId", UUID.randomUUID().toString()),
                        role = json.optString("role", "UI Worker"),
                        rsyslogHost = json.optJSONObject("network")?.optString("rsyslogHost") ?: "192.168.1.100:6514",
                        mcpPort = json.optJSONObject("network")?.optInt("mcpPort") ?: 8080,
                        llmEndpoint = json.optJSONObject("network")?.optString("llmEndpoint") ?: "http://192.168.1.100:11434/v1",
                        missionNotes = json.optString("missionNotes", "")
                    )
                )
            }
            // Key-value heuristic parse for yaml, toml, csv, md
            val map = mutableMapOf<String, String>()
            for (line in trimmed.lines()) {
                val clean = line.replace("|", "").replace("-", "").trim()
                val sep = if (clean.contains(":")) ":" else if (clean.contains("=")) "=" else if (clean.contains(",")) "," else null
                if (sep != null) {
                    val k = clean.substringBefore(sep).replace("**", "").replace("`", "").trim().lowercase()
                    val v = clean.substringAfter(sep).replace("**", "").replace("`", "").replace("\"", "").trim()
                    map[k] = v
                }
            }

            Result.success(
                PhoneIdentity(
                    callsign = map["callsign"] ?: "RAUSON-Worker",
                    operator = map["operator"] ?: "Inceptorium Apokryf (apokryf.pl)",
                    unit = map["unit"] ?: "RAUSON",
                    role = map["role"] ?: "Automation Node",
                    rsyslogHost = map["rsyslog_host"] ?: map["rsyslog target"] ?: "192.168.1.100:6514",
                    mcpPort = map["mcp_port"]?.toIntOrNull() ?: 8080,
                    llmEndpoint = map["llm_endpoint"] ?: "http://192.168.1.100:11434/v1"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
