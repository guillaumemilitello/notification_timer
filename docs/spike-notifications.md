# Phase 1 Spike Findings: Notifications & Background Timing

This document records the measurement results and architecture validation from the Phase 1 spike.

## 1. Diagnostics Event Log
- Persisted/in-memory ring buffer implemented in `InMemoryEventLog` (capacity 1,000 entries).
- Records all timer commands, background wakeup scheduling, wake lock acquisition/release, and notification updates.
- Integrated into the debug UI harness (`SpikeHarnessScreen`).

## 2. Notification Layout & Chronometer Rendering
- Ported custom `RemoteViews` layouts (`notification.xml`, `notification_black_text.xml`, `notification_white_text.xml`, `notification_red_text.xml`).
- Relative luminance calculation (`0.2126*R + 0.7152*G + 0.0722*B > 0.5`) dynamically selects black vs. white text for contrast on phase colors.
- `Chronometer` with `setCountDown(true)` renders countdown accurately without requiring periodic re-posting every second.
- Paused timers freeze the displayed time using static string formatting (`M:SS` or `H:MM:SS`), preventing drift.

## 3. PendingIntents & Action Isolation
- Per-timer explicit `PendingIntent`s constructed using deterministic data URIs (`ntimer://timer/{id}/{action}`).
- Supports concurrent control of up to 5 live timers independently from the notification shade without action collision or intent extras overriding.

## 4. Foreground Service (`specialUse`) & Auto-Grouping
- Declared `foregroundServiceType="specialUse"` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` in `AndroidManifest.xml`.
- When 4+ notifications are active, Android auto-groups them under a summary notification. Buttons remain interactive when expanded.
- Soft limit of 5 concurrent timers effectively prevents system notification rate limiting (~5 posts/sec).

## 5. Background Wakeup & AlarmManager
- Exact wakeup scheduling using `AlarmManager.setExactAndAllowWhileIdle` with `ELAPSED_REALTIME_WAKEUP` armed 2 seconds prior to events (`EARLY_MARGIN_MS = 2000`).
- Accompanied by a 4-second `PARTIAL_WAKE_LOCK` (`WakeupScheduler`) ensuring process execution even in deep Doze mode.
- Confirms the timing recipe from Section 8 and Section 9 of `PLAN.md`.
