[CmdletBinding()]
param(
    [switch]$RunMaestro
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Running NotificationTimer 2.0 Quality Gate" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Spotless code formatting check
Write-Host "`n[1/4] Checking Spotless code formatting..." -ForegroundColor Yellow
.\gradlew.bat spotlessCheck

# 2. Detekt static analysis
Write-Host "`n[2/4] Running Detekt static analysis..." -ForegroundColor Yellow
.\gradlew.bat detekt

# 3. Unit and Robolectric Tests
Write-Host "`n[3/4] Running unit and Robolectric tests..." -ForegroundColor Yellow
.\gradlew.bat test

# 4. Lint and Full Gradle Check
Write-Host "`n[4/4] Running full check & lint..." -ForegroundColor Yellow
.\gradlew.bat check

Write-Host "`nAll Gradle checks and unit tests PASSED!" -ForegroundColor Green

# Optional: Maestro Integration Tests
if ($RunMaestro) {
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host " Running Maestro End-to-End Tests" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan

    $devices = adb devices
    $hasDevice = $false
    foreach ($line in $devices) {
        if ($line -match "\s+device$") {
            $hasDevice = $true
            break
        }
    }

    if (-not $hasDevice) {
        Write-Error "No ADB device/emulator found. Please connect an Android device or start an emulator."
        exit 1
    }

    Write-Host "Building and installing debug app..." -ForegroundColor Yellow
    .\gradlew.bat :app:installDebug

    Write-Host "Executing Maestro flows..." -ForegroundColor Yellow
    maestro test --exclude-tags wip maestro/

    Write-Host "`nMaestro E2E tests PASSED!" -ForegroundColor Green
}

Write-Host "`nQuality gate completed successfully!" -ForegroundColor Green
