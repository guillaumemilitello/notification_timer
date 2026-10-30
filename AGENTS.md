# Agent Guidelines for NotificationTimer 2.0

## Purpose
This document provides rules, standards, and workflow instructions for AI agents working on NotificationTimer 2.0. For the full functional specification, technical details, and phase roadmap, see [PLAN.md](PLAN.md).

## Module Map and Dependency Rules
NotificationTimer 2.0 uses a modular Jetpack Compose architecture:
- `:app`: Application entry point, `MainActivity`, navigation, and Hilt dependency injection.
- `:feature:timers`: Main timer screens, presets strip, timer pager, timer page, and "+" page.
- `:feature:presets`: Preset editor wizard, numpad, and preset management UI.
- `:feature:settings`: Settings UI, import/export UI, and "How it works" guidance.
- `:core:ui`: Theme, fonts (Recursive), custom `TextAutoSize` modifier, and shared UI components.
- `:core:domain`: Pure Kotlin domain models, state machine, event planner, `TimerEngine`, and ports.
- `:core:data`: DataStore persistence (`library.json`, `runtime.json`), JSON codec, import/export repositories.
- `:core:runtime`: Android runtime implementations (Foreground Service, Notification Renderer, `WakeupScheduler`, `AlertPlayer`, Broadcast Receivers).
- `:core:testing`: Test rules, fake clocks, in-memory stores, and fake adapters.

### Dependency Strict Rules:
1. Feature modules (`:feature:*`) depend **only** on `:core:domain` and `:core:ui`.
2. `:core:domain` has **zero Android framework dependencies**.
3. Only `:app` sees all modules for DI assembly.

## Architecture Invariants
Every code modification must preserve the following invariants:
1. **State Ownership**: Only `:core:domain`'s state machine changes timer state. UI, receivers, and services only send commands.
2. **Time Sources**: Timer calculations must exclusively use `TimeSource`, backed by `SystemClock.elapsedRealtime()`. Never use `System.nanoTime()`, Kotlin's `TimeSource.Monotonic`, or `uptimeMillis`.
3. **No Ticks Counter**: Never store or count ticks. Always compute remaining time from stored timestamps.
4. **State Persistence**: Save on every state transition, not on periodic ticks. State after process kill must match state before.
5. **Single-threaded Engine**: `TimerEngine` processes commands, wake-ups, and events strictly one at a time.
6. **Explicit PendingIntents**: All `PendingIntent`s must be explicit, immutable (`FLAG_IMMUTABLE`), and derive IDs/URIs deterministically from `TimerId` or `PresetId`.
7. **No UI Logic**: Composables must remain purely presentational. A composable branching on timer state beyond rendering indicates a domain leak.

## Common Developer Commands
- Run lint and all unit tests:
  ```bash
  ./gradlew check
  ```
- Run tests for a specific module (e.g., `:core:domain`):
  ```bash
  ./gradlew :core:domain:test
  ```
- Apply code formatting:
  ```bash
  ./gradlew spotlessApply
  ```

## Code Conventions
- **Kotlin Style**: Idiomatic Kotlin, explicit return types on public APIs.
- **State Exposure**: Domain and ViewModels expose UI state strictly via `StateFlow` or `Readonly` flows.
- **Error Handling**: Cross-module operations return sealed types or `Result<T>` instead of throwing exceptions.
- **Composables**: Pure, stateless functions receiving state and emitting events up.

## Definition of Done
A task or phase item is complete when:
1. Unit or integration tests covering the functionality are written first and pass.
2. `./gradlew check` runs clean without warnings or errors.
3. No architectural invariants are broken.
4. All user-facing strings are present in English, French, and Spanish.
5. The corresponding checkbox in `PLAN.md` is checked.

## Gotchas & Platform Log
*(Record platform quirks, OEM background restrictions, or flaky test patterns here as encountered during implementation)*
- **Android 15 Exact Alarms**: `USE_EXACT_ALARM` requires Doze-aware wake-lock handling if alerts are scheduled within 60 seconds of each other.
