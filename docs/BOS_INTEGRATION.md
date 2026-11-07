# Blacksmith Organization System (BOS) Integration

Mobile Croaker is architected to conform with the standards and conventions of the **Blacksmith Organization System (BOS)** and the **RavensArmy** operational framework.

---

## 1. Principles of BOS in Mobile Croaker

The Blacksmith Organization System emphasizes modularity, reproducible builds, explicit task automation, and externalized configuration over monolithic codebases.

| BOS Principle | Mobile Croaker Implementation |
|---|---|
| **Explicit Task Runners** | Root `Taskfile.yml` includes domain-specific taskfiles (`devops/Taskfile.yml`, `docs/Taskfile.yml`). |
| **Separation of Concerns** | Shell commands reside in executable `.sh` scripts under `scripts/devops/` rather than inline CI yaml blocks. |
| **Sanitized Assets** | No raw HTML/JS/CSS string concatenation inside Kotlin source code. Static web assets are stored in `assets/web/`. |
| **Declarative Workflows** | Automation is expressed as YAML pipelines (`assets/scenarios/`) with versioning, descriptions, and fallback logic. |
| **Standardized Identity** | Identities conform to BOS entity templates in Markdown, YAML, TOML, and CSV (`assets/identity/`). |

---

## 2. Directory Taxonomy

Conforming to BOS standards, the repository maintains the following structure:

```
mobile-croaker/
├── .github/workflows/          # CI/CD pipelines (dispatch only)
├── app/                        # Android application module
│   ├── src/main/assets/        # Externalized assets (sanitized, zero-injection)
│   │   ├── identity/           # BOS identity configurations
│   │   ├── openapi/            # OpenAPI 3.0 specification
│   │   ├── scenarios/          # Declarative YAML scenarios
│   │   └── web/                # Zero-inline HTML/CSS/JS dashboard
│   ├── src/main/java/          # Kotlin source files (MVVM, Clean Architecture)
│   └── src/main/res/           # Android resources & i18n string packages
├── docs/                       # Project documentation and guidebooks
│   ├── ARCHITECTURE.md
│   ├── BOS_INTEGRATION.md
│   ├── HARDWARE_INTEGRATIONS.md
│   ├── TUTORIAL.md
│   └── Taskfile.yml            # Documentation task delegation
├── scripts/                    # Automation scripts
│   └── devops/                 # CI/CD and deployment shell scripts
│       └── Taskfile.yml        # DevOps task delegation
└── Taskfile.yml                # Main BOS orchestrator
```

---

## 3. Extending BOS Scenarios

To add a new capability or scenario following BOS standards:
1. Create a `.yaml` specification in `app/src/main/assets/scenarios/<scenario_name>.yaml`.
2. Add the scenario ID to `app/src/main/assets/scenarios/index.json` or list via `AssetHelper`.
3. Provide unit or instrumented tests in `app/src/test/java/`.
4. Run validation via `task test`.
