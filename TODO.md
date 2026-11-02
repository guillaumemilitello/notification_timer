# TODO

NotificationTimer 2.0 backlog, roadmap, and completed milestones.

## Now (Active Backlog)
- [x] In a new timer entry island: remove the tab for "Manual" and "Preset", Default is manual and history will be accessible through an icon left to the close cross.
- [x] Remove the manual timer setting wheel and color from the timer name text box. Use default values, only changeable from the settings.
- [x] Redesign the timer tile from the main activity. I want a dedicated "part/object" (dont know the name) at the top with the name and set equivalent to the "= drag to reorder" when available.
- Long press will just change the available icons and items in this section of the tile (`=` only to indicate it can be dragged and dropped) + the setting wheel
- It should be at the top of the tile and it can stay white for now


- [x] When entering a new timer, I should have access to "more" settings with the wheel icon and access to the same settings I get after a set timer and a long-press. The settings should then be saved into the current draft timer until the user press "start".
- [x] After long press on a Timer, I don't want to see the Drag to Organize, redesign it (more like MD3), the `=` sign should be closed to the text and drag and drop should be easy. The settings wheel icon should also be well integrated.

### MainActivity & UI
- [x] Lock screen on feature. Use same icons as the 1.5 version.
      The icon should be in the main activity on the right of "NotificaitonTimer" header.
      Make a plan, include to run the gate, add unit and meastro tests, update the docs.
- [x] Preset should become history. User will not be asked to save a timer as 'preset' but the app will keep a list of historical entries.
      From the timer entry replace the preset icon by a 'history' icon and display a list of Most frequently used timers.
      For now, in the list, display the timer name, time, and sets numbers.
      Add the possibility to star some timers so they always appear at the top of the list. No drag and drop in this simple list.
      Find the best number of timers to keep and make it configurable. Fow now, use default value of 10.
      How is saved a timer "on the disk" should be scalable and "retrocompatible" as a timer will grow in features, just keep a list of IDs for example and the timers are in a small DB?
      Make a plan, include to run the gate, add unit and meastro tests, update the docs.
- [x] Add notification sound and vibration setting into the timer from the setting panel. The setting should be saved alongside the timer (history).
- [ ] Each timer can setup M steps per timer, with a name, color, and notification sound/vibration for the step
      We can configure them from the timer settings. Default values applies.
- [ ] Change countdown progression by clicking or dragging on progress bar.
- [ ] Circular dial picker as second input mode in numpad.

### Notifications
- [ ] Notification redesign: custom progress bar, refreshed vector icons, lock-screen layout polish.

## Next (Roadmap & Enhancements)
- [ ] Material 3 Expressive redesign (shared play button style when active, live animations).
- [ ] Focus mode (fullscreen immersive countdown view).
- [ ] Home screen widgets (Jetpack Glance) and Quick Settings tile.
- [ ] Alarm volume stream option (play alerts on alarm stream instead of notification stream).

## Completed

### Groundwork & Core Architecture
- [x] Multi-module Gradle build (`:app`, `:feature:*`, `:core:*`) with version catalog and convention plugins.
- [x] Tooling setup: Spotless (ktlint), Detekt, Android Lint, strict quality gate script (`gate.ps1` / `gate.sh`).
- [x] Resource porting: Recursive variable fonts, notification layouts, 18-color palette, English, French, and Spanish localization.
- [x] Documentation: Lean caveman `AGENT.md`, `CONTRIBUTING.md`, `docs/architecture.md`, `docs/testing.md`, `README.md` with Mermaid diagram.
- [x] Structured diagnostic logging across all modules with log levels, in-memory event buffer, and R8 rules stripping debug/info logs in release builds.

### Domain Core (`:core:domain`)
- [x] Pure Kotlin business models (`Timer`, `TimerConfig`, `HistoryEntry`, `Alert`, `UserSettings`, `TimerCommand`, `TimerEvent`).
- [x] Deterministic state machine (`Ready`, `Running`, `Paused`, `Completed`).
- [x] Event planner for alert scheduling, threshold calculations, and next-event computation.
- [x] `TimerEngine`: command queue, single-threaded processing, restore and reconcile on startup.
- [x] Invariant enforcement: timestamps only (`elapsedRealtime`), zero tick counters, state changes only in domain.

