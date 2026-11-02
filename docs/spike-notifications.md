# Spike Findings: Notifications & Background Timing

Phase 1 validation measurements and results.

## Diagnostics Event Log
- `InMemoryEventLog` ring buffer (1,000 entries).
- Logs all timer commands, wakeups, wake locks, notification updates.

## Notification Layout & Chronometer
- Native Material 3 accent style: unified `notification.xml` with leading `viewColorAccent` bar.
- Theme-aware contrast resolver (`NotificationColorResolver`) ensuring WCAG contrast in Light and Dark modes.
- `Chronometer` with `setCountDown(true)` ticks in system process. Zero per-second re-posting.
- Paused timers freeze with static string (`M:SS` or `H:MM:SS`). No drift.

## PendingIntents & Action Isolation
- Per-timer explicit `PendingIntent` with URI `ntimer://timer/{id}/{action}`.
- Immutable (`FLAG_IMMUTABLE`). Zero action collisions between timers.

## Foreground Service & System Grouping
- `foregroundServiceType="specialUse"` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
- 4+ active timers auto-grouped by Android. Buttons remain clickable in expanded group.
- Notification rate limit avoided by per-timer diffing and selective posting.

## Background Wakeup & AlarmManager
- `AlarmManager.setExactAndAllowWhileIdle` on `ELAPSED_REALTIME_WAKEUP`.
- Armed 2 seconds before event (`EARLY_MARGIN_MS = 2000`).
- 4-second `PARTIAL_WAKE_LOCK` handles execution in deep Doze.
