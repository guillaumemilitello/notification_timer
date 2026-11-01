# CONTRIBUTING

Rules for contributing to NotificationTimer 2.0.

## Workflow
- Branch from `feature/2.0`.
- Keep commits small and focused.
- Use conventional commit messages: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`.
- Zero AI slop in code or docs. Direct, concise, bullet points.

## Architecture Rules
- Read [AGENT.md](AGENT.md) and [docs/architecture.md](docs/architecture.md).
- `:core:domain` is pure Kotlin. No Android imports.
- Only `:core:domain` state machine modifies timer state.
- Features depend only on `:core:domain` and `:core:ui`.
- Compute remaining time from timestamps. Never store ticks.

## Logging Guidelines
- **How to log**:
  - Always inject and use `EventLog` (`d`, `i`, `w`, `e`).
  - **Never use raw `android.util.Log`**: violates pure Kotlin domain rule, bypasses in-memory event buffer, and breaks JVM unit tests.
  - Release builds strip `d` and `i` logs automatically via R8 (`-assumenosideeffects`). `w` and `e` are retained.
- **When to log**:
  - State machine transitions, command submissions, and startup restoration.
  - Background lifecycle: `AlarmManager` wakeup arm/fire, `WakeLock` acquire/release.
  - `ForegroundService` start/stop and `NotificationRenderer` post/cancel.
  - `DataStore` save/load, corrupted file recovery, and backup import/export.
  - Audio focus grants/losses and alert playback.
- **When NOT to log**:
  - Never log inside 30fps/32ms Compose ticker loops or composition passes.
  - Never log in tight timestamp/tick calculation loops.
  - Never log unmasked user data or PII.

## Testing & Gate (Mandatory)
Every code change must pass validation before commit:
- **Unit tests**:
  - Add or update tests for any changed logic.
  - Run: `./gradlew test`
- **Maestro E2E tests**:
  - Add or update Maestro flow if UI or notification shade flow touched.
  - Run: `maestro test maestro/<flow>.yaml`
  - Tag in-progress flows with `tags: [wip]`.
- **Quality gate**:
  - Windows: `.\scripts\gate.ps1`
  - Linux/Mac: `./scripts/gate.sh`
  - Gate checks: `spotlessCheck`, `detekt`, `test`, `check`.
  - Fix format issues: `./gradlew spotlessApply`.
- **Code coverage**:
  - Deferred for now. Will be enforced in a future phase.

## Definition of Done
- [ ] Unit tests pass (`./gradlew test`).
- [ ] Maestro E2E test added or updated if UI/notification affected.
- [ ] Quality gate passes clean with zero errors (`.\scripts\gate.ps1` or `./scripts/gate.sh`).
- [ ] No architecture invariants broken.
- [ ] Logging uses `EventLog` (no raw `android.util.Log`; no logs in high-frequency loops).
- [ ] Strings localized in `values/strings.xml`, `values-fr/strings.xml`, `values-es/strings.xml`.
