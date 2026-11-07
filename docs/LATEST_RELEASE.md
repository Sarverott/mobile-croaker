# Mobile Croaker Release Notes

## Version 1.0.0 (RAUSON Node Initialization)

### 🌟 New Features & Capabilities
- **Mobile Croaker Core Engine**: 4-tier resilient accessibility service (`MobileCroakerAccessibilityService`) providing zero-crash UI automation on rooted and non-rooted Android 7.0–15 devices.
- **BOS Standards Alignment**: Conformance to Blacksmith Organization System (BOS) principles with modular architecture, clean Taskfile automation, and externalized assets.
- **XSS & Injection Elimination**: Replaced in-code HTML/JS string interpolations with dedicated assets (`assets/web/dashboard.html`, `assets/scripting/pyodide_runner.html`, `assets/openapi/openapi.json`) and Base64-isolated script execution.
- **Model Context Protocol (MCP)**: Embedded JSON-RPC 2.0 tool server on port `8080/mcp` for Claude, Cursor, OpenAI, and Gemini orchestration.
- **OpenAPI 3.0 REST Gateway**: Embedded HTTP server with live cyberpunk terminal dashboard, telemetry endpoints, and streaming support.
- **Declarative YAML & JSON Automation**: Codable workflow engine with validation assertions (`exists`, `text_matches`, `package_equals`).
- **Autonomous OpenAI-Compatible LLM Reasoning**: Multi-step perception-reasoning-action loop for local/remote LLMs.
- **Multi-Format Device Identity**: Node persona exportable in Markdown (`IDENTITY.md`), YAML, JSON, TOML, and CSV.
- **Pyodide WebAssembly Python Sandbox**: Run Python 3.12 automation scripts locally within a sandboxed WebView bridge.

### 🛡️ Security & Quality
- Elimination of ad-hoc string concatenation for scripting commands.
- 100% JVM unit test pass rate across scenario parser, identity serialization, and MCP protocol handlers.
- Automated GitHub Actions release pipeline building signed APK artifacts.

---
*Co-Authored by Merlin (Gemini 3.8 Flash) & Sett Sarverott A.A.B. (Inceptorium Apokryf)*
