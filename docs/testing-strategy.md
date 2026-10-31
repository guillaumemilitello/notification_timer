# Testing Strategy & Test Suggestions for NotificationTimer 2.0

This document outlines the testing strategy, current coverage, test suggestions, and automated quality gates for NotificationTimer 2.0.

---

## 1. Test Architecture Overview

NotificationTimer 2.0 uses a test pyramid suited for modern multi-module Android apps:

1. **JVM Unit Tests (`:core:domain`)**:
   - Fast, pure Kotlin tests with zero Android framework dependencies.
   - Powered by JUnit 4, `kotlinx-coroutines-test`, CashApp Turbine, and Kotest Property testing.
2. **Robolectric & Integration Tests (`:core:runtime`, `:core:data`)**:
   - Emulate Android framework components (DataStore, AlarmManager, BroadcastReceivers, NotificationManager / RemoteViews) on JVM.
3. **Compose UI Tests (`:feature:*`, `:app`)**:
   - Verify UI component behavior, state restoration, and auto-sizing typography under various screen dimensions.
4. **End-to-End System Tests (Maestro & UI Automator)**:
   - Validate full user journeys: timer creation, interactive notifications, notification swiping, and multi-timer background persistence.

---

## 2. Existing Test Coverage

* **`:core:domain`**:
  - `TimerStateMachineTest`: State transitions (Ready, Running, Paused, Completed), action permissions (`actionsFor`), time adjustments.
  - `EventPlannerTest`: Alert scheduling, threshold planning, and next-event computation.
  - `TimerEngineTest`: Command dispatch queue, single-threaded execution, reconciliation on restart.
* **`:core:data`**:
  - `DataStoreStoreTest`: DataStore serialization, schema integrity, and fallback handling.
* **`:core:runtime`**:
  - `NotificationRendererTest`: Notification channels, layout variants (black/white/red), progress bar synchronization, and PendingIntent generation.
  - `TimerNotifierIsolationTest`: Notification isolation across multiple active timers, foreground service switching, dismiss handling.
* **`maestro/`**:
  - `timer_basics.yaml`: Creation of a 1-minute timer from numpad, start, pause, and clear.

---

## 3. Recommended New Tests & Implementation Roadmap

### A. Unit Tests (High Priority)

1. **BackupRepository & JSON Serialization (`:core:data`)**:
   - **Target**: `BackupRepository.kt`, `JsonCodec.kt`.
   - **Scenarios to test**:
     - Export library to JSON and import back (round-trip test).
     - Import corrupted or malformed JSON (verify fallback/error reporting without crashing).
     - Schema version migrations: importing v1.x legacy data or older 2.0 schema versions.
2. **AlertPlayer Audio & Vibration Unit / Robolectric Tests (`:core:runtime`)**:
   - **Target**: `AlertPlayerImpl.kt`.
   - **Scenarios to test**:
     - Verify wake lock acquisition and release during alert playback.
     - Audio focus request/abandon behavior when multiple alerts fire in sequence.
     - Silent/Do Not Disturb mode handling.
3. **WakeupScheduler Timing & Alarm Tests (`:core:runtime`)**:
   - **Target**: `WakeupSchedulerImpl.kt`.
   - **Scenarios to test**:
     - Scheduling exact alarms with `ShadowAlarmManager`.
     - Wake-up collapsing when events are scheduled within less than 2 seconds.
     - Cancellation of alarms when all timers are paused or cleared.
4. **ViewModels Unit Tests (`:feature:timers`, `:feature:presets`, `:feature:settings`)**:
   - **Target**: Feature ViewModels.
   - **Scenarios to test**:
     - State exposure via `StateFlow` with Turbine.
     - Numpad button presses updating the formatted duration buffer (e.g. `1` -> `00:00:01`, `100` -> `00:01:00`).
     - Validation when creating presets with invalid durations (0s or exceeding 99:59:59).

### B. Compose UI Tests (Robolectric / AndroidTest)

1. **Timer Page Auto-Sizing Text**:
   - Verify `TextAutoSize` scales digits without clipping on small, medium, and large window sizes (400dp, 600dp, 900dp).
2. **Presets Strip Horizontal Scroll & Selection**:
   - Verify active timer chip highlights in the phase color of the most urgent timer.
   - Verify tapping an active preset chip jumps to that timer's page in the pager.
3. **Timer Settings & Preset Island Dialog**:
   - Test floating center dialog dismissal (tapping outside or pressing cancel).
   - Test updating timer set count or ±time step values directly from the UI.

### C. Maestro Integration & End-to-End Tests

1. **Multi-Timer Concurrency (`maestro/multi_timer.yaml`)**:
   - Launch app -> Create Timer 1 (1 min) -> Start.
   - Add Timer 2 (3 min) from preset -> Start.
   - Verify both timers run concurrently and pager displays both tabs.
   - Verify top bar chips show both timers active.
2. **Notification Shade Interaction (`maestro/notification_actions.yaml`)**:
   - Start a timer in app -> Send app to background / Home.
   - Open notification shade via Maestro `openNotification`.
   - Tap "Pause" action in the notification -> Verify state changes to Paused.
   - Tap "Resume" -> Verify countdown resumes.
   - Tap "Clear" -> Verify notification is removed.
3. **Preset Creation Wizard (`maestro/preset_wizard.yaml`)**:
   - Open "+" page -> Tap "New Preset".
   - Enter name "HIIT Workout", duration "00:45", sets "4".
   - Add an alert at "00:10" before end.
   - Save preset -> Verify new preset chip appears in the strip.
4. **Settings & JSON Backup Flow (`maestro/backup_restore.yaml`)**:
   - Open Settings screen -> Verify toggles (Keep Screen On, Sound & Vibration).
   - Test export / import flow triggers SAF without error.

---

## 4. Automated Quality Gate

The quality gate ensures no regressions in code formatting, static analysis, unit tests, or build scripts before committing or merging:

```bash
# Windows
.\scripts\gate.ps1

# Linux / macOS
./scripts/gate.sh
```

Flags:
- `-RunMaestro` / `--maestro`: Additionally executes Maestro end-to-end tests against any connected Android device or emulator.
