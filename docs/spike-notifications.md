# Spike Findings: Notifications & Background Timing

Phase 1 validation measurements and results.

## Diagnostics Event Log
- `InMemoryEventLog` ring buffer (1,000 entries).
- Logs all timer commands, wakeups, wake locks, notification updates.
- Exposed in debug harness (`SpikeHarnessScreen`).

## Notification Layout & Chronometer
- Custom `RemoteViews`: `notification.xml`, `notification_black_text.xml`, `notification_white_text.xml`, `notification_red_text.xml`.
- Luminance formula `0.2126*R + 0.7152*G + 0.0722*B > 0.5` picks black vs. white text on phase color.
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
