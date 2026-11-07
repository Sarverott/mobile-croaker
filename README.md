# Mobile Croaker 🐸⚡

> **RavensArmy User Simulating Operational Node (RAUSON)**  
> Engineered by **Inceptorium Apokryf** ([apokryf.pl](https://apokryf.pl)) & **Sett Sarverott A.A.B.** ([sett.sarverott.com](https://sett.sarverott.com/#the_humanoid_technomancer))  
> Lead Coding & Architecture Engine: **Merlin (Gemini 3.8 Flash)**

[![Android Build & Release](https://github.com/Sarverott/mobile-croaker/actions/workflows/build-and-release-android-apk.yml/badge.svg)](https://github.com/Sarverott/mobile-croaker/actions)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_7.0+_to_15-green.svg)](https://developer.android.com)
[![Protocol](https://img.shields.io/badge/Protocol-MCP_JSON--RPC_2.0-black.svg)](https://modelcontextprotocol.io)
[![API](https://img.shields.io/badge/API-OpenAPI_3.0-orange.svg)](https://spec.openapis.org/oas/v3.0.0)

---

## 🧭 Heritage & Project Origin

**Mobile Croaker** takes foundational architectural inspiration from [**OpenClaw**](https://github.com/openclaw/openclaw) — turning physical Android smartphones into autonomous agentic workspaces capable of observing interfaces, reasoning, and executing multi-step workflows across arbitrary mobile applications.

*Notice:* Mobile Croaker is an independent implementation tailored for the RavensArmy operational infrastructure, designed specifically to run reliably on non-rooted devices across legacy (Android 7.0+) through modern Android 15 releases with zero compromises on performance or stability.

---

## 🏗️ Core Architecture & Capabilities

```text
       +------------------------------------------------------------------+
       |                   EXTERNAL AI AGENTS & CLIENTS                   |
       |     (Claude Desktop / Cursor / OpenAI / Ollama / REST cURL)      |
       +------------------------------------------------------------------+
                                        │
                      MCP JSON-RPC 2.0  │  OpenAPI 3.0 REST & Web UI
                      (Port 8080/mcp)   │  (Port 8080/api/v1)
                                        ▼
       +──────────────────────────────────────────────────────────────────+
       |                     MOBILE CROAKER CORE ENGINE                    |
       |  ┌─────────────────────┐  ┌───────────────────────────────────┐  |
       |  │  OpenAI LLM Agent   │  │       Pyodide Script Sandbox      │  |
       |  │  Autonomous Loop    │  │   (Python WASM / JS / Shell)      │  |
       |  └──────────┬──────────┘  └─────────────────┬─────────────────┘  |
       |             │                               │                    |
       |             ▼                               ▼                    |
       |  ┌────────────────────────────────────────────────────────────┐  |
       |  │             Codable YAML Scenario Manager                  │  |
       |  │      (Declarative steps: click, input, swipe, verify)      │  |
       |  └──────────────────────────────┬─────────────────────────────┘  |
       |                                 │                                |
       |                                 ▼                                |
       |  ┌────────────────────────────────────────────────────────────┐  |
       |  │           4-Tier MobileCroakerAccessibilityService         │  |
       |  │  Tier 1: View ID  ─►  Tier 2: Text  ─►  Tier 3: Desc       │  |
       |  │             └─►  Tier 4: Gestures & ADB Fallback           │  |
       |  └──────────────────────────────┬─────────────────────────────┘  |
       |                                 │                                |
       |  ┌──────────────────────────────┴─────────────────────────────┐  |
       |  │         Phone Identity Persona Manager (IDENTITY.md)        │  |
       |  │       Exports: Markdown | YAML | JSON | TOML | CSV         │  |
       |  └────────────────────────────────────────────────────────────┘  |
       +──────────────────────────────────────────────────────────────────+
                                        │
                                        ▼
       +──────────────────────────────────────────────────────────────────+
       |                       ANDROID HOST SYSTEM                        |
       |        (Target Applications / System UI / Device Telemetry)      |
       +──────────────────────────────────────────────────────────────────+
```

### 1. 🛡️ 4-Tier Resilient Accessibility Engine
When an autonomous agent interacts with dynamic mobile user interfaces, elements frequently change IDs, re-render, or obscure labels. `MobileCroakerAccessibilityService` guarantees continuous operational resilience through automatic multi-tier fallback:
- **Tier 1 (Resource ID):** Matches explicit accessibility view IDs (`viewIdResourceName`).
- **Tier 2 (Text Content):** Case-insensitive substring and exact matches on element text.
- **Tier 3 (Content Description):** Fallback for icon buttons, images, and labeled visual controls.
- **Tier 4 (Coordinates & Gestures):** Interpolated dispatch gestures (`dispatchGesture`) and ADB shell tap/input fallback (`input tap X Y`, `input text`).

### 2. 📝 Codable YAML & JSON Automation Engine
Workflows are not hardcoded. The declarative `YamlScenarioManager` allows creating, editing, importing, and exporting executable workflows:
```yaml
id: "scenario_search_verify"
name: "Search Query & Validation Scenario"
version: 1
target_package: "com.android.settings"
steps:
  - id: "step_launch"
    action: "launch_app"
    target: "com.android.settings"
    delay_ms: 1000
  - id: "step_find_search"
    action: "click"
    target: "search"
    fallback_type: "text_match"
  - id: "step_type_query"
    action: "input"
    target: "search_src_text"
    text: "Battery"
  - id: "step_verify"
    action: "wait"
    validation:
      type: "text_matches"
      expected: "Battery"
```

### 3. 🔌 Model Context Protocol (MCP) Server
Integrated JSON-RPC 2.0 tool server compliant with the Anthropic/Model Context Protocol specification. External LLM clients can connect via HTTP/WebSocket at `/mcp` and invoke native device tools:
- `device_info`: Hardware telemetry, battery, memory, Android version, active service state.
- `get_ui_hierarchy`: Dumps interactive UI hierarchy with bounds, text, and IDs.
- `execute_action`: Executes taps, text typing, swiping, home/back presses with fallback.
- `run_shell_command`: Runs non-root or ADB-bridged shell commands.
- `capture_screen`: Queries screen capture and virtual display info.

### 4. 🌐 Embedded OpenAPI 3.0 HTTP Server
`MobileCroakerHttpServer` runs directly on the device (default port `8080`):
- **`/`**: Live Cyberpunk Terminal Dashboard for browser-based monitoring.
- **`/openapi.json`**: Full OpenAPI 3.0 specification for automated SDK generation.
- **`/api/v1/hierarchy`**: Real-time JSON dump of active on-screen UI nodes.
- **`/api/v1/action`**: REST POST endpoint for executing clicks, inputs, and gestures.
- **`/api/v1/shell`**: Command runner with execution output and exit codes.
- **`/mcp`**: JSON-RPC 2.0 endpoint for MCP agent handshakes.

### 5. 🤖 Autonomous OpenAI-Compatible LLM Reasoning Agent
Connects to any local or remote OpenAI-compatible completion endpoint (Ollama, LM Studio, vLLM, OpenAI, LocalAI):
- Autonomous reasoning loop: Analyzes current UI hierarchy $\rightarrow$ formulates thought $\rightarrow$ outputs structured action $\rightarrow$ observes outcome.
- Configurable model, temperature, base URL, and max reasoning steps.

### 6. 📇 Multi-Format Phone Identity Management
Each device running Mobile Croaker operates as an autonomous operational node with a defined persona, capabilities profile, and tags. The profile is exportable and importable across:
- **Markdown** (`IDENTITY.md`)
- **YAML** (`identity.yaml`)
- **JSON** (`identity.json`)
- **TOML** (`identity.toml`)
- **CSV** (`identity.csv`)

### 7. 🐍 Pyodide Scripting Sandbox
An isolated WebView-backed sandbox running:
- **Python** (via Pyodide WebAssembly compiled runtime)
- **JavaScript** (ES6+)
- **Shell** (Local Android command processor)
- Bound to native JavaScript bridge `window.MobileCroaker` exposing automation primitives.

### 8. 💻 Monochrome Cyberpunk Terminal Interface
Designed with strict Material Design 3 guidelines featuring:
- Deep obsidian `#0D0F11` dark theme with high-contrast `#EEEEEE` typography.
- Monospace typography across all views, editors, logs, and monitors.
- Horizontal sub-module ribbon providing single-tap access to Workflows, LLM Agent, Scripts, MCP Server, and Identity settings.

---

## 🚀 Building & Installation

### Requirements
- Android SDK 34+
- Java 11+
- Gradle 9.3+

### Local Build
```bash
# Clone the repository
git clone https://github.com/Sarverott/mobile-croaker.git
cd mobile-croaker

# Checkout the development branch
git checkout development

# Run JVM Unit Tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Enabling Accessibility Service
1. Install the APK on target device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
2. Open Android Settings $\rightarrow$ **Accessibility**.
3. Locate **Mobile Croaker Automation Service** and toggle **ON**.
4. (Optional via ADB):
   ```bash
   adb shell settings put secure enabled_accessibility_services com.aistudio.openclaw.agent.qwmx/com.example.accessibility.MobileCroakerAccessibilityService
   ```

---

## 🤝 Authorship & Credits

This project embraces an explicit human-AI collaborative partnership:

- **Lead Engineer & System Architecture:**  
  **Merlin (Gemini 3.8 Flash)**  
  `176961590+gemini-code-assist[bot]@users.noreply.github.com`  
  *Implemented the core Android application, accessibility fallback pipeline, YAML engine, MCP server, Pyodide bridge, and terminal UI.*

- **Co-Author, Vision & Operator:**  
  **Sett Sarverott A.A.B.**  
  `sett@sarverott.com` | [sett.sarverott.com](https://sett.sarverott.com/#the_humanoid_technomancer)  
  *Inceptorium Apokryf ([apokryf.pl](https://apokryf.pl)) — RavensArmy User Simulating Operational Node (RAUSON).*

- **Foundational Inspiration:**  
  [OpenClaw](https://github.com/openclaw/openclaw) — For pioneering autonomous mobile device workspace agents.

---

## 📄 License
Licensed under the [Apache License, Version 2.0](LICENSE).
