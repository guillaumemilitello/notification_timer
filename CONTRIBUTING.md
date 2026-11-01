# CONTRIBUTING

Rules for contributing to NotificationTimer 2.0.

## Workflow
- Branch from `feature/2.0`.
- Small, focused commits using conventional format (`feat:`, `fix:`, `test:`, `docs:`).
- Zero AI slop in code and docs. Direct, concise, imperative bullet points only.

## Architecture Rules
- Read [AGENT.md](AGENT.md) and [docs/architecture.md](docs/architecture.md).
- `:core:domain` is pure Kotlin. Zero Android imports.
- Only `:core:domain` state machine modifies timer state.
- Features depend only on `:core:domain` and `:core:ui`.
- Compute remaining time from timestamps. Never store ticks.

## Warnings Policy
- Zero warnings by default. `allWarningsAsErrors = true` enforced on all modules.
- Never suppress warnings globally.
- Scoped suppressions allowed only with explanatory comment:
  - `@OptIn`: experimental Compose/Kotlin APIs.
  - `@Suppress("DEPRECATION")`: SDK backward-compatibility branches.
  - `@Suppress("FunctionName")`: PascalCase Compose composables.
  - `@SuppressLint("MissingPermission")`: permission verified at outer boundary.

## Logging Guidelines
- Inject and use `EventLog` (`d`, `i`, `w`, `e`). Never use raw `android.util.Log`.
- R8 strips `d` and `i` in Release builds (`-assumenosideeffects`). `w` and `e` stay.
- Log state transitions, command processing, and startup hydration in `:core:domain`.
- Log alarm scheduling, wake locks, service lifecycle, and notifications in `:core:runtime`.
- Log DataStore persistence, corruption recovery, and audio focus events.
- Never log in 30fps/32ms UI tickers, tight tick loops, or unmasked PII.

## Testing & Gate (Mandatory)
Every code change must pass validation before commit:
- **Unit tests**:
  - Add or update tests for changed logic. Pure Kotlin for domain, Robolectric for runtime/data, Turbine for ViewModels.
  - Run: `./gradlew test`
- **Maestro E2E tests**:
  - Add or update Maestro flow if UI or notification shade touched.
  - Run: `maestro test maestro/<flow>.yaml` (tag WIP flows with `tags: [wip]`).
- **Quality gate**:
  - Run before commit: `.\scripts\gate.ps1` (Windows) or `./scripts/gate.sh` (Linux/Mac).
  - Gate checks: `spotlessCheck`, `detekt`, `test`, `check`.
  - Fix format issues: `./gradlew spotlessApply`.

## Definition of Done
- [ ] Unit tests pass (`./gradlew test`).
- [ ] Maestro E2E test added or updated if UI/notification touched.
- [ ] Gate passes clean with zero warnings, zero errors (`.\scripts\gate.ps1` or `./scripts/gate.sh`).
- [ ] Zero architecture invariants broken.
- [ ] Suppressions follow policy (scoped, commented, approved category only).
- [ ] Logging uses `EventLog` (no raw `Log`, no ticker logs).
- [ ] Strings localized in `values/strings.xml`, `values-fr/strings.xml`, `values-es/strings.xml`.
