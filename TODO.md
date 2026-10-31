# TODO

List of items, bugs, and enhancements noticed while testing NotificationTimer 2.0.

## Now
- [ ] Create a README.md file to explain how to use the project, install dependencies, etc...
- [ ] How to run unit and maestro tests
- [ ] List and extend all tests (unit and integration (maestro))
- [ ] Create a unit and integration tests "gate"

### MainActivity
- [ ] I want to change the progression of the timer by clicking on the progress bar.
- [ ] Settings for a timer implementation island

### Notification
- [ ] Better Design, progress bar and icons

### Long term
- [ ] Timer colors
- [ ] M Extra notifications or steps
- [ ] Focus mode

## Completed

- [x] Fixed extra second at the end: `00:00` is no longer displayed during countdown (uses ceiling rounding `(millis + 999) / 1000` for time formatting and alert threshold calculations).
- [x] The main button Play/Pause is always centered in a 3-slot layout (`[Empty] [Play/Pause] [Next/Reset/Stop]`, with timer termination moved exclusively to the long-press floating red cross).
- [x] Adding a timer places the new timer at the top of the list (while keeping the floating `+` button).
- [x] Long-press options scoped strictly to the triggered timer (floating red cross, settings wheel, and drag handle).
- [x] Drag-and-drop reordering with live item animations.
- [x] New Timer & Timer Settings as MD3 center floating islands.
- [x] Fixed notifications jumping to 'silent' section when paused or moving between sets (upgraded channel to `IMPORTANCE_DEFAULT` without sound, maintained ongoing foreground service while active, and set `CATEGORY_STOPWATCH`).
- [x] Eliminated `0:00` and negative `-00:01` values: upon timer completion, state immediately advances to the next set in `Ready(isCompleted = true)` with full duration, color staying green (`#4CAF50`), and `RemoteViews` chronometer countdown prevented from overrunning past zero.
- [x] Strict per-timer notification isolation & N-timer scalability: removed 5-timer ceiling, persisted monotonic `TimerId` counter, tracked explicit per-timer `isNotificationVisible` lifecycle (including notification close/swipe dismissal without deleting tiles), added per-timer `TimerNotificationSpec` diffing with sticky Running-only foreground service anchoring, and scaled periodic refresh intervals dynamically across N running timers.


## Later

- [ ] MD3. Play button should be shared when playing.
- [ ] Focus mode
