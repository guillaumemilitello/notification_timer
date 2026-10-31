# NotificationTimer 2.0 ⏱️

A modern, multi-timer Android application designed for precision, background resilience, and customizable alerts. NotificationTimer 2.0 is a complete Kotlin & Jetpack Compose multi-module rewrite engineered for Android 15+ (minSdk 35, targetSdk 37).

Each timer operates independently with its own set of alerts (sound, vibration, phase color) and dedicated interactive notification with custom lock-screen controls.

---

## Architecture & Modules

The project is built following strict unidirectional data flow and clean architecture invariants:

```
                  ┌──────────────┐
                  │     :app     │ (Application entry point, DI assembly)
                  └──────┬───────┘
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
┌────────────────┐┌──────────────┐┌────────────────┐
│:feature:timers ││:feature:presets│:feature:settings│ (Compose UI Features)
└────────┬───────┘└──────┬───────┘└──────┬─────────┘
         │               │               │
         ├───────────────┼───────────────┤
         ▼                               ▼
  ┌─────────────┐                 ┌─────────────┐
  │  :core:ui   │                 │:core:domain │ (Pure Kotlin: Engine, State Machine)
  └─────────────┘                 └──────┬──────┘
                                         │
                         ┌───────────────┴───────────────┐
                         ▼                               ▼
                  ┌─────────────┐                 ┌─────────────┐
                  │ :core:data  │                 │:core:runtime│ (Service, Alarms, Notifications)
                  └─────────────┘                 └─────────────┘
```

* **`:app`**: Application entry point, `MainActivity`, navigation host, and Hilt root setup.
* **`:feature:timers`**: Main screen, horizontal timer pager, dynamic presets strip, and floating island dialogs.
* **`:feature:presets`**: Preset editor wizard, numpad input, and alert configuration.
* **`:feature:settings`**: Settings UI, JSON backup import/export, and "How it works" guide.
* **`:core:ui`**: Material 3 theming, Recursive variable font integration, and custom auto-sizing text components.
* **`:core:domain`**: Pure Kotlin business models, deterministic state machine, event scheduler, and `TimerEngine`. Zero Android framework dependencies.
* **`:core:data`**: DataStore persistence (`library.json`, `runtime.json`), JSON codecs, and SAF backup repository.
* **`:core:runtime`**: Foreground service management, AlarmManager exact wakeups, interactive notification renderer (`RemoteViews`), and alert audio/haptics.
* **`:core:testing`**: Test doubles, fake clocks, and in-memory test stores.

---

## Prerequisites & Installation

### Environment Requirements
- **JDK**: Java 17 or higher
- **Android Studio**: Ladybug (2024.2.1) or newer recommended
- **Android SDK**:
  - `compileSdk`: **37**
  - `minSdk`: **35**
  - `targetSdk`: **37**
- **Maestro CLI** (optional, for end-to-end testing):
  - macOS / Linux: `curl -fsSL "https://get.maestro.mobile.dev" | bash`
  - Windows: `powershell -c "irm https://get.maestro.mobile.dev | iex"`

### Building the Project
Clone the repository and build the debug APK:

```bash
# Clone the repository
git clone https://github.com/guillaumemilitello/notification_timer.git
cd notification_timer

# Build debug APK
./gradlew assembleDebug
```

---

## Running Tests

### Running Tests from Android Studio UI

You can run and debug tests directly within the Android Studio IDE without touching the command line:

1. **From the Code Editor (Gutter Icons)**:
   - Open any test file (e.g. [`TimerEngineTest.kt`](file:///D:/Code/notification_timer/core/domain/src/test/java/com/notification/timer/core/domain/engine/TimerEngineTest.kt) or [`BackupRepositoryTest.kt`](file:///D:/Code/notification_timer/core/data/src/test/java/com/notification/timer/core/data/repository/BackupRepositoryTest.kt)).
   - Click the green **Play icon (▶)** in the left gutter next to the class declaration (to run all tests in the file) or next to any individual `@Test` function.
   - Choose **Run '...'** or **Debug '...'**.

2. **From the Project Explorer**:
   - In the **Project** or **Android** tool window (left panel `Alt+1` / `Cmd+1`):
     - Switch view to **Project**.
     - Right-click any module's `src/test` folder (e.g. `core/domain/src/test` or `core/runtime/src/test`).
     - Select **Run 'Tests in 'test''** (or press `Ctrl+Shift+F10` / `Ctrl+Shift+R`).

3. **From the Gradle Tool Window**:
   - Open the **Gradle** side panel (on the right side of Android Studio).
   - Expand `notification_timer` -> `Tasks` -> `verification`.
   - Double-click **`test`** (runs all unit tests) or **`check`** (runs unit tests + lint + spotless + detekt).
   - Alternatively, expand any subproject (e.g. `:core:domain` -> `Tasks` -> `verification` -> `test`).

---

### Command Line

#### 1. Unit & Local Tests (JVM & Robolectric)
Run all unit tests across all subprojects:

```bash
# Run all unit tests
./gradlew test

# Run unit tests for a specific module
./gradlew :core:domain:test
./gradlew :core:runtime:testDebugUnitTest
./gradlew :core:data:testDebugUnitTest
```

### 2. Code Quality & Lint Gates
Execute the comprehensive verification gate (linting, Spotless formatting check, detekt static analysis, and unit tests):

```bash
# Complete quality check
./gradlew check

# Automatically fix code formatting issues
./gradlew spotlessApply
```

### 3. Maestro End-to-End Tests
[Maestro](https://maestro.mobile.dev/) is used for reliable UI integration and smoke flows.

Make sure an Android device or emulator (API 35+) is running and connected via ADB:

```bash
# Verify connected device
adb devices

# Install the debug build onto the device
./gradlew :app:installDebug

# Run a specific Maestro test flow
maestro test maestro/timer_basics.yaml

# Or run all flows in the maestro directory
maestro test maestro/
```

### 4. Running the Quality Gate Script
You can run the unified test and quality gate script directly from the repository root:

* **Windows PowerShell:**
  ```powershell
  .\scripts\gate.ps1
  ```
  *(Pass `-RunMaestro` to include end-to-end Maestro tests on a connected device: `.\scripts\gate.ps1 -RunMaestro`)*

* **macOS / Linux:**
  ```bash
  ./scripts/gate.sh
  ```
  *(Pass `--maestro` to include end-to-end Maestro tests: `./scripts/gate.sh --maestro`)*

---

## Contributing & Agent Guidelines
For development rules, invariants, and guidelines for AI agents, refer to [AGENTS.md](AGENTS.md) and the comprehensive architecture roadmap in [PLAN.md](PLAN.md).
