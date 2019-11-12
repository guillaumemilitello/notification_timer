package com.notification.timer;

import androidx.annotation.NonNull;

import java.util.Locale;

class Preset {

    private final long timer;
    private final int sets;
    private String name;

    Preset() {
        this.timer = -1;
        this.sets = -1;
        this.name = "";
    }

    Preset(long timer, int sets, String name) {
        this.timer = timer;
        this.sets = sets;
        this.name = name;
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

    @NonNull
    public String toString() {
        return String.format(Locale.US, "(%s:%s:%s|%s|%s)", getTimerHoursString(), getTimerMinutesString(true), getTimerSecondsString(), getName(), getSetsString());
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
