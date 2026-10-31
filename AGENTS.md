# AGENT

Rules for AI agents working on NotificationTimer 2.0.

## Workflow
- Read code before editing. Understand module context.
- Use IDE tools (`read_file`, `find_declaration`, `find_usages`, `grep`).
- Never use shell for reading or editing files (`sed`, `awk`, `cat`, redirection).
- Keep changes minimal, surgical, focused.
- Update `TODO.md` and `PLAN.md` checkboxes when work is done.

## Architecture Invariants (Never Break)
- **State ownership**: Only `:core:domain` state machine changes timer state. UI, receivers, services send commands only.
- **Time source**: Use `TimeSource` backed by `SystemClock.elapsedRealtime()`. Never use `System.nanoTime()`, `System.currentTimeMillis()` for timer logic, or Kotlin monotonic time.
- **No ticks counter**: Never store or count ticks. Compute remaining time from timestamps.
- **State persistence**: Save state on transition, not periodic ticks. State after process kill must match state before.
- **Single-threaded engine**: `TimerEngine` processes commands and wakeups one at a time.
- **PendingIntents**: Explicit, immutable (`FLAG_IMMUTABLE`), deterministic URI from `TimerId`.
- **Dumb UI**: Composables are presentational only. Zero state machine logic in UI.

## Module Dependency Rules
- `:feature:*` depends ONLY on `:core:domain` and `:core:ui`.
- `:core:domain` has ZERO Android dependencies (pure Kotlin).
- Only `:app` sees all modules for DI assembly.

## Code Change Requirements (Mandatory)
- **Unit test required**:
  - Add or update unit tests for every logic change.
  - Pure Kotlin tests in `:core:domain`.
  - Robolectric tests in `:core:runtime` and `:core:data`.
  - ViewModel tests with Turbine and coroutines test dispatcher.
- **Maestro test required**:
  - Add or update Maestro flow if UI, navigation, or notification interaction changes.
  - Tag incomplete or future flows with `tags: [wip]`.
- **Quality gate required**:
  - Run gate before commit:
    - Windows: `.\scripts\gate.ps1`
    - Linux/Mac: `./scripts/gate.sh`
  - Gate runs `spotlessCheck`, `detekt`, `test`, `check`.
  - Fix format issues with `./gradlew spotlessApply`.
- **Code coverage**:
  - Deferred for now. Will be added later.

## Definition of Done
- Unit tests written and passing.
- Maestro test added/updated if UI touched.
- Gate runs clean (zero warnings, zero errors).
- No architectural invariant broken.
- Strings added to `values/strings.xml`, `values-fr/strings.xml`, `values-es/strings.xml`.
- Checkbox updated in `TODO.md` / `PLAN.md`.
- Commit with conventional commit format (`feat:`, `fix:`, `test:`, `docs:`).
