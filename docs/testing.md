# Testing

Testing guide and rules for NotificationTimer 2.0.

## Test Layers
- **JVM Unit Tests (`:core:domain`)**:
  - Fast, pure Kotlin tests. Zero Android dependencies.
  - Tools: JUnit 4, `kotlinx-coroutines-test`, Turbine, Kotest property testing.
- **Robolectric & Integration Tests (`:core:runtime`, `:core:data`)**:
  - Emulate Android framework (AlarmManager, NotificationManager, DataStore, Audio/Vibration) on JVM.
- **ViewModel Unit Tests (`:feature:*`)**:
  - StateFlow emission verification using Turbine and test coroutine dispatchers.
- **Maestro E2E Tests (`maestro/`)**:
  - Real device or emulator UI integration flows (timer creation, notification shade actions, multi-timer concurrency).

## Running Tests

### Android Studio IDE
- **Gutter icon**: Click green Play button (▶) next to any test class or test function.
- **Project window**: Right-click test directory -> Run 'Tests in ...' (`Ctrl+Shift+F10`).
- **Gradle window**: Expand `Tasks` -> `verification` -> `test` or `check`.

### Command Line
```bash
# Run all unit tests
./gradlew test

# Run tests for specific module
./gradlew :core:domain:test
./gradlew :core:runtime:testDebugUnitTest
./gradlew :core:data:testDebugUnitTest
./gradlew :feature:timers:testDebugUnitTest

# Run code format check & detekt
./gradlew spotlessCheck
./gradlew detekt

# Auto-apply code formatting
./gradlew spotlessApply
```

### Maestro E2E
```bash
# Check device connected
adb devices

# Install app
./gradlew :app:installDebug

# Run single flow
maestro test maestro/timer_basics.yaml

# Run all active flows (excluding WIP)
maestro test --exclude-tags wip maestro/
```

## Quality Gate (Mandatory Before Commit)
Run the automated gate script before any commit:
- **Windows PowerShell**:
  ```powershell
  .\scripts\gate.ps1
  ```
- **Linux / macOS**:
  ```bash
  ./scripts/gate.sh
  ```
- Optional flag `-RunMaestro` / `--maestro`: runs Maestro E2E suite on connected device.

## Rules for Code Changes
- Any logic change must have new or updated unit tests.
- Any UI/notification shade change must have new or updated Maestro test.
- Incomplete flows must be marked `tags: [wip]`.
- All tests and gate checks must pass clean.
- Code coverage is deferred for now (will be enforced later).
