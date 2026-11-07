# Mobile Croaker Architecture & Design

Mobile Croaker (RAUSON) is engineered as an autonomous agent runtime for Android devices, adhering to strict multi-tier fallback patterns, non-root accessibility automation, and clean asset separation.

---

## 1. System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       External Agents                       │
│    (Claude Desktop / Cursor MCP / REST / WebSocket / SSE)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌─────────────────────────┐           ┌─────────────────────────┐
│ MobileCroakerMcpServer  │           │ MobileCroakerHttpServer │
│      (Port 8081)        │           │       (Port 8080)       │
└───────────┬─────────────┘           └───────────┬─────────────┘
            │                                     │
            └──────────────────┬──────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 MobileCroakerAutomator                      │
│                 (Multi-Tier Automation)                     │
└──────┬───────────────────────┬───────────────────────┬──────┘
       │ Tier 1                │ Tier 2                │ Tier 3/4
       ▼                       ▼                       ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│ Accessibility │       │ ADB Bridge /  │       │ Computer      │
│    Service    │       │ Secure Shell  │       │ Vision & CV   │
└───────────────┘       └───────────────┘       └───────────────┘
```

---

## 2. Multi-Tier Automation Strategy

Mobile Croaker guarantees maximum reliability across legacy and modern Android devices using a 4-tier cascading execution pipeline:

1. **Tier 1: Android Accessibility Service (`MobileCroakerAccessibilityService`)**
   - Native event injection (`ACTION_CLICK`, `ACTION_SET_TEXT`, `ACTION_PASTE`, `ACTION_SCROLL_FORWARD`).
   - Non-intrusive, zero-root, battery-friendly.
   - Real-time window inspection via `AccessibilityNodeInfo` hierarchy parsing.

2. **Tier 2: Secure Shell & ADB Bridge (`AdbShellBridge`)**
   - Activated if Accessibility node lookup fails (e.g., custom WebViews, Unity games, Flutter canvases).
   - Issues `input tap X Y`, `input text STRING`, `input keyevent KEYCODE`.
   - Communicates through loopback ADB (`adb connect 127.0.0.1:5555`) or USB debugging.

3. **Tier 3: Computer Vision & Optical Coordinate Matching (`OpenCvVisionHelper`)**
   - Captures screen frames.
   - Performs template matching, edge detection, and visual anchor alignment.
   - Computes physical touch coordinates for Tiers 1 and 2 gestures.

4. **Tier 4: Remote Operator / Agent Notification**
   - Emits structured error events to the connected MCP client and remote rsyslog server.
   - Requests human or higher-level agent intervention.

---

## 3. Asset Externalization & Sanitization

To ensure security, maintainability, and clean codebases:
- **No Injected Scripts / HTML**: All dashboard HTML, CSS, JavaScript, and OpenAPI JSON files are bundled as static assets in `app/src/main/assets/web/` and `app/src/main/assets/openapi/`.
- **No Hardcoded Workflows**: Automation scenarios are saved as declarative YAML files in `app/src/main/assets/scenarios/`.
- **No Hardcoded Identity Blocks**: Identity profiles (Markdown, YAML, TOML, CSV) are externalized in `app/src/main/assets/identity/`.
- **Central Asset Loader**: `AssetHelper.loadAsset(context, path)` provides cached, sanitized loading without dynamic string concatenation or injection risks.

---

## 4. Internationalization (i18n)

- Central resource strings in `app/src/main/res/values/strings.xml`.
- Dedicated English localization in `app/src/main/res/values-en/strings.xml`.
- Zero hardcoded English strings in composable UI views; all labels, descriptions, and tab headers query `stringResource(R.string.*)`.
