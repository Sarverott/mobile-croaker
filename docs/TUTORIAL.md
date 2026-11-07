# Mobile Croaker Tutorial & User Guide

Welcome to **Mobile Croaker** (RAUSON - *RavensArmy User Simulating Operational Node*). Mobile Croaker turns any physical Android smartphone into a headless or interactive robotic workspace for AI agents, external automation, and edge LLMs.

---

## 1. Quick Start

### 1.1 Requirements
- Android device running Android 5.0+ (Lollipop) up to Android 15+.
- Developer Options & USB Debugging enabled (for ADB fallback / WiFi debugging).
- Accessibility Service enabled for Mobile Croaker in Android Settings.

### 1.2 Initial Setup on Device
1. Install the APK built from the release workflow or run `./gradlew installDebug`.
2. Open the **Mobile Croaker** app.
3. Grant required runtime permissions:
   - **Accessibility Service**: Tap "Open Accessibility Settings" and enable **Mobile Croaker Accessibility Service**.
   - **Permissions**: Grant Camera (flashlight/vision), Location, Contacts, Audio recording, SMS, and Phone permissions as needed.
4. Go to the **Services** tab:
   - Toggle **HTTP & REST Server** (default port: `8080`).
   - Toggle **MCP Server** (default port: `8081`).
   - Note your device IP address shown in the dashboard (e.g., `192.168.1.150`).

---

## 2. Connecting AI Agents via MCP (Model Context Protocol)

Mobile Croaker provides a built-in MCP server supporting JSON-RPC 2.0 over SSE / HTTP or direct socket on port `8081`.

### 2.1 Claude Desktop Configuration
Add the Mobile Croaker MCP configuration to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "mobile-croaker": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "http://<PHONE_IP>:8081/mcp/sse"
      ]
    }
  }
}
```

Or connect directly via standard HTTP POST / WebSocket bridge.

### 2.2 Available MCP Tools

| MCP Tool Name | Description | Key Arguments |
|---|---|---|
| `mobile_croaker_get_ui_tree` | Dumps the active Android UI hierarchy with node IDs, bounds, texts, and classes. | None |
| `mobile_croaker_click_element` | Clicks a specific UI element by text, resource ID, or content description. | `target`, `searchBy` |
| `mobile_croaker_input_text` | Inputs text into a target input field or clipboard paste fallback. | `target`, `text`, `searchBy` |
| `mobile_croaker_execute_global_action` | Triggers Android system navigation actions. | `action`: `BACK`, `HOME`, `RECENTS`, `NOTIFICATIONS`, `QUICK_SETTINGS`, `POWER_DIALOG`, `SPLIT_SCREEN` |
| `mobile_croaker_execute_yaml_scenario` | Executes a declarative multi-step YAML automation workflow. | `yaml_content` |
| `mobile_croaker_manage_clipboard` | Reads or writes device clipboard text. | `action` (`get`/`set`), `text` |
| `mobile_croaker_hardware_control` | Controls hardware peripherals (Torch, Audio volume, WiFi, Location). | `subsystem`, `action`, `value` |
| `mobile_croaker_telephony_sms` | Telephony tools for SMS sending and dialer triggering. | `action` (`send_sms`/`dial`), `phone_number`, `message` |
| `mobile_croaker_device_buttons` | Triggers physical hardware buttons (Volume, Lock, Power, Screenshot). | `button` (`VOLUME_UP`, `VOLUME_DOWN`, `LOCK_SCREEN`, `TAKE_SCREENSHOT`, `POWER_DIALOG`) |

---

## 3. Writing and Running YAML Automation Scenarios

Workflows in Mobile Croaker are defined declaratively in clean, readable YAML files.

### 3.1 YAML Scenario Structure
```yaml
id: open_and_search_settings
name: "Search Android Settings"
version: 1.0.0
description: "Opens Android Settings, searches for Display, and inspects results"
author: "RAUSON Agent"

steps:
  - id: step_home
    action: home
    delay_ms: 500

  - id: step_launch
    action: launch_app
    package: "com.android.settings"
    delay_ms: 1200

  - id: step_click_search
    action: click
    target: "Search settings"
    search_by: "text"
    fallback:
      action: click
      target: "com.android.settings:id/search_action_bar"
      search_by: "resource_id"
    delay_ms: 600

  - id: step_type_query
    action: input_text
    target: "com.android.settings:id/search_src_text"
    search_by: "resource_id"
    text: "Display"
    delay_ms: 1000

  - id: step_capture
    action: dump_tree
```

### 3.2 Loading and Executing Scenarios
- **From App UI**: Navigate to the **Scenarios** tab, select a template from the dropdown (loaded dynamically from assets), edit or inspect in the terminal view, and tap **"RUN SCENARIO"**.
- **Via REST API**:
  ```bash
  curl -X POST http://<PHONE_IP>:8080/api/v1/workflow/execute \
    -H "Content-Type: application/x-yaml" \
    --data-binary @my_workflow.yaml
  ```
- **Via MCP**: Invoke `mobile_croaker_execute_yaml_scenario` with the YAML string.

---

## 4. Hardware Management & Telephony

Mobile Croaker exposes direct hardware bridge endpoints and MCP actions:
- **Clipboard Replacement**: Automatically bypasses cumbersome virtual keyboard typing by staging text into the system clipboard and dispatching `ACTION_PASTE`.
- **Flashlight / Torch**: Remotely illuminate dark testing chambers or trigger optical signaling.
- **Volume & Audio**: Adjust Alarm, Ring, and Media streams before and during automated voice calls or tests.
- **SMS & Calls**: Automated alerts, OTP extraction pipelines, or remote dial testing.
- **Physical Keys**: Trigger screenshots, lock the display, or invoke the system power dialog.

---

## 5. DevOps & Automation with Taskfile

Run commands locally or in CI using the modular `Taskfile.yml`:

```bash
# List all available tasks
task --list

# Run app tests
task test

# Build debug APK
task build:debug

# Build release APK via DevOps module
task devops:build

# Generate documentation index
task docs:build
```
