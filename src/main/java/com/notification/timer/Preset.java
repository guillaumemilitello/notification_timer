package com.notification.timer;

import androidx.annotation.NonNull;

import java.util.Locale;

class Preset {

    static final int DISPLAY_MODE_TIMER = 0;
    static final int DISPLAY_MODE_NAME = 1;

    private final long timer;
    private final int sets;
    private String name;
    private int displayMode;

    Preset() {
        this.timer = -1;
        this.sets = -1;
        this.name = "";
        this.displayMode = DISPLAY_MODE_TIMER;
    }

    Preset(long timer, int sets, String name, int displayMode) {
        this.timer = timer;
        this.sets = sets;
        this.name = name;
        this.displayMode = displayMode;
    }

    int getSets() {
        return sets;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    int getDisplayMode() {
        return this.displayMode;
    }

    void setDisplayMode(int displayMode) {
        this.displayMode = displayMode;
    }

    void switchDisplayMode() {
        if (this.displayMode == DISPLAY_MODE_TIMER) {
            this.displayMode = DISPLAY_MODE_NAME;
        } else {
            this.displayMode = DISPLAY_MODE_TIMER;
        }
    }

    boolean isInfinity() {
        return sets == Integer.MAX_VALUE;
    }

    String getSetsString() {
        return (sets == Integer.MAX_VALUE)? "" : String.format(Locale.US, "x%d", sets);
    }

    public long getTimer() {
        return timer;
    }

    String getTimerHoursString() {
        return String.format(Locale.US, "%d", timer / 3600);
    }

    String getTimerMinutesString(boolean zero) {
        String format = zero ? "%02d" : "%d";
        return String.format(Locale.US, format, timer % 3600 / 60);
    }

    String getTimerSecondsString() {
        return String.format(Locale.US, "%02d", timer % 60);
    }

    String getShortcutString() {
        String timerString;
        if (timer >= 3600) {
            timerString = String.format(Locale.US, "%s:%s:%s", getTimerHoursString(), getTimerMinutesString(true), getTimerSecondsString());
        } else {
            timerString = String.format(Locale.US, "%s:%s", getTimerMinutesString(false), getTimerSecondsString());
        }
        if (isInfinity()) {
            return String.format(Locale.US, "%s | %s", getName(), timerString);
        } else {
            return String.format(Locale.US, "%s | %s x%d", getName(), timerString, getSets());
        }
    }

    String getShortcutId() {
        return String.format(Locale.US, "%s_%d_%d", getName().substring(0, Math.min(getName().length(), 16)), getTimer(), getSets());
    }

    @NonNull
    public String toString() {
        return String.format(Locale.US, "[%s:%s:%s|%s|%s|%d]", getTimerHoursString(), getTimerMinutesString(true), getTimerSecondsString(), getName(), getSetsString(), getDisplayMode());
    }

    boolean isValid() {
        return this.timer > 0 && this.sets > 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object instanceof Preset) {
            Preset preset = (Preset)object;
            return this.timer == preset.timer && this.sets == preset.sets && this.name.equals(preset.name);
        } else {
            return false;
        }
    }
}
