#!/usr/bin/env bash
set -euo pipefail

RUN_MAESTRO=false

for arg in "$@"; do
  case $arg in
    --maestro|-m)
      RUN_MAESTRO=true
      shift
      ;;
  esac
done

echo "=========================================="
echo " Running NotificationTimer 2.0 Quality Gate"
echo "=========================================="

# 1. Spotless code formatting check
echo -e "\n[1/4] Checking Spotless code formatting..."
./gradlew spotlessCheck

# 2. Detekt static analysis
echo -e "\n[2/4] Running Detekt static analysis..."
./gradlew detekt

# 3. Unit and Robolectric Tests
echo -e "\n[3/4] Running unit and Robolectric tests..."
./gradlew test

# 4. Lint and Full Gradle Check
echo -e "\n[4/4] Running full check & lint..."
./gradlew check

echo -e "\nAll Gradle checks and unit tests PASSED!"

# Optional: Maestro Integration Tests
if [ "$RUN_MAESTRO" = true ]; then
  echo "=========================================="
  echo " Running Maestro End-to-End Tests"
  echo "=========================================="

  if ! adb devices | grep -q "device$"; then
    echo "❌ No ADB device/emulator found. Please connect an Android device or start an emulator."
    exit 1
  fi

  echo "Building and installing debug app..."
  ./gradlew :app:installDebug

  echo "Executing Maestro flows..."
  maestro test --exclude-tags wip maestro/

  echo -e "\n🎉 Maestro E2E tests PASSED!"
fi

echo -e "\n✨ Quality gate completed successfully! ✨"