### Data & Persistence (`:core:data`)
- [x] DataStore persistence with JSON serialization (`library.json` for history/settings, `runtime.json` for active live timers).
- [x] Normalized schema v2 (`timersById` + `history` list) with deterministic fingerprint deduplication, smart eviction, and seamless backward-compatible v1 migration.
- [x] Configurable history limit (default 10, range 5..50, step 5) with starred timer pinning.
- [x] Storage Access Framework (SAF) JSON backup export and import repository with schema checks.

### Android Runtime (`:core:runtime`)
- [x] Background wakeup with `AlarmManager.setExactAndAllowWhileIdle` (2-second early margin) + `PARTIAL_WAKE_LOCK`.
- [x] `AlertPlayer`: audio ducking (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`), vibration patterns, logging to `InMemoryEventLog`.
- [x] `ForegroundService` (`specialUse` subtype) with sticky Running-only anchoring.
- [x] Per-timer `RemoteViews` notification renderer with countdown `Chronometer` (no per-second re-posting).
- [x] Dynamic luminance calculation (`0.2126*R + 0.7152*G + 0.0722*B > 0.5`) picking black vs. white text on phase colors.
- [x] Per-timer deterministic `PendingIntent`s (`ntimer://timer/{id}/{action}`) with `FLAG_IMMUTABLE`.

### Jetpack Compose UI (`:feature:*`, `:core:ui`)
- [x] Horizontal timer pager and live timer cards with phase color indicators.
- [x] Custom `TextAutoSize` modifier for auto-scaling timer digits without clipping.
- [x] Compose numpad with formatted display buffer and sets selector in `:core:ui`.
- [x] Automated timer history island with quick launch, star pinning, and frequency tracking.
- [x] Retired legacy `:feature:presets` module and cleanly consolidated UI into `:core:ui`.
- [x] Settings screen with history limit stepper, JSON backup import/export UI, and "How it works" guidance page.
- [x] Floating island dialogs (MD3 center dialogs) for Add Timer and Timer Settings.
- [x] Custom timer name entry when creating a new timer via Add Timer dialog.
- [x] Per-timer sets selector (from 0 (infinite) to N) in Timer Settings island.
- [x] Keep Screen On header toggle with 1.5 icons (`ic_screen` / `ic_screen_locked`), `LocalView.keepScreenOn` window flag, and `DisplayPreferences` persistence in `library.json`.

### Bug Fixes & UX Polish
- [x] Eliminated extra second at countdown end: ceiling rounding `(millis + 999) / 1000` for time display and alert thresholds.
- [x] Centered 3-slot Play/Pause control layout (`[Empty] [Play/Pause] [Next/Reset/Stop]`).
- [x] Timer termination moved exclusively to scoped floating red cross button.
- [x] New timers placed at top of list while preserving floating `+` button.
- [x] Scoped long-press options per timer (floating red cross, settings wheel, drag handle).
- [x] Drag-and-drop timer reordering with live animated item movements.
- [x] Prevented notifications jumping to silent section: channel `IMPORTANCE_DEFAULT` without sound, ongoing foreground service, `CATEGORY_STOPWATCH`.
- [x] Prevented `0:00` and `-00:01` overruns: state advances to next set in `Ready(isCompleted = true)` with full duration, color staying green (`#4CAF50`).
- [x] Strict per-timer notification isolation and N-timer scalability: removed 5-timer ceiling, persisted monotonic `TimerId`, per-timer `isNotificationVisible` lifecycle.
- [x] Prevented countdown number jump on Play: evaluated elapsed time dynamically during composition and decoupled running ticker from stale remember snapshot.

### Testing & Verification
- [x] JVM unit tests covering domain state machine, event planner, config fingerprinting, and `TimerEngine`.
- [x] Robolectric tests covering `BackupRepository`, `DataStoreStore`, `HistoryRepositoryImpl`, `DisplayPreferencesRepository`, `AlertPlayer`, `WakeupScheduler`, and `NotificationRenderer`.
- [x] Feature ViewModel unit tests with Turbine and coroutines test dispatcher (`TimersViewModelTest`).
- [x] Maestro end-to-end integration flows (`timer_basics.yaml`, `multi_timer.yaml`, `timer_history.yaml`, `keep_screen_on.yaml`, `notification_actions.yaml`, `settings_and_help.yaml` tagged `wip`).
- [x] Unified quality gate scripts (`scripts/gate.ps1`, `scripts/gate.sh`) enforcing `spotlessCheck`, `detekt`, `test`, `check`.
